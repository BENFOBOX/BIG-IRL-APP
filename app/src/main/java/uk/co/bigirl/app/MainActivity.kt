package uk.co.bigirl.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BigIrlApp()
        }
    }
}

@Composable
private fun BigIrlApp() {
    val context = LocalContext.current

    var status by remember {
        mutableStateOf("Ready")
    }

    var endpoint by remember {
        mutableStateOf("")
    }

    var permissionGranted by remember {
        mutableStateOf(
            hasCameraAndMicrophonePermissions(context),
        )
    }

    var cameraRequested by remember {
        mutableStateOf(false)
    }

    var cameraStarted by remember {
        mutableStateOf(false)
    }

    var surfaceView by remember {
        mutableStateOf<SurfaceView?>(null)
    }

    var networks by remember {
        mutableStateOf(
            readNetworkSummary(context),
        )
    }

    val engine = remember {
        StreamEngine(context) {
            status = it
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            permissionGranted =
                hasCameraAndMicrophonePermissions(context)

            status = if (permissionGranted) {
                "Permissions granted. Press Start camera preview."
            } else {
                "Camera and microphone permissions are required"
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            engine.release()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            networks = readNetworkSummary(context)
            delay(2_000)
        }
    }

    LaunchedEffect(
        cameraRequested,
        surfaceView,
        permissionGranted,
    ) {
        val preview = surfaceView

        if (
            cameraRequested &&
            preview != null &&
            permissionGranted &&
            !cameraStarted
        ) {
            status = "Starting camera"

            cameraStarted = engine.startPreview(
                surfaceView = preview,
                profile = StreamProfile(),
            )
        }
    }

    val colours = darkColorScheme(
        primary = Color(0xFF51E1A6),
        onPrimary = Color(0xFF001F16),
        background = Color(0xFF0B1220),
        onBackground = Color.White,
        surface = Color(0xFF121C2E),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1B2940),
        onSurfaceVariant = Color(0xFFD7E2F4),
    )

    MaterialTheme(
        colorScheme = colours,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "BIG IRL APP",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )

                Text(
                    text =
                        "Original alpha • direct RTMP/SRT contribution",
                    color = Color(0xFFD7E2F4),
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF121C2E),
                        contentColor = Color.White,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                    ) {
                        NetworkPill(
                            name = "Wi-Fi",
                            active = networks.wifi,
                        )

                        NetworkPill(
                            name = "Cellular",
                            active = networks.cellular,
                        )
                    }

                    Text(
                        text = status,
                        color = Color.White,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = 12.dp,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .onSizeChanged { size ->
                            engine.updatePreviewSize(
                                width = size.width,
                                height = size.height,
                            )
                        }
                        .background(
                            color = Color.Black,
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            SurfaceView(it).also { view ->
                                surfaceView = view
                            }
                        },
                    )

                    if (!permissionGranted) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.RECORD_AUDIO,
                                    ),
                                )
                            },
                        ) {
                            Text("Allow camera and microphone")
                        }
                    } else if (!cameraStarted) {
                        Button(
                            onClick = {
                                cameraRequested = true
                            },
                        ) {
                            Text("Start camera preview")
                        }
                    }
                }

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Stream endpoint")
                    },
                    placeholder = {
                        Text("rtmps://... or srt://...")
                    },
                    singleLine = true,
                )

                Button(
                    onClick = {
                        engine.toggleStream(endpoint)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        permissionGranted &&
                            cameraStarted &&
                            endpoint.isNotBlank(),
                ) {
                    Text(
                        if (engine.isStreaming) {
                            "Stop"
                        } else {
                            "Go live"
                        },
                    )
                }

                Text(
                    text =
                        "Test profile: 720p30, 2.5 Mbps video, " +
                            "128 Kbps audio",
                    color = Color(0xFFD7E2F4),
                )

                Text(
                    text =
                        "Next milestone: standard SRT to your " +
                            "home Jetson Nano.",
                    color = Color(0xFFD7E2F4),
                )
            }
        }
    }
}

private fun hasCameraAndMicrophonePermissions(
    context: android.content.Context,
): Boolean {
    val camera =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    val microphone =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    return camera && microphone
}

@Composable
private fun NetworkPill(
    name: String,
    active: Boolean,
) {
    val colour = if (active) {
        Color(0xFF51E1A6)
    } else {
        Color(0xFF9AA7BA)
    }

    Text(
        text = "$name ${if (active) "●" else "○"}",
        color = colour,
    )
}
