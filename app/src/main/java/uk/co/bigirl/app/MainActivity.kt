package uk.co.bigirl.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val configuration = LocalConfiguration.current

    val isLandscape =
        configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE

    var status by remember {
        mutableStateOf("Ready")
    }

    var endpoint by rememberSaveable {
        mutableStateOf("")
    }

    var settingsOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var controlsOnLeft by rememberSaveable {
        mutableStateOf(false)
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

    /*
     * These counters are connected to the interface now.
     *
     * They remain zero until the multipath transport is implemented,
     * because the current single-stream transport does not expose
     * trustworthy per-network bitrate figures.
     */
    var wifiKbps by remember {
        mutableIntStateOf(0)
    }

    var cellularKbps by remember {
        mutableIntStateOf(0)
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
                "Permissions granted"
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

            if (!networks.wifi) {
                wifiKbps = 0
            }

            if (!networks.cellular) {
                cellularKbps = 0
            }

            delay(2_000)
        }
    }

   LaunchedEffect(
    cameraRequested,
    surfaceView,
    permissionGranted,
    cameraStarted,
) {
    val preview = surfaceView

    if (
        !cameraRequested ||
        preview == null ||
        !permissionGranted
    ) {
        return@LaunchedEffect
    }

    if (!cameraStarted) {
        status = "Starting camera"

        cameraStarted = engine.startPreview(
            surfaceView = preview,
            profile = StreamProfile(),
        )
    } else {
        /*
         * Rotation can replace the Android SurfaceView even though the
         * Activity and camera engine remain alive. Attach the existing
         * camera pipeline to the newly created preview surface.
         */
        engine.attachPreviewSurface(preview)
    }
}
    BackHandler(
        enabled = settingsOpen,
    ) {
        settingsOpen = false
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
          Box(
    modifier = Modifier.fillMaxSize(),
) {
    if (isLandscape) {
        LandscapeStudio(
            permissionGranted = permissionGranted,
            cameraStarted = cameraStarted,
            engine = engine,
            networks = networks,
            wifiKbps = wifiKbps,
            cellularKbps = cellularKbps,
            status = status,
            endpointReady = endpoint.isNotBlank(),
            controlsOnLeft = controlsOnLeft,
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                    ),
                )
            },
            onStartCamera = {
                cameraRequested = true
            },
            onSurfaceReady = {
                surfaceView = it
            },
            onOpenSettings = {
                settingsOpen = true
            },
        )
    } else {
        PortraitStudio(
            permissionGranted = permissionGranted,
            cameraStarted = cameraStarted,
            engine = engine,
            networks = networks,
            wifiKbps = wifiKbps,
            cellularKbps = cellularKbps,
            status = status,
            endpointReady = endpoint.isNotBlank(),
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                    ),
                )
            },
            onStartCamera = {
                cameraRequested = true
            },
            onSurfaceReady = {
                surfaceView = it
            },
            onOpenSettings = {
                settingsOpen = true
            },
        )
    }

    if (settingsOpen) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            SettingsScreen(
                endpoint = endpoint,
                onEndpointChanged = {
                    endpoint = it
                },
                controlsOnLeft = controlsOnLeft,
                onControlsOnLeftChanged = {
                    controlsOnLeft = it
                },
                onClose = {
                    settingsOpen = false
                },
            )
        }
    }
}
                        endpoint = endpoint,
                        onEndpointChanged = {
                            endpoint = it
                        },
                        controlsOnLeft = controlsOnLeft,
                        onControlsOnLeftChanged = {
                            controlsOnLeft = it
                        },
                        onClose = {
                            settingsOpen = false
                        },
                    )
                }

                isLandscape -> {
                    LandscapeStudio(
                        permissionGranted = permissionGranted,
                        cameraStarted = cameraStarted,
                        engine = engine,
                        networks = networks,
                        wifiKbps = wifiKbps,
                        cellularKbps = cellularKbps,
                        status = status,
                        endpointReady = endpoint.isNotBlank(),
                        controlsOnLeft = controlsOnLeft,
                        onRequestPermissions = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO,
                                ),
                            )
                        },
                        onStartCamera = {
                            cameraRequested = true
                        },
                        onSurfaceReady = {
                            surfaceView = it
                        },
                        onOpenSettings = {
                            settingsOpen = true
                        },
                    )
                }

                else -> {
                    PortraitStudio(
                        permissionGranted = permissionGranted,
                        cameraStarted = cameraStarted,
                        engine = engine,
                        networks = networks,
                        wifiKbps = wifiKbps,
                        cellularKbps = cellularKbps,
                        status = status,
                        endpointReady = endpoint.isNotBlank(),
                        onRequestPermissions = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO,
                                ),
                            )
                        },
                        onStartCamera = {
                            cameraRequested = true
                        },
                        onSurfaceReady = {
                            surfaceView = it
                        },
                        onOpenSettings = {
                            settingsOpen = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeStudio(
    permissionGranted: Boolean,
    cameraStarted: Boolean,
    engine: StreamEngine,
    networks: NetworkSummary,
    wifiKbps: Int,
    cellularKbps: Int,
    status: String,
    endpointReady: Boolean,
    controlsOnLeft: Boolean,
    onRequestPermissions: () -> Unit,
    onStartCamera: () -> Unit,
    onSurfaceReady: (SurfaceView) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (controlsOnLeft) {
            FutureControlSpace(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight(),
                engine = engine,
                cameraStarted = cameraStarted,
                endpointReady = endpointReady,
                onOpenSettings = onOpenSettings,
            )
        }

        PreviewPanel(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            permissionGranted = permissionGranted,
            cameraStarted = cameraStarted,
            engine = engine,
            networks = networks,
            wifiKbps = wifiKbps,
            cellularKbps = cellularKbps,
            status = status,
            onRequestPermissions = onRequestPermissions,
            onStartCamera = onStartCamera,
            onSurfaceReady = onSurfaceReady,
        )

        if (!controlsOnLeft) {
            FutureControlSpace(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight(),
                engine = engine,
                cameraStarted = cameraStarted,
                endpointReady = endpointReady,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun PortraitStudio(
    permissionGranted: Boolean,
    cameraStarted: Boolean,
    engine: StreamEngine,
    networks: NetworkSummary,
    wifiKbps: Int,
    cellularKbps: Int,
    status: String,
    endpointReady: Boolean,
    onRequestPermissions: () -> Unit,
    onStartCamera: () -> Unit,
    onSurfaceReady: (SurfaceView) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BIG IRL APP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = onOpenSettings,
            ) {
                Text("Settings")
            }
        }

        PreviewPanel(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            permissionGranted = permissionGranted,
            cameraStarted = cameraStarted,
            engine = engine,
            networks = networks,
            wifiKbps = wifiKbps,
            cellularKbps = cellularKbps,
            status = status,
            onRequestPermissions = onRequestPermissions,
            onStartCamera = onStartCamera,
            onSurfaceReady = onSurfaceReady,
        )

        Button(
            onClick = {
                /*
                 * Streaming is started from this button once a saved endpoint
                 * exists. The settings page owns the endpoint field.
                 */
            },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                cameraStarted &&
                    endpointReady,
        ) {
            Text(
                if (engine.isStreaming) {
                    "Stop stream"
                } else {
                    "Go live"
                },
            )
        }

        if (!endpointReady) {
            Text(
                text = "Add a stream endpoint in Settings.",
                color = Color(0xFFB8C4D8),
            )
        }
    }
}

@Composable
private fun PreviewPanel(
    modifier: Modifier,
    permissionGranted: Boolean,
    cameraStarted: Boolean,
    engine: StreamEngine,
    networks: NetworkSummary,
    wifiKbps: Int,
    cellularKbps: Int,
    status: String,
    onRequestPermissions: () -> Unit,
    onStartCamera: () -> Unit,
    onSurfaceReady: (SurfaceView) -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .onSizeChanged { size ->
                engine.updatePreviewSize(
                    width = size.width,
                    height = size.height,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                SurfaceView(it).also(onSurfaceReady)
            },
        )

        CompactStatusOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            networks = networks,
            wifiKbps = wifiKbps,
            cellularKbps = cellularKbps,
            status = status,
        )

        if (!permissionGranted) {
            Button(
                onClick = onRequestPermissions,
            ) {
                Text("Allow camera and microphone")
            }
        } else if (!cameraStarted) {
            Button(
                onClick = onStartCamera,
            ) {
                Text("Start camera preview")
            }
        }
    }
}

@Composable
private fun CompactStatusOverlay(
    modifier: Modifier,
    networks: NetworkSummary,
    wifiKbps: Int,
    cellularKbps: Int,
    status: String,
) {
    Surface(
        modifier = modifier,
        color = Color(0xCC07101D),
        contentColor = Color.White,
        shape = RoundedCornerShape(9.dp),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CompactNetworkStat(
                name = "Wi-Fi",
                active = networks.wifi,
                kbps = wifiKbps,
            )

            CompactNetworkStat(
                name = "SIM",
                active = networks.cellular,
                kbps = cellularKbps,
            )

            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactNetworkStat(
    name: String,
    active: Boolean,
    kbps: Int,
) {
    val colour = if (active) {
        Color(0xFF51E1A6)
    } else {
        Color(0xFF8795A9)
    }

    Text(
        text = "$name ${if (active) "●" else "○"}  $kbps Kbps",
        color = colour,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun FutureControlSpace(
    modifier: Modifier,
    engine: StreamEngine,
    cameraStarted: Boolean,
    endpointReady: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121C2E),
            contentColor = Color.White,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(
                    modifier = Modifier.weight(1f),
                )

                TextButton(
                    onClick = onOpenSettings,
                ) {
                    Text("Settings")
                }
            }

            HorizontalDivider(
                color = Color(0xFF31405A),
            )

            Text(
                text = "Reserved for your next controls.",
                color = Color(0xFFB8C4D8),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = {
                    /*
                     * The final call is connected once saved endpoint state is
                     * passed into this control panel.
                     */
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    cameraStarted &&
                        endpointReady,
            ) {
                Text(
                    if (engine.isStreaming) {
                        "Stop stream"
                    } else {
                        "Go live"
                    },
                )
            }

            if (!endpointReady) {
                Text(
                    text = "Set the endpoint in Settings.",
                    color = Color(0xFFB8C4D8),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    endpoint: String,
    onEndpointChanged: (String) -> Unit,
    controlsOnLeft: Boolean,
    onControlsOnLeftChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = onClose,
            ) {
                Text("Done")
            }
        }

        HorizontalDivider(
            color = Color(0xFF31405A),
        )

        Text(
            text = "Streaming",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpointChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Stream endpoint")
            },
            placeholder = {
                Text("rtmps://... or srt://...")
            },
            supportingText = {
                Text(
                    "The endpoint stays inside the settings screen.",
                )
            },
            singleLine = true,
        )

        Text(
            text = "Layout",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Left-handed controls",
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = if (controlsOnLeft) {
                            "Control space appears on the left."
                        } else {
                            "Control space appears on the right."
                        },
                        color = Color(0xFFB8C4D8),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Switch(
                    checked = controlsOnLeft,
                    onCheckedChange = onControlsOnLeftChanged,
                )
            }
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Return to camera")
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
