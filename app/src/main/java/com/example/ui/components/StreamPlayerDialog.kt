package com.example.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.StreamItem
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SportsCyan
import com.example.ui.theme.TrophyGold

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun StreamPlayerDialog(
    title: String,
    subtitle: String,
    currentStream: StreamItem,
    allStreams: List<StreamItem>,
    onSelectStream: (StreamItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isVideoBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    var forceWebEmbedMode by remember { mutableStateOf(false) }

    val shouldUseWeb = currentStream.isIframeOrWeb || forceWebEmbedMode

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("stream_player_dialog"),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Title, Subtitle, Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("close_player_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }
                }

                // Video Cinema Surface (16:9 Aspect Ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasError) {
                        key(currentStream.cleanUrl, reloadKey, shouldUseWeb) {
                            if (shouldUseWeb) {
                                // Enhanced HTML5 & Web Embed Stream Player
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                mediaPlaybackRequiresUserGesture = false
                                                loadWithOverviewMode = true
                                                useWideViewPort = true
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                            }
                                            webChromeClient = WebChromeClient()
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    isVideoBuffering = false
                                                }
                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    errorCode: Int,
                                                    description: String?,
                                                    failingUrl: String?
                                                ) {
                                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                                    isVideoBuffering = false
                                                    hasError = true
                                                    errorMessage = description ?: "Playback stream error"
                                                }
                                            }

                                            // If it's a direct m3u8 in web mode, load an embedded HTML5 video player wrapper
                                            if (currentStream.cleanUrl.contains(".m3u8", ignoreCase = true) || currentStream.cleanUrl.contains(".mp4", ignoreCase = true)) {
                                                val htmlData = """
                                                    <!DOCTYPE html>
                                                    <html>
                                                    <head>
                                                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                                      <style>
                                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                                        video { width: 100%; height: 100%; object-fit: contain; }
                                                      </style>
                                                      <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
                                                    </head>
                                                    <body>
                                                      <video id="videoPlayer" controls autoplay playsinline></video>
                                                      <script>
                                                        var video = document.getElementById('videoPlayer');
                                                        var streamUrl = '${currentStream.cleanUrl}';
                                                        if (Hls.isSupported()) {
                                                          var hls = new Hls();
                                                          hls.loadSource(streamUrl);
                                                          hls.attachMedia(video);
                                                          hls.on(Hls.Events.MANIFEST_PARSED, function() {
                                                            video.play();
                                                          });
                                                        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                                                          video.src = streamUrl;
                                                          video.play();
                                                        } else {
                                                          video.src = streamUrl;
                                                          video.play();
                                                        }
                                                      </script>
                                                    </body>
                                                    </html>
                                                """.trimIndent()
                                                loadDataWithBaseURL("https://mukulsports.live", htmlData, "text/html", "UTF-8", null)
                                            } else {
                                                loadUrl(currentStream.cleanUrl)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // ExoPlayer Media3 Production Engine
                                val exoPlayer = remember(currentStream.cleanUrl, reloadKey) {
                                    val userAgent = currentStream.headers["User-Agent"]
                                        ?: "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                                        .setUserAgent(userAgent)
                                        .setAllowCrossProtocolRedirects(true)
                                        .setConnectTimeoutMs(15000)
                                        .setReadTimeoutMs(15000)

                                    if (currentStream.headers.isNotEmpty()) {
                                        httpDataSourceFactory.setDefaultRequestProperties(currentStream.headers)
                                    }

                                    val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

                                    ExoPlayer.Builder(context)
                                        .setMediaSourceFactory(mediaSourceFactory)
                                        .build().apply {
                                            val uri = Uri.parse(currentStream.cleanUrl)
                                            val mediaItem = MediaItem.Builder()
                                                .setUri(uri)
                                                .build()
                                            setMediaItem(mediaItem)
                                            prepare()
                                            playWhenReady = true
                                        }
                                }

                                DisposableEffect(exoPlayer) {
                                    val listener = object : Player.Listener {
                                        override fun onPlaybackStateChanged(playbackState: Int) {
                                            when (playbackState) {
                                                Player.STATE_BUFFERING -> {
                                                    isVideoBuffering = true
                                                    hasError = false
                                                }
                                                Player.STATE_READY -> {
                                                    isVideoBuffering = false
                                                    hasError = false
                                                }
                                                Player.STATE_ENDED -> {
                                                    isVideoBuffering = false
                                                }
                                                Player.STATE_IDLE -> {}
                                            }
                                        }

                                        override fun onPlayerError(error: PlaybackException) {
                                            isVideoBuffering = false
                                            hasError = true
                                            errorMessage = error.localizedMessage ?: "Stream source buffer error"
                                        }
                                    }
                                    exoPlayer.addListener(listener)

                                    onDispose {
                                        exoPlayer.removeListener(listener)
                                        exoPlayer.stop()
                                        exoPlayer.release()
                                    }
                                }

                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = true
                                            setShowNextButton(false)
                                            setShowPreviousButton(false)
                                            controllerAutoShow = true
                                            controllerShowTimeoutMs = 3000
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    update = { playerView ->
                                        playerView.player = exoPlayer
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Buffering Indicator
                    if (isVideoBuffering && !hasError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = SportsCyan,
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading Mukul Sports Live Stream...",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Error & Fallback View
                    if (hasError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TrophyGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Stream source temporarily unreachable.",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        hasError = false
                                        isVideoBuffering = true
                                        reloadKey++
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }

                                if (!shouldUseWeb) {
                                    OutlinedButton(
                                        onClick = {
                                            forceWebEmbedMode = true
                                            hasError = false
                                            isVideoBuffering = true
                                            reloadKey++
                                        }
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = SportsCyan)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Web Player", color = SportsCyan)
                                    }
                                }
                            }
                        }
                    }
                }

                // Stream & Channel Switcher Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = SportsCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Available Broadcast Channels",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${allStreams.size} Server${if (allStreams.size > 1) "s" else ""}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        // Horizontal Server Selection Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allStreams.forEach { stream ->
                                val isSelected = stream.cleanUrl == currentStream.cleanUrl
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!isSelected) {
                                                forceWebEmbedMode = false
                                                isVideoBuffering = true
                                                hasError = false
                                                onSelectStream(stream)
                                            }
                                        },
                                    color = if (isSelected) SportsCyan else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = stream.channelName,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
