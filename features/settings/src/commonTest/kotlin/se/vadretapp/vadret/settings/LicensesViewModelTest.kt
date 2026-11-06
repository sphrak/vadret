package se.vadretapp.vadret.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.model.LibraryGroup
import se.vadretapp.vadret.settings.usecase.DefaultGetLibrariesTask
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val ktor: Library = library(author = "JetBrains", title = "Ktor")
    private val coil: Library = library(author = "Coil Contributors", title = "Coil")
    private val kotlinxDatetime: Library = library(author = "JetBrains", title = "kotlinx-datetime")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): LicensesViewModel {
        val licensesViewModel = LicensesViewModel(
            getLibrariesTask = DefaultGetLibrariesTask(libraryRepository = FakeLibraryRepository(libraries = listOf(ktor, coil, kotlinxDatetime))),
            coroutineDispatcher = testDispatcher
        )
        licensesViewModel.onEvent(event = LicensesEvent.OnStart)
        return licensesViewModel
    }

    @Test
    fun `the libraries are grouped by author`() =
        testScope.runTest {
            val licensesViewModel: LicensesViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    "JetBrains" to listOf(ktor, kotlinxDatetime),
                    "Coil Contributors" to listOf(coil)
                ),
                actual = licensesViewModel.state.value.libraryGroups.map { libraryGroup: LibraryGroup -> libraryGroup.author to libraryGroup.libraries }
            )
        }

    @Test
    fun `a clicked library is shown until it is dismissed`() =
        testScope.runTest {
            val licensesViewModel: LicensesViewModel = viewModel()
            advanceUntilIdle()

            licensesViewModel.onEvent(event = LicensesEvent.OnLibraryClicked(library = coil))
            val shownLibrary: Library? = licensesViewModel.state.value.selectedLibrary
            licensesViewModel.onEvent(event = LicensesEvent.OnLibraryDismissed)

            assertEquals(
                expected = coil to null,
                actual = shownLibrary to licensesViewModel.state.value.selectedLibrary
            )
        }
}