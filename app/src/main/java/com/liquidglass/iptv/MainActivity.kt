@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.liquidglass.iptv


import androidx.compose.animation.ExperimentalAnimationApi
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: AppVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm) }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Início", Icons.Rounded.Home),
    Tab("Filmes", Icons.Rounded.Movie),
    Tab("Séries", Icons.Rounded.Tv),
    Tab("TV ao Vivo", Icons.Rounded.LiveTv),
    Tab("Provedores", Icons.Rounded.Dns),
    Tab("Configurações", Icons.Rounded.Settings)
)

@Composable
fun App(vm: AppVM) {
    val dark = when (vm.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val t = if (dark) DarkLG else LightLG

    CompositionLocalProvider(LocalLG provides t) {
        var tab by rememberSaveable { mutableStateOf(if (vm.providers.isEmpty()) 4 else 0) }
        var detail by remember { mutableStateOf<Item?>(null) }
        var shown by remember { mutableStateOf<Item?>(null) }
        var playing by remember { mutableStateOf<PlayReq?>(null) }
        if (detail != null) shown = detail

        BackHandler(playing != null || detail != null) {
            if (playing != null) playing = null else detail = null
        }
        LaunchedEffect(tab) { vm.query = "" }

        Box(Modifier.fillMaxSize().background(t.bg)) {
            val p = playing
            if (p == null) {
                // Ao abrir um item, todo o app desfoca e perde a cor atrás do painel de detalhe.
                Box(Modifier.fillMaxSize().dim(detail != null, vm.fx, 18.dp, 0.55f)) {
                    LGBackground()
                    Row(
                        Modifier.fillMaxSize().systemBarsPadding().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SideRail(tab) { tab = it }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            TopBar(tab, vm) { vm.updateTheme(if (dark) "light" else "dark") }
                            vm.error?.let { msg ->
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 10.dp).glass(RoundedCornerShape(20.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GText(msg, 14, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2)
                                    GButton("Tentar de novo", primary = true) { vm.error = null; vm.reload() }
                                }
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                AnimatedContent(
                                    targetState = tab,
                                    transitionSpec = {
                                        (fadeIn(tween(480, delayMillis = 120)) + scaleIn(tween(480, delayMillis = 120), initialScale = 0.97f)) togetherWith
                                            (fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.98f))
                                    },
                                    label = "tab"
                                ) { target ->
                                    // desfoque na entrada e na saída de cada aba
                                    val b by transition.animateDp(label = "tabBlur", transitionSpec = { tween(450) }) { s ->
                                        if (s == EnterExitState.Visible) 0.dp else 16.dp
                                    }
                                    val bm = if (Build.VERSION.SDK_INT >= 31 && b > 0.dp) Modifier.blur(b) else Modifier
                                    Box(Modifier.fillMaxSize().then(bm)) {
                                        when (target) {
                                            0 -> HomeScreen(vm, { detail = it }, { tab = 4 })
                                            1 -> GridScreen(vm.movies, vm.query, vm.fx, vm.loading, "Nenhum filme neste provedor.") { detail = it }
                                            2 -> GridScreen(vm.series, vm.query, vm.fx, vm.loading, "Nenhuma série neste provedor.") { detail = it }
                                            3 -> LiveScreen(vm) { playing = PlayReq(it.title, it.url) }
                                            4 -> ProvidersScreen(vm) { tab = 0 }
                                            else -> SettingsScreen(vm)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = detail != null,
                    enter = fadeIn(tween(350)) + scaleIn(tween(520), initialScale = 0.9f),
                    exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.95f)
                ) {
                    shown?.let { s ->
                        DetailSheet(s, vm, onClose = { detail = null }) { title, url ->
                            detail = null
                            playing = PlayReq(title, url)
                        }
                    }
                }
            } else {
                PlayerScreen(p, vm.fallbackTs) { playing = null }
            }
        }
    }
}

@Composable
private fun SideRail(tab: Int, onTab: (Int) -> Unit) {
    val t = LocalLG.current
    val itemH = 52.dp
    val gap = 6.dp
    Column(
        Modifier.fillMaxHeight().width(76.dp).glass(RoundedCornerShape(38.dp))
            .verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(t.onBg),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.PlayArrow, null, tint = t.onInk, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(20.dp))
        Box {
            val y by animateDpAsState((itemH + gap) * tab, spring(dampingRatio = 0.65f, stiffness = 300f), label = "ind")
            Box(Modifier.offset(y = y).size(itemH).clip(RoundedCornerShape(18.dp)).background(t.onBg))
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                TABS.forEachIndexed { i, tb ->
                    var f by remember { mutableStateOf(false) }
                    Box(
                        Modifier.size(itemH).clip(RoundedCornerShape(18.dp))
                            .border(if (f) 2.dp else 0.dp, t.ink, RoundedCornerShape(18.dp))
                            .onFocusChanged { f = it.isFocused }
                            .clickable { onTab(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tb.icon, tb.label, tint = if (i == tab) t.onInk else t.ink3, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(tab: Int, vm: AppVM, onTheme: () -> Unit) {
    val t = LocalLG.current
    val sub = when (tab) {
        0 -> vm.active?.name ?: "Bem-vindo"
        1 -> "${vm.movies.size} títulos"
        2 -> "${vm.series.size} séries"
        3 -> "${vm.live.size} canais"
        4 -> "Xtream Codes e listas M3U"
        else -> "Personalize sua experiência"
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            GText(TABS[tab].label, 30, FontWeight.ExtraBold, maxLines = 1, spacing = -1f)
            GText(sub, 14, color = t.ink2, maxLines = 1)
        }
        if (tab in 1..3) {
            Row(
                Modifier.weight(1f).widthIn(max = 380.dp).height(48.dp).glass(RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = t.ink2, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (vm.query.isEmpty()) GText("Buscar", 15, color = t.ink3)
                    BasicTextField(
                        value = vm.query, onValueChange = { vm.query = it }, singleLine = true,
                        textStyle = TextStyle(color = t.ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(t.ink), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        IconBtn(if (t.dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, onTheme)
    }
}

@Composable
fun PlayerScreen(req: PlayReq, fallbackTs: Boolean, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ExoView(req.url, Modifier.fillMaxSize(), controls = true, fallbackTs = fallbackTs)
        Row(
            Modifier.systemBarsPadding().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBtn(Icons.Rounded.ArrowBack, onBack)
            GText(req.title, 16, FontWeight.Bold, Color.White, maxLines = 1)
        }
    }
}
