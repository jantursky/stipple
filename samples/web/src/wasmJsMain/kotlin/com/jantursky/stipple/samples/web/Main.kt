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

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        GalleryApp(
            initialPage = pageFromHash(),
            onPageChange = { id ->
                // Replace instead of push, so the back button leaves the gallery rather than stepping through pages.
                if (window.location.hash.removePrefix("#") != id) window.history.replaceState(null, "", "#$id")
            },
        )
    }
}
