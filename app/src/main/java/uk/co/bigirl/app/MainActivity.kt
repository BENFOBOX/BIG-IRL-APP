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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}
@Composable private fun App(){val c=LocalContext.current;var status by remember{mutableStateOf("Ready")};val engine=remember{StreamEngine(c){status=it}};var endpoint by remember{mutableStateOf("")};var nets by remember{mutableStateOf(readNetworkSummary(c))};var allowed by remember{mutableStateOf(ContextCompat.checkSelfPermission(c,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(c,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)};val ask=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){allowed=it.values.all{v->v}};DisposableEffect(Unit){onDispose{engine.release()}};LaunchedEffect(Unit){while(true){nets=readNetworkSummary(c);delay(2000)}};MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF51E1A6))){Surface(Modifier.fillMaxSize(),color=Color(0xFF0B1220)){Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("BIG IRL APP",style=MaterialTheme.typography.headlineMedium);Text("Original alpha • direct RTMP/SRT contribution");Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF121C2E))){Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Pill("Wi-Fi",nets.wifi);Pill("Cellular",nets.cellular);Text(status)}};Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){if(allowed)AndroidView(modifier=Modifier.fillMaxSize(),factory={SurfaceView(it).also{v->if(engine.prepare(StreamProfile()))engine.preview(v)}})else Button(onClick={ask.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))}){Text("Allow camera and microphone")}};OutlinedTextField(endpoint,{endpoint=it},Modifier.fillMaxWidth(),label={Text("Stream endpoint")},placeholder={Text("rtmps://... or srt://...")},singleLine=true);Button({engine.toggle(endpoint)},Modifier.fillMaxWidth(),enabled=allowed&&endpoint.isNotBlank()){Text(if(engine.live)"Stop" else "Go live")};Text("Test profile: 720p30, 2.5 Mbps video, 128 Kbps audio");Text("Next milestone: standard SRT to your home Jetson Nano.")}}}}
@Composable private fun Pill(n:String,on:Boolean){Text("$n ${if(on)"●" else "○"}",color=if(on)Color(0xFF51E1A6)else Color(0xFF748096))}