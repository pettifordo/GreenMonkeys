package com.strive4it.greenmonkeys.capture

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.strive4it.greenmonkeys.GreenMonkeysApp

/** Plays a stored video. Platform VideoView — no media dependency needed for local files. */
@Composable
fun VideoPlayerScreen(fileName: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = (context.applicationContext as GreenMonkeysApp).videoStore
    val file = store.file(fileName)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        if (file.exists()) {
            AndroidView(
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        setMediaController(MediaController(viewContext).also { it.setAnchorView(this) })
                        setVideoURI(Uri.fromFile(file))
                        setOnPreparedListener { player -> player.isLooping = false; start() }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "That video is gone. The memory, of course, remains.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) { Text("Close", color = Color.White) }
    }
}
