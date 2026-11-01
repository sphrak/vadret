#!/usr/bin/env bash
set -euo pipefail

readonly SEMANTIC_VERSION_PATTERN='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)((alpha|beta|rc)[0-9]{2})?$'
readonly PRE_RELEASE_PATTERN='(alpha|beta|rc)[0-9]{2}$'
readonly ANDROID_BUILD_FILE="androidApp/build.gradle"
readonly IOS_CONFIG_FILE="iosApp/Configuration/Config.xcconfig"
readonly CHANGELOG_FILE="androidApp/src/main/res/raw/changelog.md"
readonly SCREENSHOT_DIRECTORY="assets/screenshots"
readonly MAIN_ACTIVITY="se.vadretapp.vadret/se.vadretapp.vadret.main.MainActivity"
readonly RADAR_IMAGE_URL_EXTRA="radar_image_url"
readonly DEVICE_UI_DUMP_FILE="/sdcard/vadret_release_ui.xml"
readonly WEBP_QUALITY=80
readonly WEBP_COMPRESSION_METHOD=6
readonly UI_WAIT_TIMEOUT_SECONDS=30
readonly SCREEN_SETTLE_SECONDS=3
readonly MAP_SETTLE_SECONDS=8
readonly DEMO_CLOCK="1200"
readonly DEMO_BATTERY_LEVEL=100
readonly DEMO_SIGNAL_LEVEL=4
readonly DEMO_MOBILE_DATA_TYPE="3g"
readonly WEATHER_TAB_TEXT="Weather"
readonly WARNINGS_TAB_TEXT="Warnings"
readonly RADAR_TAB_TEXT="Radar"
readonly SETTINGS_TAB_TEXT="Settings"
readonly THEME_ROW_TEXT="Select a theme"
readonly LIGHT_THEME_TEXT="Light"
readonly DARK_THEME_TEXT="Dark"
readonly LIGHT_SCREENSHOT_SUFFIX="light"
readonly DARK_SCREENSHOT_SUFFIX="dark"
readonly PRECIPITATION_LAYER_TEXT="Precipitation"
readonly WIND_LAYER_TEXT="Wind"
readonly COMING_DAYS_FIRST_ROW_TEXT="Today"
readonly RADAR_TIMELINE_END_TEXT="Now"

fail() {
    echo "release: $1" >&2
    exit 1
}

step() {
    echo "==> $1"
}

require_tools() {
    local tool
    for tool in "$@"; do
        if ! command -v "${tool}" > /dev/null; then
            fail "${tool} is not installed"
        fi
    done
}

is_pre_release() {
    [[ "$1" =~ ${PRE_RELEASE_PATTERN} ]]
}

require_semantic_version() {
    if [[ ! "$1" =~ ${SEMANTIC_VERSION_PATTERN} ]]; then
        fail "$1 is not a version like 3.2.0, 3.2.0alpha01, 3.2.0beta01 or 3.2.0rc01"
    fi
}

require_new_tag() {
    if git rev-parse --quiet --verify "refs/tags/$1" > /dev/null; then
        fail "tag $1 already exists"
    fi
}

require_changelog_entry() {
    if ! grep -qF "## [$1]" "${CHANGELOG_FILE}"; then
        fail "${CHANGELOG_FILE} has no \"## [$1]\" section"
    fi
}

has_changes_outside() {
    local excluded_pathspecs=()
    local path
    for path in "$@"; do
        excluded_pathspecs+=(":(exclude)${path}")
    done
    [[ -n "$(git status --porcelain --untracked-files=no -- "${excluded_pathspecs[@]}")" ]]
}

require_clean_tree() {
    if has_changes_outside "${CHANGELOG_FILE}"; then
        fail "commit or stash your changes first, only ${CHANGELOG_FILE} may be uncommitted. To prepare again, run: git restore --staged --worktree ${ANDROID_BUILD_FILE} ${IOS_CONFIG_FILE} ${SCREENSHOT_DIRECTORY}"
    fi
}

require_phone() {
    local connected_device_count
    connected_device_count=$(adb devices | grep -cE $'\tdevice$' || true)
    if [[ -z "${ANDROID_SERIAL:-}" && "${connected_device_count}" -ne 1 ]]; then
        fail "connect exactly one phone or set ANDROID_SERIAL"
    fi
    if [[ "$(adb get-state 2> /dev/null || true)" != "device" ]]; then
        fail "the phone ${ANDROID_SERIAL:-} is not connected"
    fi
    if [[ "$(adb shell dumpsys power)" != *"mWakefulness=Awake"* ]]; then
        fail "wake and unlock the phone"
    fi
}

dump_ui() {
    adb shell "rm -f ${DEVICE_UI_DUMP_FILE}; uiautomator dump ${DEVICE_UI_DUMP_FILE}" > /dev/null 2>&1 || true
    adb exec-out cat "${DEVICE_UI_DUMP_FILE}" 2> /dev/null || true
}

wait_for_text() {
    local text="$1"
    local deadline=$((SECONDS + UI_WAIT_TIMEOUT_SECONDS))
    until [[ "$(dump_ui)" == *"text=\"${text}\""* ]]; do
        if ((SECONDS >= deadline)); then
            fail "\"${text}\" never showed up on the phone"
        fi
    done
}

tap_text() {
    local text="$1"
    local node_pattern="text=\"${text}\"[^>]*bounds=\"\\[([0-9]+),([0-9]+)\\]\\[([0-9]+),([0-9]+)\\]\""
    wait_for_text "${text}"
    local user_interface
    user_interface=$(dump_ui)
    if [[ ! "${user_interface}" =~ ${node_pattern} ]]; then
        fail "\"${text}\" disappeared before it could be tapped"
    fi
    adb shell input tap \
        $(((BASH_REMATCH[1] + BASH_REMATCH[3]) / 2)) \
        $(((BASH_REMATCH[2] + BASH_REMATCH[4]) / 2))
}

demo() {
    local demo_command="$1"
    shift
    adb shell am broadcast -a com.android.systemui.demo -e command "${demo_command}" "$@" > /dev/null
}

enter_demo_mode() {
    adb shell settings put global sysui_demo_allowed 1
    demo enter
    demo clock -e hhmm "${DEMO_CLOCK}"
    demo battery -e level "${DEMO_BATTERY_LEVEL}" -e plugged false
    demo network -e wifi show -e level "${DEMO_SIGNAL_LEVEL}"
    demo network -e mobile show -e datatype "${DEMO_MOBILE_DATA_TYPE}" -e level "${DEMO_SIGNAL_LEVEL}"
    demo notifications -e visible false
    demo status -e volume hide -e bluetooth hide -e alarm hide -e location hide -e mute hide -e zen hide
}

restore_phone() {
    demo exit || true
    adb shell rm -f "${DEVICE_UI_DUMP_FILE}" || true
}

capture_screen() {
    local png_file
    png_file=$(mktemp)
    adb exec-out screencap -p > "${png_file}"
    cwebp -quiet -q "${WEBP_QUALITY}" -m "${WEBP_COMPRESSION_METHOD}" "${png_file}" -o "${SCREENSHOT_DIRECTORY}/$1.webp"
    rm "${png_file}"
}

current_theme_text() {
    tap_text "${SETTINGS_TAB_TEXT}"
    wait_for_text "${THEME_ROW_TEXT}"
    if [[ "$(dump_ui)" == *"text=\"${DARK_THEME_TEXT}\""* ]]; then
        echo "${DARK_THEME_TEXT}"
    else
        echo "${LIGHT_THEME_TEXT}"
    fi
}

other_theme_text() {
    if [[ "$1" == "${DARK_THEME_TEXT}" ]]; then
        echo "${LIGHT_THEME_TEXT}"
    else
        echo "${DARK_THEME_TEXT}"
    fi
}

screenshot_suffix() {
    if [[ "$1" == "${DARK_THEME_TEXT}" ]]; then
        echo "${DARK_SCREENSHOT_SUFFIX}"
    else
        echo "${LIGHT_SCREENSHOT_SUFFIX}"
    fi
}

select_theme() {
    local theme_text="$1"
    local deadline=$((SECONDS + UI_WAIT_TIMEOUT_SECONDS))
    tap_text "${SETTINGS_TAB_TEXT}"
    tap_text "${THEME_ROW_TEXT}"
    local user_interface
    user_interface=$(dump_ui)
    until [[ "${user_interface}" == *"text=\"${LIGHT_THEME_TEXT}\""* && "${user_interface}" == *"text=\"${DARK_THEME_TEXT}\""* ]]; do
        if ((SECONDS >= deadline)); then
            fail "the theme dialog never opened"
        fi
        user_interface=$(dump_ui)
    done
    tap_text "${theme_text}"
}

capture_screenshots_in_theme() {
    local theme_text="$1"
    local suffix
    suffix=$(screenshot_suffix "${theme_text}")
    step "Taking ${suffix} screenshots"
    select_theme "${theme_text}"
    tap_text "${WEATHER_TAB_TEXT}"
    wait_for_text "${COMING_DAYS_FIRST_ROW_TEXT}"
    sleep "${SCREEN_SETTLE_SECONDS}"
    capture_screen "weather-${suffix}"
    tap_text "${WARNINGS_TAB_TEXT}"
    sleep "${SCREEN_SETTLE_SECONDS}"
    capture_screen "warnings-${suffix}"
    tap_text "${RADAR_TAB_TEXT}"
    tap_text "${PRECIPITATION_LAYER_TEXT}"
    wait_for_text "${RADAR_TIMELINE_END_TEXT}"
    sleep "${MAP_SETTLE_SECONDS}"
    capture_screen "radar-precipitation-${suffix}"
    tap_text "${WIND_LAYER_TEXT}"
    sleep "${MAP_SETTLE_SECONDS}"
    capture_screen "radar-wind-${suffix}"
}

capture_screenshots() {
    step "Installing the local release build"
    ./gradlew --quiet :androidApp:installFdroidLocalRelease
    adb shell am start -S -W -n "${MAIN_ACTIVITY}" --es "${RADAR_IMAGE_URL_EXTRA}" "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/10/25/radar_2610251300.tif" > /dev/null
    trap restore_phone EXIT
    enter_demo_mode
    local original_theme_text
    original_theme_text=$(current_theme_text)
    capture_screenshots_in_theme "$(other_theme_text "${original_theme_text}")"
    capture_screenshots_in_theme "${original_theme_text}"
}

replace_in_file() {
    local file="$1"
    local expression="$2"
    local expected_text="$3"
    sed -i.bak -E "${expression}" "${file}"
    rm "${file}.bak"
    if ! grep -qF "${expected_text}" "${file}"; then
        fail "could not set ${expected_text} in ${file}"
    fi
}

set_version_names() {
    local version="$1"
    step "Setting version ${version}"
    replace_in_file "${ANDROID_BUILD_FILE}" "s/versionName = \"[^\"]*\"/versionName = \"${version}\"/" "versionName = \"${version}\""
    replace_in_file "${IOS_CONFIG_FILE}" "s/^MARKETING_VERSION=.*$/MARKETING_VERSION=${version%%[!0-9.]*}/" "MARKETING_VERSION=${version%%[!0-9.]*}"
}

increment_version_codes() {
    local version_code
    version_code=$(sed -nE 's/^def appVersionCode = ([0-9]+)$/\1/p' "${ANDROID_BUILD_FILE}")
    if [[ -z "${version_code}" ]]; then
        fail "${ANDROID_BUILD_FILE} has no appVersionCode"
    fi
    local next_version_code=$((version_code + 1))
    step "Setting version code ${next_version_code}"
    replace_in_file "${ANDROID_BUILD_FILE}" "s/^def appVersionCode = [0-9]+$/def appVersionCode = ${next_version_code}/" "def appVersionCode = ${next_version_code}"
    replace_in_file "${IOS_CONFIG_FILE}" "s/^CURRENT_PROJECT_VERSION=.*$/CURRENT_PROJECT_VERSION=${next_version_code}/" "CURRENT_PROJECT_VERSION=${next_version_code}"
}

stage_release_files() {
    git add -- "${ANDROID_BUILD_FILE}" "${IOS_CONFIG_FILE}" "${CHANGELOG_FILE}" "${SCREENSHOT_DIRECTORY}"
}

print_commit_and_tag_command() {
    local version="$1"
    step "Prepared ${version}. Look over git diff --cached, then commit and tag with:"
    echo "git commit -m \"release/${version}\" && git tag ${version} -m \"[changelog](https://github.com/vadret/android/blob/${version}/androidApp/src/main/res/raw/changelog.md)\""
}

main() {
    if [[ $# -ne 1 ]]; then
        fail "usage: ./release.sh <version>, for example ./release.sh 3.2.0"
    fi
    local version="$1"
    cd "$(dirname "${BASH_SOURCE[0]}")"
    require_tools git
    require_semantic_version "${version}"
    require_new_tag "${version}"
    require_clean_tree
    if is_pre_release "${version}"; then
        set_version_names "${version}"
    else
        require_tools adb cwebp
        require_changelog_entry "${version}"
        require_phone
        capture_screenshots
        set_version_names "${version}"
        increment_version_codes
    fi
    stage_release_files
    print_commit_and_tag_command "${version}"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
