package com.jantursky.stipple.samples.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jantursky.stipple.samples.gallery.GalleryApp
import kotlinx.browser.document
import kotlinx.browser.window

private fun pageFromHash(): String? =
    window.location.hash
        .removePrefix("#")
        .ifEmpty { null }

/** A query flag such as `?embed=1`: [default] when absent, otherwise true unless it is "0". */
private fun flag(
    name: String,
    default: Boolean = false,
): Boolean {
    val value = Regex("[?&]$name=([^&]*)").find(window.location.search)?.groupValues?.get(1)
    return if (value == null) default else value != "0"
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        GalleryApp(
            initialPage = pageFromHash(),
            initialAnimated = flag("animated", default = true),
            embed = flag("embed"),
            onPageChange = { id ->
                // Replace instead of push, so the back button leaves the gallery rather than stepping through pages.
                if (window.location.hash.removePrefix("#") != id) window.history.replaceState(null, "", "#$id")
            },
        )
    }
}
