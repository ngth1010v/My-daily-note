package io.github.ngth1010v.mydailynote.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline + startup profile:
 * `gradlew :app:generateBaselineProfile` with an API 33+ emulator connected, English UI.
 * Covers cold start, adding actions by text, scrolling the list, search and every screen.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("Add action")), TIMEOUT)

        repeat(3) { i ->
            tap(By.desc("Add action"))
            device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)?.text = "drink water $i"
            tap(By.desc("Save"))
        }
        device.findObject(By.scrollable(true))?.let { list ->
            list.fling(androidx.test.uiautomator.Direction.UP)
            list.fling(androidx.test.uiautomator.Direction.DOWN)
        }
        tap(By.desc("Search"))
        device.pressBack()
        Thread.sleep(500)
        for (screen in listOf("Groups", "Analysis", "Settings", "Home")) tap(By.desc(screen))
    }
}

private fun MacrobenchmarkScope.tap(selector: BySelector) {
    // A node found mid-animation can be recycled before the click lands; look it up again.
    for (attempt in 1..3) {
        try {
            device.wait(Until.findObject(selector), TIMEOUT)?.click()
            break
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
    device.waitForIdle()
    Thread.sleep(600)
}

internal const val PACKAGE = "io.github.ngth1010v.mydailynote"
internal const val TIMEOUT = 5_000L
