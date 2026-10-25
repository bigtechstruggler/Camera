package app.grapheneos.camera

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.grapheneos.camera.ui.FoldTabMode
import app.grapheneos.camera.ui.activities.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real UI regression paths. Fold-specific cases run on a device which exposes
 * the WindowArea modes; other devices skip rather than claim hardware coverage.
 */
@RunWith(AndroidJUnit4::class)
class FoldTabInteractionRegressionTest {
    @get:Rule
    val cameraPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val screenAwake = ScreenAwakeRule()

    @Test
    fun keyboardOrTalkBackClick_onStandardTabActuallyChangesCameraMode() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitModeTabs(scenario)
            scenario.onActivity { activity ->
                val tab = activity.tabLayout.getTabForMode(CameraMode.VIDEO)!!
                tab.view.performClick()
                activity.tabLayout.settleNow()
                assertEquals(CameraMode.VIDEO, activity.camConfig.currentMode)
            }
        }
    }

    @Test
    fun dualToRearToCamera_cancelsDelayedRearHandoff() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitModeTabs(scenario)
            scenario.onActivity { activity ->
                assumeTrue("Dual Screen and Rear Selfie require a foldable",
                    activity.availableFoldTabModes().containsAll(
                        listOf(FoldTabMode.DUAL, FoldTabMode.REAR)))
                activity.finalizeMode(activity.tabLayout.getTabForFoldMode(FoldTabMode.DUAL))
                activity.tabLayout.settleNow()
                activity.finalizeMode(activity.tabLayout.getTabForFoldMode(FoldTabMode.REAR))
                activity.tabLayout.settleNow()
                activity.finalizeMode(activity.tabLayout.getTabForMode(CameraMode.CAMERA))
                activity.tabLayout.settleNow()
            }
            // Outlast the 2-second deferred handoff, including a late WindowArea callback.
            Thread.sleep(2800)
            scenario.onActivity { activity ->
                assertEquals(CameraMode.CAMERA, activity.camConfig.currentMode)
                assertEquals(null, activity.foldSelectedMode)
                assertEquals(CameraMode.CAMERA, activity.tabLayout.selectedTab?.tag)
            }
        }
    }

    @Test
    fun pendingDualThenVideo_doesNotRestoreFoldTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitModeTabs(scenario)
            scenario.onActivity { activity ->
                assumeTrue("Dual Screen is not available on this device",
                    activity.availableFoldTabModes().contains(FoldTabMode.DUAL))
                activity.finalizeMode(activity.tabLayout.getTabForFoldMode(FoldTabMode.DUAL))
                activity.tabLayout.settleNow()
                activity.finalizeMode(activity.tabLayout.getTabForMode(CameraMode.VIDEO))
                activity.tabLayout.settleNow()
            }
            Thread.sleep(1600)
            scenario.onActivity { activity ->
                assertEquals(CameraMode.VIDEO, activity.camConfig.currentMode)
                assertEquals(null, activity.foldSelectedMode)
                assertEquals(CameraMode.VIDEO, activity.tabLayout.selectedTab?.tag)
            }
        }
    }
}
