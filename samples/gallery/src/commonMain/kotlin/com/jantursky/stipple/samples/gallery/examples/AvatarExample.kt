package com.jantursky.stipple.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherPattern
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.DitherAvatar
import com.jantursky.stipple.samples.gallery.LandscapePainter

// Without a photo the avatar is the initials on a pattern made from the name. With one, the photo is dithered.
@Composable
fun AvatarExample(
    theme: StippleTheme,
    pattern: DitherPattern,
    animated: Boolean,
) {
    val photo = remember { LandscapePainter() }
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        listOf("Ada Lovelace", "Grace Hopper", "Alan Turing").forEach { name ->
            DitherAvatar(name, size = 96.dp, theme = theme, pattern = pattern, animated = animated)
        }
        DitherAvatar("Sunset", size = 96.dp, painter = photo, theme = theme, pattern = pattern)
    }
}
