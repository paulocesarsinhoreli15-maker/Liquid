package com.liquidglass.iptv

import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@Composable
fun GText(
    text: String,
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalLG.current.ink,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    spacing: Float = -0.3f
) {
    Text(
        text, modifier = modifier, color = color, fontSize = size.sp, fontWeight = weight,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, letterSpacing = spacing.sp
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0x59000000))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        GText(text, 12, FontWeight.Bold, Color.White, maxLines = 1, spacing = 0.2f)
    }
}

/** Pôster / capa com efeito de foco (o resto desfoca e perde a cor). */
@Composable
fun PosterCard(
    item: Item,
    modifier: Modifier,
    dimmed: Boolean,
    fx: Boolean,
    onFocus: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(
        if (focused) 1.06f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "sc"
    )
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .dim(dimmed, fx)
            .clip(shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x33FFFFFF), shape)
            .onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }
            .clickable(onClick = onClick)
    ) {
        Art(item.id + item.title, Modifier.fillMaxSize())
        if (item.logo != null) {
            AsyncImage(
                model = item.logo, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xD0000000))
            )
        )
        val r = item.rating.toDoubleOrNull()
        if (r != null && r > 0) {
            Pill(
                String.format("%.1f", r),
                Modifier.align(Alignment.TopEnd).padding(10.dp),
                Icons.Rounded.Star
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            GText(item.title, 14, FontWeight.Bold, Color.White, maxLines = 2)
            GText(item.category, 11, FontWeight.Medium, Color(0xCCFFFFFF), maxLines = 1)
        }
    }
}

@Composable
fun ChannelRow(
    item: Item,
    selected: Boolean,
    dimmed: Boolean,
    fx: Boolean,
    onFocus: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val t = LocalLG.current
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    val bg by animateColorAsState(if (selected) t.thumb else Color.Transparent, tween(400), label = "bg")
    Row(
        Modifier.fillMaxWidth()
            .dim(dimmed, fx, 3.dp)
            .clip(shape)
            .background(bg)
            .border(if (f) 2.dp else 0.dp, t.ink, shape)
            .onFocusChanged { f = it.isFocused; onFocus(it.isFocused) }
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Art(item.id, Modifier.fillMaxSize())
            if (item.logo != null) {
                AsyncImage(
                    model = item.logo, contentDescription = null,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(5.dp)
                )
            } else {
                GText(item.title.take(1).uppercase(), 18, FontWeight.ExtraBold, Color.White)
            }
        }
        Column(Modifier.weight(1f)) {
            GText(item.title, 16, FontWeight.Bold, maxLines = 1)
            GText(item.category, 13, FontWeight.Normal, t.ink2, maxLines = 1)
        }
        if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(t.ink))
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val t = LocalLG.current
    var f by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (on) t.onBg else Color.Transparent, tween(350), label = "c")
    val fg by animateColorAsState(if (on) t.onInk else t.ink2, tween(350), label = "f")
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.height(44.dp).clip(shape).background(bg)
            .border(if (f) 2.dp else 0.dp, t.ink, shape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) { GText(text, 15, FontWeight.SemiBold, fg, maxLines = 1) }
}

@Composable
fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.glass(RoundedCornerShape(30.dp)).horizontalScroll(rememberScrollState()).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, o -> Chip(o, i == selected) { onSelect(i) } }
    }
}

@Composable
fun GButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val t = LocalLG.current
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (f) 1.05f else 1f, label = "s")
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier.height(52.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .then(if (primary) Modifier.clip(shape).background(t.onBg) else Modifier.glass(shape))
            .border(if (f) 2.dp else 0.dp, t.ink, shape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, null, tint = if (primary) t.onInk else t.ink, modifier = Modifier.size(22.dp))
        GText(label, 16, FontWeight.Bold, if (primary) t.onInk else t.ink, maxLines = 1)
    }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit) {
    val t = LocalLG.current
    var f by remember { mutableStateOf(false) }
    Box(
        Modifier.size(46.dp).glass(CircleShape)
            .border(if (f) 2.dp else 0.dp, t.ink, CircleShape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = t.ink, modifier = Modifier.size(22.dp)) }
}

@Composable
fun GToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val t = LocalLG.current
    val x by animateDpAsState(
        if (on) 30.dp else 4.dp,
        spring(dampingRatio = 0.6f, stiffness = 400f), label = "x"
    )
    val bg by animateColorAsState(if (on) t.onBg else t.line, tween(350), label = "bg")
    val shape = RoundedCornerShape(19.dp)
    Box(
        Modifier.size(66.dp, 38.dp).clip(shape).background(bg).border(1.dp, t.line, shape)
            .clickable { onChange(!on) }
    ) {
        Box(
            Modifier.offset(x = x, y = 3.dp).size(32.dp).clip(CircleShape)
                .background(if (on) t.onInk else t.ink)
        )
    }
}

@Composable
fun GField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Uri
) {
    val t = LocalLG.current
    Column(modifier) {
        GText(label, 13, FontWeight.SemiBold, t.ink2)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 15.dp)
        ) {
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = t.ink, fontSize = 16.sp),
                cursorBrush = SolidColor(t.ink),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else keyboard,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Player Media3 (HLS/TS/MP4). Se o .m3u8 falhar, tenta o mesmo canal em .ts. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ExoView(url: String, modifier: Modifier, controls: Boolean, fallbackTs: Boolean) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var err by remember(url) { mutableStateOf<String?>(null) }
    val exo = remember {
        val ds = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) LiquidIPTV/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
        ExoPlayer.Builder(ctx).setMediaSourceFactory(DefaultMediaSourceFactory(ds)).build()
    }
    // declarado primeiro para ser liberado por último
    DisposableEffect(Unit) { onDispose { exo.release() } }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) exo.pause()
            if (e == Lifecycle.Event.ON_START) exo.play()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    DisposableEffect(url) {
        var tried = false
        val l = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (!tried && fallbackTs && url.endsWith(".m3u8")) {
                    tried = true
                    exo.setMediaItem(MediaItem.fromUri(url.removeSuffix(".m3u8") + ".ts"))
                    exo.prepare()
                    exo.playWhenReady = true
                } else {
                    err = "Não foi possível reproduzir (${error.errorCodeName})"
                }
            }
        }
        exo.addListener(l)
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        exo.playWhenReady = true
        onDispose { exo.removeListener(l) }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    player = exo
                    useController = controls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                    visibility = View.VISIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        err?.let {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GText(it, 15, FontWeight.Medium, Color.White, Modifier.padding(24.dp))
            }
        }
    }
}
