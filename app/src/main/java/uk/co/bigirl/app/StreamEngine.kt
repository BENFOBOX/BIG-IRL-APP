package uk.co.bigirl.app

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream

class StreamEngine(
    context: Context,
    private val event: (String) -> Unit,
) : ConnectChecker {

    companion object {
        private const val TAG = "BIG_IRL_APP"
    }

    private val stream = GenericStream(
        context.applicationContext,
        this,
    ).apply {
        getGlInterface().autoHandleOrientation = true
    }

    private var prepared = false
    private var previewStarted = false

    fun startPreview(
        surfaceView: SurfaceView,
        profile: StreamProfile,
    ): Boolean {
        return try {
            if (!prepared) {
                event("Preparing camera and microphone")

                prepared =
                    stream.prepareVideo(
                        profile.width,
                        profile.height,
                        profile.videoBitrate,
                    ) &&
                    stream.prepareAudio(
                        profile.sampleRate,
                        true,
                        profile.audioBitrate,
                    )
            }

            if (!prepared) {
                event("Encoder preparation failed")
                return false
            }

            if (!stream.isOnPreview) {
                stream.startPreview(surfaceView)
            }

            previewStarted = true
            event("Camera ready")
            true
        } catch (error: Throwable) {
            prepared = false
            previewStarted = false

            Log.e(TAG, "Camera error", error)

            event(
                "Camera error: " +
                    (error.message ?: error.javaClass.simpleName),
            )

            false
        }
    }

    fun attachPreviewSurface(
        surfaceView: SurfaceView,
    ) {
        if (!prepared || !previewStarted) {
            return
        }

        runCatching {
            if (stream.isOnPreview) {
                stream.stopPreview()
            }

            stream.startPreview(surfaceView)

            event("Camera ready")
        }.onFailure { error ->
            Log.e(TAG, "Preview attach error", error)

            event(
                "Preview attach error: " +
                    (error.message ?: error.javaClass.simpleName),
            )
        }
    }

    fun updatePreviewSize(
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }

        if (!prepared) {
            return
        }

        runCatching {
            stream.getGlInterface().setPreviewResolution(
                width,
                height,
            )
        }.onFailure { error ->
            Log.e(TAG, "Preview resize error", error)

            event(
                "Preview resize error: " +
                    (error.message ?: error.javaClass.simpleName),
            )
        }
    }

    fun toggleStream(endpoint: String): Boolean {
        return try {
            if (!prepared || !previewStarted) {
                event("Start the camera first")
                return false
            }

            if (stream.isStreaming) {
                stream.stopStream()
                event("Stream stopped")
                return true
            }

            val cleanEndpoint = endpoint.trim()

            Log.d(TAG, "Starting stream to: $cleanEndpoint")

            val supported =
                cleanEndpoint.startsWith("rtmp://") ||
                    cleanEndpoint.startsWith("rtmps://") ||
                    cleanEndpoint.startsWith("srt://")

            if (!supported) {
                event("Enter a valid RTMP, RTMPS or SRT address")
                return false
            }

            stream.startStream(cleanEndpoint)
            true
        } catch (error: Throwable) {
            Log.e(TAG, "Streaming error", error)

            event(
                "Streaming error: " +
                    (error.message ?: error.javaClass.simpleName),
            )

            false
        }
    }

    fun release() {
        runCatching {
            if (stream.isStreaming) {
                stream.stopStream()
            }
        }

        runCatching {
            if (stream.isOnPreview) {
                stream.stopPreview()
            }
        }

        runCatching {
            stream.release()
        }

        previewStarted = false
        prepared = false
    }

    val isStreaming: Boolean
        get() = stream.isStreaming

    override fun onConnectionStarted(url: String) {
        Log.d(TAG, "Connection started: $url")
        event("Connecting")
    }

    override fun onConnectionSuccess() {
        Log.i(TAG, "Connection success")
        event("Live")
    }

    override fun onConnectionFailed(reason: String) {
        Log.e(TAG, "Connection failed: $reason")
        event("Connection failed: $reason")
    }

    override fun onNewBitrate(bitrate: Long) {
        event("Sending ${bitrate / 1000} Kbps")
    }

    override fun onDisconnect() {
        Log.w(TAG, "Disconnected")
        event("Disconnected")
    }

    override fun onAuthError() {
        Log.e(TAG, "Authentication failed")
        event("Authentication failed")
    }

    override fun onAuthSuccess() {
        Log.i(TAG, "Authentication success")
        event("Authenticated")
    }
}
