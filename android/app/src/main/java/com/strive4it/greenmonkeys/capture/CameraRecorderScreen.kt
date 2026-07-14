package com.strive4it.greenmonkeys.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.strive4it.greenmonkeys.GreenMonkeysApp
import kotlinx.coroutines.delay

/** 120-second cap, same as iOS (brief §4). */
private const val MAX_SECONDS = 120

/**
 * Front-camera video recorder. One tap to stop; the stored file name is
 * returned via [onRecorded] — drunk users don't navigate (brief §4).
 */
@Composable
fun CameraRecorderScreen(
    onRecorded: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = (context.applicationContext as GreenMonkeysApp).videoStore

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCamera = grants[Manifest.permission.CAMERA] ?: hasCamera
        hasMic = grants[Manifest.permission.RECORD_AUDIO] ?: hasMic
    }

    LaunchedEffect(Unit) {
        if (!hasCamera || !hasMic) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCamera) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onCancel) {
                Text("Camera permission needed — tap to go back")
            }
        }
        return
    }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(MAX_SECONDS) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    // Camera preview
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                val previewView = PreviewView(viewContext)
                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.SD))
                        .build()
                    val capture = VideoCapture.withOutput(recorder)
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            capture,
                        )
                        videoCapture = capture
                    } catch (_: Exception) {
                        // Front camera unavailable (odd emulators) — try any camera.
                        try {
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                            )
                            videoCapture = capture
                        } catch (_: Exception) {
                            // No camera at all: the cancel button remains.
                        }
                    }
                }, ContextCompat.getMainExecutor(viewContext))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        TextButton(
            onClick = {
                recording?.stop()
                onCancel()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) { Text("Cancel", color = Color.White) }

        if (isRecording) {
            Text(
                "●  ${secondsLeft}s",
                color = Color.Red,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )
        }

        Button(
            onClick = {
                val capture = videoCapture ?: return@Button
                if (isRecording) {
                    recording?.stop()
                    return@Button
                }
                val temp = store.newTempFile()
                val options = FileOutputOptions.Builder(temp).build()
                var pending = capture.output.prepareRecording(context, options)
                if (hasMic) {
                    @Suppress("MissingPermission")
                    pending = pending.withAudioEnabled()
                }
                recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> isRecording = true
                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            recording = null
                            if (!event.hasError() && temp.exists()) {
                                onRecorded(store.store(temp))
                            } else {
                                temp.delete()
                            }
                        }
                        else -> {}
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(84.dp),
            shape = CircleShape,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else Color.White,
            ),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (isRecording) Color.White else Color.Red, CircleShape),
            )
        }
    }

    // 120 s cap: count down while recording, then stop.
    LaunchedEffect(isRecording) {
        if (isRecording) {
            secondsLeft = MAX_SECONDS
            while (secondsLeft > 0 && isRecording) {
                delay(1_000)
                secondsLeft -= 1
            }
            if (isRecording) recording?.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { recording?.stop() }
    }
}
