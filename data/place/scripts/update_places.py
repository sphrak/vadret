import hashlib
import io
import math
import sys
import urllib.request
import zipfile
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[3]
TARGET = REPOSITORY / "core" / "di" / "src" / "commonMain" / "composeResources" / "files" / "places.tsv"
COUNTRY = "SE"
POPULATED_PLACE = "P"
MUNICIPALITY_DIVISION = "ADM2"
# GeoNames names the biggest towns in English, as Gothenburg, so a county seat or the capital uses its Swedish name.
SEAT_CODES = {"PPLA", "PPLC"}
SWEDISH = "sv"
YES = "1"
# Abandoned, historical and destroyed places are not where anyone wants the weather.
GONE_PLACE_CODES = {"PPLQ", "PPLH", "PPLW", "PPLCH"}
MUNICIPALITY_SUFFIXES = (" Kommun", " kommun", " Stad", " stad")
GENITIVE_ENDING = "s"
# GeoNames only has these municipalities in a form that no rule turns into the name people use.
MUNICIPALITY_NAMES = {
    "Falu": "Falun",
    "Härjedalens": "Härjedalen",
    "Malung-Sälens": "Malung-Sälen",
}
VERSION_LENGTH = 16
COORDINATE_DECIMALS = 5
LATITUDE_TO_KILOMETRES = 111.0

COLUMN_GEONAME_ID = 0
COLUMN_NAME = 1
COLUMN_ALTERNATE_NAMES = 3
COLUMN_LATITUDE = 4
COLUMN_LONGITUDE = 5
COLUMN_FEATURE_CLASS = 6
COLUMN_FEATURE_CODE = 7
COLUMN_ADMIN1 = 10
COLUMN_ADMIN2 = 11
COLUMN_POPULATION = 14

ALTERNATE_COLUMN_GEONAME_ID = 1
ALTERNATE_COLUMN_LANGUAGE = 2
ALTERNATE_COLUMN_NAME = 3
ALTERNATE_COLUMN_PREFERRED = 4
ALTERNATE_COLUMN_COLLOQUIAL = 6
ALTERNATE_COLUMN_HISTORIC = 7


def download(url: str) -> bytes:
    with urllib.request.urlopen(url) as response:
        return response.read()


def read_rows(text: str) -> list[list[str]]:
    return [line.split("\t") for line in text.splitlines() if line and not line.startswith("#")]


def read_zipped_rows(url: str) -> list[list[str]]:
    with zipfile.ZipFile(io.BytesIO(download(url=url))) as zip_file:
        return read_rows(zip_file.read(f"{COUNTRY}.txt").decode("utf-8"))


def read_geonames() -> tuple[list[list[str]], list[list[str]], dict[str, str], dict[str, str]]:
    places = read_zipped_rows(url="https://download.geonames.org/export/dump/SE.zip")
    alternate_names = read_zipped_rows(url="https://download.geonames.org/export/dump/alternatenames/SE.zip")
    counties = {
        row[0].removeprefix(f"{COUNTRY}."): row[1]
        for row in read_rows(download(url="https://download.geonames.org/export/dump/admin1CodesASCII.txt").decode("utf-8"))
        if row[0].startswith(f"{COUNTRY}.")
    }
    municipalities = {
        row[0].removeprefix(f"{COUNTRY}."): row[1]
        for row in read_rows(download(url="https://download.geonames.org/export/dump/admin2Codes.txt").decode("utf-8"))
        if row[0].startswith(f"{COUNTRY}.")
    }
    return places, alternate_names, counties, municipalities


def swedish_names(
    places: list[list[str]],
    alternate_names: list[list[str]]
) -> dict[str, str]:
    seats = {row[COLUMN_GEONAME_ID] for row in places if row[COLUMN_FEATURE_CODE] in SEAT_CODES}
    names = {}
    for row in alternate_names:
        is_current = row[ALTERNATE_COLUMN_COLLOQUIAL] != YES and row[ALTERNATE_COLUMN_HISTORIC] != YES
        geoname_id = row[ALTERNATE_COLUMN_GEONAME_ID]
        is_shown = row[ALTERNATE_COLUMN_PREFERRED] == YES or (geoname_id in seats and geoname_id not in names)
        if row[ALTERNATE_COLUMN_LANGUAGE] == SWEDISH and is_current and is_shown:
            names[geoname_id] = row[ALTERNATE_COLUMN_NAME]
    return names


def municipality_key(row: list[str]) -> str:
    return f"{row[COLUMN_ADMIN1]}.{row[COLUMN_ADMIN2]}"


# GeoNames names a municipality as in "Bodens Kommun", so the genitive s goes only when the name without it is also
# a name of that municipality, which keeps "Tranås" and "Kramfors" whole.
def municipality_names(places: list[list[str]], municipalities: dict[str, str]) -> dict[str, str]:
    known_names: dict[str, set[str]] = {}
    for row in places:
        names = known_names.setdefault(municipality_key(row), set())
        if row[COLUMN_FEATURE_CODE] == MUNICIPALITY_DIVISION:
            names.update(row[COLUMN_ALTERNATE_NAMES].split(","))
        if row[COLUMN_FEATURE_CLASS] == POPULATED_PLACE:
            names.add(row[COLUMN_NAME])
    shown_names = {}
    for key, name in municipalities.items():
        base = name
        for suffix in MUNICIPALITY_SUFFIXES:
            base = base.removesuffix(suffix)
        without_genitive = base.removesuffix(GENITIVE_ENDING)
        if base.endswith(GENITIVE_ENDING) and without_genitive in known_names.get(key, set()):
            base = without_genitive
        shown_names[key] = MUNICIPALITY_NAMES.get(base, base)
    return shown_names


def distance(first: list[str], second: list[str]) -> float:
    latitude = math.radians(float(first[COLUMN_LATITUDE]))
    north = float(first[COLUMN_LATITUDE]) - float(second[COLUMN_LATITUDE])
    east = (float(first[COLUMN_LONGITUDE]) - float(second[COLUMN_LONGITUDE])) * math.cos(latitude)
    return math.hypot(north, east) * LATITUDE_TO_KILOMETRES


# A few small places lack a municipality, so they take the one of the nearest place in the same county.
def with_municipalities(places: list[list[str]]) -> list[list[str]]:
    placed_by_county: dict[str, list[list[str]]] = {}
    for row in places:
        if row[COLUMN_ADMIN2]:
            placed_by_county.setdefault(row[COLUMN_ADMIN1], []).append(row)
    completed = []
    for row in places:
        if row[COLUMN_ADMIN2]:
            completed.append(row)
        else:
            nearest = min(placed_by_county[row[COLUMN_ADMIN1]], key=lambda other: distance(first=row, second=other))
            completed.append(row[:COLUMN_ADMIN2] + [nearest[COLUMN_ADMIN2]] + row[COLUMN_ADMIN2 + 1:])
    return completed


def population(row: list[str]) -> int:
    return int(row[COLUMN_POPULATION] or "0")


def place_lines(
    places: list[list[str]],
    names: dict[str, str],
    counties: dict[str, str],
    municipalities: dict[str, str]
) -> list[str]:
    populated = [
        row[:COLUMN_NAME] + [names.get(row[COLUMN_GEONAME_ID], row[COLUMN_NAME])] + row[COLUMN_NAME + 1:]
        for row in places
        if row[COLUMN_FEATURE_CLASS] == POPULATED_PLACE
        and row[COLUMN_FEATURE_CODE] not in GONE_PLACE_CODES
        and row[COLUMN_ADMIN1] in counties
    ]
    shown = {}
    # Places of one name in one municipality look the same in a search, so only the most populated is kept.
    for row in sorted(with_municipalities(places=populated), key=lambda row: (-population(row), int(row[COLUMN_GEONAME_ID]))):
        municipality = municipalities[municipality_key(row)]
        shown.setdefault((row[COLUMN_NAME], municipality), (row, municipality))
    lines = []
    for (name, municipality), (row, _) in sorted(shown.items(), key=lambda item: (item[0][0], item[0][1])):
        lines.append(
            "\t".join(
                [
                    name,
                    municipality,
                    counties[row[COLUMN_ADMIN1]],
                    f"{float(row[COLUMN_LATITUDE]):.{COORDINATE_DECIMALS}f}",
                    f"{float(row[COLUMN_LONGITUDE]):.{COORDINATE_DECIMALS}f}",
                    str(population(row)),
                ]
            )
        )
    return lines


def main() -> None:
    places, alternate_names, counties, municipalities = read_geonames()
    lines = place_lines(
        places=places,
        names=swedish_names(places=places, alternate_names=alternate_names),
        counties=counties,
        municipalities=municipality_names(places=places, municipalities=municipalities),
    )
    body = "\n".join(lines) + "\n"
    version = hashlib.sha256(body.encode("utf-8")).hexdigest()[:VERSION_LENGTH]
    TARGET.write_text(f"{version}\n{body}", encoding="utf-8")
    print(f"Wrote {len(lines)} places to {TARGET.relative_to(REPOSITORY)}", file=sys.stderr)


if __name__ == "__main__":
    main()
