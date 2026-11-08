package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.Language
import se.vadretapp.vadret.setting.model.SystemSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ShownLanguageTaskTest {

    @Test
    fun aChosenLanguageIsShownWhateverThePhoneSpeaks() =
        runTest {
            val shown: List<Either<Failure, Language>> = AppLanguage.entries
                .filter { appLanguage: AppLanguage -> appLanguage != AppLanguage.SYSTEM }
                .map { appLanguage: AppLanguage ->
                    DefaultGetShownLanguageTask(
                        settingRepository = FakeSettingRepository(
                            language = appLanguage.asRight(),
                            systemSettings = phone(languageTag = "fi-FI")
                        )
                    )()
                }

            assertEquals(
                expected = Language.entries.map { language: Language -> language.asRight() },
                actual = shown
            )
        }

    // The app's texts are kept for these languages in these regions only, so any other region reads English.
    @Test
    fun followingThePhoneShowsItsLanguageOnlyWhenTheAppHasItForThatRegion() =
        runTest {
            val shownByPhone: Map<String, Either<Failure, Language>> = listOf(
                "sv-SE",
                "sv_SE",
                "sv-SE-u-mu-celsius",
                "da-DK",
                "de-DE",
                "fi-FI",
                "fr-FR",
                "no-NO",
                "pl-PL",
                "en-US",
                "de-AT",
                "sv-FI",
                "nb-NO",
                "es-ES"
            ).associateWith { languageTag: String ->
                DefaultGetShownLanguageTask(settingRepository = FakeSettingRepository(systemSettings = phone(languageTag = languageTag)))()
            }

            assertEquals(
                expected = mapOf(
                    "sv-SE" to Language.SWEDISH.asRight(),
                    "sv_SE" to Language.SWEDISH.asRight(),
                    "sv-SE-u-mu-celsius" to Language.SWEDISH.asRight(),
                    "da-DK" to Language.DANISH.asRight(),
                    "de-DE" to Language.GERMAN.asRight(),
                    "fi-FI" to Language.FINNISH.asRight(),
                    "fr-FR" to Language.FRENCH.asRight(),
                    "no-NO" to Language.NORWEGIAN.asRight(),
                    "pl-PL" to Language.POLISH.asRight(),
                    "en-US" to Language.ENGLISH.asRight(),
                    "de-AT" to Language.ENGLISH.asRight(),
                    "sv-FI" to Language.ENGLISH.asRight(),
                    "nb-NO" to Language.ENGLISH.asRight(),
                    "es-ES" to Language.ENGLISH.asRight()
                ),
                actual = shownByPhone
            )
        }

    @Test
    fun aChoiceOrAPhoneThatCannotBeReadIsAFailure() =
        runTest {
            val failures: List<Pair<Either<Failure, Language>, Either<Failure, Language>>> = listOf(
                FakeSettingRepository(language = STORAGE_FAILED.asLeft()),
                FakeSettingRepository(systemSettings = STORAGE_FAILED.asLeft())
            ).map { settingRepository: FakeSettingRepository ->
                DefaultGetShownLanguageTask(settingRepository = settingRepository)() to DefaultObserveShownLanguageTask(settingRepository = settingRepository)().first()
            }

            assertEquals(
                expected = List(size = 2) { STORAGE_FAILED.asLeft() to STORAGE_FAILED.asLeft() },
                actual = failures
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aLanguageSwitchedOnThePhoneIsFollowedOnceAndAChangeOfClockAloneIsNot() =
        runTest {
            val settingRepository = FakeSettingRepository()
            val followed = mutableListOf<Either<Failure, Language>>()
            val job = launch { DefaultObserveShownLanguageTask(settingRepository = settingRepository)().toList(destination = followed) }
            advanceUntilIdle()

            settingRepository.systemSettings.value = SystemSettings(isTwentyFourHour = false, languageTag = "sv-SE").asRight()
            advanceUntilIdle()
            settingRepository.systemSettings.value = phone(languageTag = "pl-PL")
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(Language.SWEDISH.asRight(), Language.POLISH.asRight()),
                actual = followed.toList()
            )
        }
}

private fun phone(languageTag: String): Either<Failure, SystemSettings> =
    SystemSettings(
        isTwentyFourHour = true,
        languageTag = languageTag
    ).asRight()

private val STORAGE_FAILED = Failure(errorCode = ErrorCode.STORAGE_FAILED)