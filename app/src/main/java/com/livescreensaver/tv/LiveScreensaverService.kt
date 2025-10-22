package com.livescreensaver.tv

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.service.dreams.DreamService
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class LiveScreensaverService : DreamService(), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "LiveScreensaverService"
        private const val PREF_VIDEO_URL = "video_url"
        const val DEFAULT_VIDEO_URL = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_adv_example_hevc/master.m3u8"
        private const val CACHE_DURATION = 300L
        private const val MAX_RETRIES = 3
        private const val STALL_TIMEOUT_MS = 10000L
        private const val STALL_CHECK_INTERVAL_MS = 1000L
    }

    private var player: ExoPlayer? = null
    private var surfaceView: SurfaceView? = null
    private lateinit var streamExtractor: StreamExtractor
    private lateinit var preferences: SharedPreferences
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var retryCount = 0
    private var currentSourceUrl: String? = null
    private var stallDetectionTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var surfaceReady = false
    private var isRetrying = false

    private val stallCheckRunnable = object : Runnable {
        override fun run() {
            checkForStall()
            handler.postDelayed(this, STALL_CHECK_INTERVAL_MS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        streamExtractor = StreamExtractor(this)

        setupSurface()
    }

    private fun setupSurface() {
        surfaceView = SurfaceView(this).apply {
            holder.addCallback(this@LiveScreensaverService)
        }
        setContentView(surfaceView)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
        surfaceReady = true
        initializePlayer()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: ${width}x${height}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        surfaceReady = false
        releasePlayer()
    }

    private fun initializePlayer() {
        if (player != null || !surfaceReady) return

        val videoUrl = preferences.getString(
            PREF_VIDEO_URL,
            DEFAULT_VIDEO_URL
        ) ?: DEFAULT_VIDEO_URL

        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                setVideoSurfaceView(surfaceView)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d(TAG, "Player ready")
                                stallDetectionTime = 0
                                retryCount = 0
                            }
                            Player.STATE_BUFFERING -> Log.d(TAG, "Player buffering")
                            Player.STATE_ENDED -> Log.d(TAG, "Playback ended")
                            Player.STATE_IDLE -> Log.d(TAG, "Player idle")
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}", error)
                        handlePlaybackFailure()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            stallDetectionTime = 0
                        } else if (playbackState == Player.STATE_READY) {
                            if (stallDetectionTime == 0L) {
                                stallDetectionTime = System.currentTimeMillis()
                            }
                        }
                    }
                })
            }

        loadStream(videoUrl)
        handler.post(stallCheckRunnable)
    }

    private fun loadStream(sourceUrl: String) {
        currentSourceUrl = sourceUrl
        Log.d(TAG, "Loading stream: $sourceUrl")

        serviceScope.launch {
            try {
                val streamUrl = if (streamExtractor.needsExtraction(sourceUrl)) {
                    val cachedUrl = streamExtractor.extractStreamUrl(
                        sourceUrl,
                        forceRefresh = false,
                        cacheExpirationSeconds = CACHE_DURATION
                    )

                    if (cachedUrl != null) {
                        launch(Dispatchers.IO) {
                            streamExtractor.extractStreamUrl(
                                sourceUrl,
                                forceRefresh = true,
                                cacheExpirationSeconds = CACHE_DURATION
                            )
                        }
                        cachedUrl
                    } else {
                        streamExtractor.extractStreamUrl(
                            sourceUrl,
                            forceRefresh = true,
                            cacheExpirationSeconds = CACHE_DURATION
                        )
                    }
                } else {
                    sourceUrl
                }

                if (streamUrl != null) {
                    withContext(Dispatchers.Main) {
                        playStream(streamUrl)
                    }
                } else {
                    Log.e(TAG, "Failed to extract stream URL")
                    handlePlaybackFailure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stream", e)
                handlePlaybackFailure()
            }
        }
    }

    private fun playStream(streamUrl: String) {
        try {
            Log.d(TAG, "Playing stream: $streamUrl")

            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))

            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing stream", e)
            handlePlaybackFailure()
        }
    }

    private fun checkForStall() {
        if (stallDetectionTime > 0) {
            val stallDuration = System.currentTimeMillis() - stallDetectionTime
            if (stallDuration > STALL_TIMEOUT_MS) {
                Log.w(TAG, "Stream stalled for ${stallDuration}ms, retrying")
                handlePlaybackFailure()
            }
        }
    }

    private fun handlePlaybackFailure() {
        if (isRetrying) {
            return
        }

        if (retryCount >= MAX_RETRIES) {
            Log.e(TAG, "Max retries reached, giving up")
            handler.removeCallbacks(stallCheckRunnable)
            return
        }

        isRetrying = true
        retryCount++
        Log.d(TAG, "Retry attempt $retryCount of $MAX_RETRIES")

        currentSourceUrl?.let { url ->
            if (streamExtractor.needsExtraction(url)) {
                streamExtractor.clearCache()
            }
        }

        val delay = Math.pow(2.0, (retryCount - 1).toDouble()).toLong() * 1000
        handler.postDelayed({
            retryPlayback()
        }, delay)
    }

    private fun retryPlayback() {
        stallDetectionTime = 0
        isRetrying = false
        player?.stop()
        player?.clearMediaItems()

        currentSourceUrl?.let { url ->
            loadStream(url)
        }

        handler.removeCallbacks(stallCheckRunnable)
        handler.post(stallCheckRunnable)
    }

    private fun releasePlayer() {
        handler.removeCallbacksAndMessages(null)
        isRetrying = false
        retryCount = 0
        stallDetectionTime = 0
        player?.release()
        player = null
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d(TAG, "Dream started")
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.d(TAG, "Dream stopped")
        releasePlayer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        serviceScope.cancel()
        releasePlayer()
    }

    

    private class StreamExtractor(private val context: Context) {

        companion object {
            private const val TAG = "StreamExtractor"
            private const val CACHE_DIR = "stream_cache"
            private const val LOCK_SUFFIX = "_lock"
            private const val EXTRACTION_TIMEOUT_MS = 15000L
            private val EXPIRATION_PATTERN = Pattern.compile("expire/(\\d+)")

            init {
                NewPipe.init(DownloaderImpl())
            }
        }

        private val cacheDir: File by lazy {
            File(context.cacheDir, CACHE_DIR).apply {
                if (!exists()) mkdirs()
            }
        }

        fun needsExtraction(url: String): Boolean = !url.contains(".m3u8")

        suspend fun extractStreamUrl(
            sourceUrl: String,
            forceRefresh: Boolean = false,
            cacheExpirationSeconds: Long = 300
        ): String? = withContext(Dispatchers.IO) {
            try {
                if (!needsExtraction(sourceUrl)) {
                    return@withContext sourceUrl
                }

                if (!forceRefresh) {
                    getCachedUrl(sourceUrl, cacheExpirationSeconds)?.let { cached ->
                        Log.d(TAG, "Using cached URL for $sourceUrl")
                        return@withContext cached
                    }
                }

                val lockFile = getLockFile(sourceUrl)
                if (lockFile.exists()) {
                    val lockAge = System.currentTimeMillis() - lockFile.lastModified()
                    if (lockAge < EXTRACTION_TIMEOUT_MS) {
                        Log.d(TAG, "Extraction already in progress for $sourceUrl")
                        return@withContext null
                    } else {
                        lockFile.delete()
                    }
                }

                lockFile.createNewFile()

                try {
                    val extractedUrl = extractFromYouTube(sourceUrl)
                    if (extractedUrl != null) {
                        cacheUrl(sourceUrl, extractedUrl)
                        Log.d(TAG, "Successfully extracted URL from $sourceUrl")
                    }
                    extractedUrl
                } finally {
                    lockFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting stream URL", e)
                null
            }
        }

        private suspend fun extractFromYouTube(url: String): String? = withContext(Dispatchers.IO) {
            try {
                val streamInfo = StreamInfo.getInfo(url)

                streamInfo.hlsUrl?.let { hlsUrl ->
                    Log.d(TAG, "Found HLS URL: $hlsUrl")
                    return@withContext hlsUrl
                }

                val videoStreams = streamInfo.videoStreams
                if (videoStreams.isNotEmpty()) {
                    val bestStream = videoStreams.maxByOrNull { it.bitrate ?: 0 }
                    bestStream?.url?.let { streamUrl ->
                        Log.d(TAG, "Found video stream URL: $streamUrl")
                        return@withContext streamUrl
                    }
                }

                val videoOnlyStreams = streamInfo.videoOnlyStreams
                if (videoOnlyStreams.isNotEmpty()) {
                    val bestStream = videoOnlyStreams.maxByOrNull { it.bitrate ?: 0 }
                    bestStream?.url?.let { streamUrl ->
                        Log.d(TAG, "Found video-only stream URL: $streamUrl")
                        return@withContext streamUrl
                    }
                }

                Log.w(TAG, "No suitable stream found for $url")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting from YouTube", e)
                null
            }
        }

        private fun getCachedUrl(sourceUrl: String, cacheExpirationSeconds: Long): String? {
            val cacheFile = getCacheFile(sourceUrl)
            if (!cacheFile.exists()) return null

            try {
                val cachedUrl = cacheFile.readText().trim()

                val expirationTime = extractExpirationTime(cachedUrl)
                if (expirationTime != null) {
                    val currentTime = System.currentTimeMillis() / 1000
                    if (currentTime < expirationTime) {
                        return cachedUrl
                    } else {
                        cacheFile.delete()
                        return null
                    }
                }

                val fileAge = System.currentTimeMillis() - cacheFile.lastModified()
                if (fileAge < cacheExpirationSeconds * 1000) {
                    return cachedUrl
                } else {
                    cacheFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading cache", e)
            }

            return null
        }

        private fun cacheUrl(sourceUrl: String, extractedUrl: String) {
            try {
                val cacheFile = getCacheFile(sourceUrl)
                cacheFile.writeText(extractedUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error caching URL", e)
            }
        }

        private fun extractExpirationTime(url: String): Long? {
            val matcher = EXPIRATION_PATTERN.matcher(url)
            return if (matcher.find()) {
                matcher.group(1)?.toLongOrNull()
            } else {
                null
            }
        }

        private fun getCacheFile(sourceUrl: String): File {
            val hash = sourceUrl.hashCode().toString()
            return File(cacheDir, "stream_$hash")
        }

        private fun getLockFile(sourceUrl: String): File {
            val hash = sourceUrl.hashCode().toString()
            return File(cacheDir, "stream_${hash}$LOCK_SUFFIX")
        }

        fun clearCache() {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }



    private class DownloaderImpl : Downloader() {

        private val client = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        override fun execute(request: NPRequest): NPResponse {
            val httpMethod = request.httpMethod()
            val url = request.url()
            val headers = request.headers()
            val dataToSend = request.dataToSend()

            val requestBuilder = Request.Builder()
                .method(httpMethod, if (dataToSend != null) {
                    okhttp3.RequestBody.create(null, dataToSend)
                } else {
                    null
                })
                .url(url)

            headers.forEach { (key, values) ->
                values.forEach { value ->
                    requestBuilder.addHeader(key, value)
                }
            }

            val response: Response = try {
                client.newCall(requestBuilder.build()).execute()
            } catch (e: Exception) {
                throw e
            }

            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val body = response.body?.string() ?: ""
            val responseHeaders = mutableMapOf<String, MutableList<String>>()

            response.headers.names().forEach { name ->
                responseHeaders[name] = response.headers.values(name).toMutableList()
            }

            return NPResponse(
                response.code,
                response.message,
                responseHeaders,
                body,
                url
            )
        }
    }
}
