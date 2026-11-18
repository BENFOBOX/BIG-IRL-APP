package uk.co.bigirl.app

import android.content.Context
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream

class StreamEngine(
    context: Context,
    private val event: (String) -> Unit,
) : ConnectChecker {

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

            event(
                "Camera error: " +
                    (error.message ?: error.javaClass.simpleName),
            )

            false
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
        event("Connecting")
    }

    override fun onConnectionSuccess() {
        event("Live")
    }

    override fun onConnectionFailed(reason: String) {
        event("Connection failed: $reason")
    }

    override fun onNewBitrate(bitrate: Long) {
        event("Sending ${bitrate / 1000} Kbps")
    }

    override fun onDisconnect() {
        event("Disconnected")
    }

    override fun onAuthError() {
        event("Authentication failed")
    }

    override fun onAuthSuccess() {
        event("Authenticated")
    }
}
