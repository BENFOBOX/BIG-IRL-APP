package uk.co.bigirl.app
import android.content.Context
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream
class StreamEngine(c:Context,private val event:(String)->Unit):ConnectChecker{
 private val stream=GenericStream(c.applicationContext,this);private var ready=false
 fun prepare(p:StreamProfile):Boolean{if(ready)return true;ready=try{stream.prepareVideo(p.width,p.height,p.videoBitrate)&&stream.prepareAudio(p.sampleRate,true,p.audioBitrate)}catch(e:IllegalArgumentException){event("Encoder error: ${e.message}");false};event(if(ready)"Encoder ready" else "Encoder failed");return ready}
 fun preview(v:SurfaceView){if(!stream.isOnPreview)stream.startPreview(v)}
 fun toggle(url:String){if(stream.isStreaming)stream.stopStream() else if(ready)stream.startStream(url.trim())}
 fun release(){if(stream.isStreaming)stream.stopStream();if(stream.isOnPreview)stream.stopPreview();stream.release()}
 val live:Boolean get()=stream.isStreaming
 override fun onConnectionStarted(url:String)=event("Connecting")
 override fun onConnectionSuccess()=event("Live")
 override fun onConnectionFailed(reason:String)=event("Failed: $reason")
 override fun onNewBitrate(bitrate:Long)=event("${bitrate/1000} Kbps")
 override fun onDisconnect()=event("Disconnected")
 override fun onAuthError()=event("Authentication failed")
 override fun onAuthSuccess()=event("Authenticated")
}