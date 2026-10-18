package com.jrussell.fitness.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.backup.BackupFailure
import com.jrussell.fitness.core.domain.backup.DocumentRef
import com.jrussell.fitness.core.domain.backup.RestoreKind
import com.jrussell.fitness.core.domain.backup.RestorePreview
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BackupSettingsSemanticsTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 10, 18)
    private val off: BackupUiState = BackupUiState(
        folderName = null,
        lastBackup = null,
        lastFailure = null,
        today = today,
        exportDate = "2026-10-18",
        busy = false,
        message = null,
        pendingRestore = null,
    )

    @Test
    fun backupSection_hasHeading_andButtons() {
        show(off)

        rule.onNodeWithText("Backup").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Choose folder").assertIsEnabled()
        // Nothing to back up into until a folder is chosen.
        rule.onNodeWithText("Back up now").assertIsNotEnabled()
        rule.onNodeWithText("Export data").assertIsEnabled()
        rule.onNodeWithText("Export database copy").assertIsEnabled()
        rule.onNodeWithText("Restore from a file…").assertIsEnabled()
    }

    @Test
    fun statusOff_readsNotSetUp() {
        show(off)

        rule.onNodeWithText("Not set up. Choose a folder, such as one in Google Drive.").assertIsDisplayed()
    }

    @Test
    fun statusOn_readsFolderAndShortDate() {
        show(off.copy(folderName = "GetFit backups", lastBackup = LocalDate(2026, 10, 17)))

        rule.onNodeWithText("To GetFit backups · last backup Oct 17").assertIsDisplayed()
        rule.onNodeWithText("Change folder").assertIsEnabled()
        rule.onNodeWithText("Back up now").assertIsEnabled()
    }

    @Test
    fun statusFolderUnavailable_asksToChooseAgain() {
        show(off.copy(folderName = "GetFit backups", lastFailure = BackupFailure.FOLDER_UNAVAILABLE))

        rule.onNodeWithText("Can't reach the backup folder. Choose it again.").assertIsDisplayed()
    }

    @Test
    fun message_isPoliteLiveRegion() {
        show(off.copy(message = BackupMessage.Exported(42)))

        rule.onNodeWithText("Exported 42 entries")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun replaceDialog_readsWarningAndRestartButton() {
        val preview: RestorePreview = RestorePreview(RestoreKind.REPLACE, 12, null)
        show(
            off.copy(pendingRestore = PendingRestoreUi(DocumentRef("content://doc"), preview, LocalDate(2026, 10, 10))),
        )

        rule.onNodeWithText("Replace all data?").assertIsDisplayed()
        rule.onNodeWithText(
            "Everything on this phone is replaced with this backup (12 entries, last changed Oct 10). " +
                "GetFit will restart.",
        ).assertIsDisplayed()
        rule.onNodeWithText("Replace and restart").assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    @Config(fontScale = 2f)
    fun atFontScale2_buttonsWrapNotClip() {
        show(off.copy(folderName = "GetFit backups"))

        listOf("Change folder", "Back up now", "Export data", "Export database copy", "Restore from a file…")
            .forEach { label: String -> rule.onNodeWithText(label).performScrollTo().assertIsDisplayed() }
    }

    private fun show(state: BackupUiState) {
        rule.setContent {
            GetFitTheme {
                // Inside a scrolling column, as on the Settings screen.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    BackupSection(state = state, actions = BackupSectionActions.None)
                }
            }
        }
    }
}
