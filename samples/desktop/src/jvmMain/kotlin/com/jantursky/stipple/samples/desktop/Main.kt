package com.jantursky.stipple.samples.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jantursky.stipple.samples.gallery.GalleryApp

fun main(args: Array<String>) =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Stipple gallery",
            state = rememberWindowState(size = DpSize(1280.dp, 840.dp)),
        ) {
            GalleryApp(initialPage = args.firstOrNull())
        }
    }
