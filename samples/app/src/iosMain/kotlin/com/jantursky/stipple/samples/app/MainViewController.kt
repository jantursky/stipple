package com.jantursky.stipple.samples.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

/**
 * The view controller the iOS app shows. Launch arguments pick the first page and theme, as the Android app's
 * intent extras do: `-entry gauge` opens that page, `-dark NO` starts in the light theme. iOS puts `-key value`
 * launch arguments into the standard user defaults.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val defaults = NSUserDefaults.standardUserDefaults
    val entry = defaults.stringForKey("entry")
    val dark = defaults.objectForKey("dark")?.let { defaults.boolForKey("dark") }
    return ComposeUIViewController { GalleryApp(entry, dark) }
}
