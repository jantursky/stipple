package com.jantursky.stipple.samples.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

/**
 * The view controller the iOS app shows. Launch arguments pick the first page and theme, as the Android app's
 * intent extras do: `-entry gauge` opens that page, `-dark NO` starts in the light theme,
 * `-renderer shader` makes the benchmark page draw with the shader renderer. iOS puts `-key value`
 * launch arguments into the standard user defaults.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val defaults = NSUserDefaults.standardUserDefaults
    val entry = defaults.stringForKey("entry")
    val dark = defaults.objectForKey("dark")?.let { defaults.boolForKey("dark") }
    val renderer = defaults.stringForKey("renderer")
    return ComposeUIViewController { GalleryApp(entry, dark, renderer) }
}
