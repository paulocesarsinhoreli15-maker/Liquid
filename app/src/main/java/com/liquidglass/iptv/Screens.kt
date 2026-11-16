package com.liquidglass.iptv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID

/* ============ estados vazios / carregando ============ */

@Composable
fun EmptyState(title: String, text: String, button: String?, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 460.dp).glass(RoundedCornerShape(36.dp)).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GText(title, 24, FontWeight.ExtraBold)
            GText(text, 15, color = LocalLG.current.ink2)
            if (button != null) {
                Spacer(Modifier.height(6.dp))
                GButton(button, primary = true, onClick = onClick)
            }
        }
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = LocalLG.current.ink, strokeWidth = 3.dp)
            GText("Carregando seu conteúdo…", 15, color = LocalLG.current.ink2)
        }
    }
}

/* ============ INÍCIO ============ */

@Composable
fun HomeScreen(vm: AppVM, onOpen: (Item) -> Unit, onProviders: () -> Unit) {
    if (vm.active == null) {
        EmptyState(
            "Adicione seu provedor",
            "Cadastre um provedor Xtream Codes ou uma lista M3U para carregar canais, filmes e séries.",
            "Adicionar provedor", onProviders
        )
        return
    }
    if (vm.loading && vm.live.isEmpty() && vm.movies.isEmpty()) { Loading(); return }
    var focusId by remember { mutableStateOf<String?>(null) }
    val hero = vm.movies.firstOrNull { it.logo != null } ?: vm.series.firstOrNull { it.logo != null }
        ?: vm.movies.firstOrNull() ?: vm.series.firstOrNull() ?: vm.live.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        if (hero != null) item { Hero(hero, focusId != null, vm.fx, onOpen) }
        if (vm.recents.isNotEmpty()) item {
            Rail("Assistidos recentemente", vm.recents, 200, 120, focusId, vm.fx, { focusId = it }, onOpen)
        }
        if (vm.movies.isNotEmpty()) item {
            Rail("Filmes", vm.movies.take(24), 150, 220, focusId, vm.fx, { focusId = it }, onOpen)
        }
        if (vm.series.isNotEmpty()) item {
            Rail("Séries", vm.series.take(24), 150, 220, focusId, vm.fx, { focusId = it }, onOpen)
        }
        if (vm.live.isNotEmpty()) item {
            Rail("Canais ao vivo", vm.live.take(24), 200, 120, focusId, vm.fx, { focusId = it }, onOpen)
        }
    }
}

@Composable
private fun Hero(item: Item, dimmed: Boolean, fx: Boolean, onOpen: (Item) -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    Box(
        Modifier.fillMaxWidth().height(250.dp).dim(dimmed, fx).clip(shape)
            .border(1.dp, Color(0x33FFFFFF), shape).clickable { onOpen(item) }
    ) {
        Art(item.id + item.title, Modifier.fillMaxSize())
        if (item.logo != null) {
            AsyncImage(item.logo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color(0xCC000000), 0.75f to Color(0x11000000))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Em destaque")
            GText(item.title, 32, FontWeight.ExtraBold, Color.White, Modifier.widthIn(max = 520.dp), maxLines = 2, spacing = -1f)
            GButton("Assistir", icon = Icons.Rounded.PlayArrow, primary = true) { onOpen(item) }
        }
    }
}

@Composable
private fun Rail(
    title: String,
    list: List<Item>,
    w: Int,
    h: Int,
    focusId: String?,
    fx: Boolean,
    onFocus: (String?) -> Unit,
    onOpen: (Item) -> Unit
) {
    Column {
        GText(title, 22, FontWeight.Bold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 4.dp)
        ) {
            items(list, key = { it.id }) { it2 ->
                PosterCard(
                    it2, Modifier.size(w.dp, h.dp),
                    dimmed = focusId != null && focusId != it2.id, fx = fx,
                    onFocus = { f -> onFocus(if (f) it2.id else null) },
                    onClick = { onOpen(it2) }
                )
            }
        }
    }
}

/* ============ FILMES / SÉRIES (grade com categorias) ============ */

@Composable
fun GridScreen(all: List<Item>, query: String, fx: Boolean, loading: Boolean, empty: String, onOpen: (Item) -> Unit) {
    if (all.isEmpty()) {
        if (loading) Loading() else EmptyState("Nada por aqui", empty, null) {}
        return
    }
    val cats = remember(all) { listOf("Todos") + all.map { it.category }.distinct().take(60) }
    var sel by rememberSaveable { mutableStateOf(0) }
    if (sel >= cats.size) sel = 0
    val list = remember(all, sel, query) {
        all.asSequence()
            .filter { (sel == 0 || it.category == cats[sel]) && (query.isBlank() || it.title.contains(query, true)) }
            .take(300).toList()
    }
    var focusId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        ChipRow(cats, sel, { sel = it })
        Spacer(Modifier.height(14.dp))
        if (list.isEmpty()) {
            EmptyState("Nenhum resultado", "Tente outra categoria ou outra busca.", null) {}
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                gridItems(list, key = { it.id }) { it2 ->
                    PosterCard(
                        it2, Modifier.fillMaxWidth().aspectRatio(0.68f),
                        dimmed = focusId != null && focusId != it2.id, fx = fx,
                        onFocus = { f -> focusId = if (f) it2.id else null },
                        onClick = { onOpen(it2) }
                    )
                }
            }
        }
    }
}

/* ============ TV AO VIVO ============ */

@Composable
fun LiveScreen(vm: AppVM, onFull: (Item) -> Unit) {
    if (vm.live.isEmpty()) {
        if (vm.loading) Loading() else EmptyState("Sem canais", "Adicione um provedor na aba Provedores.", null) {}
        return
    }
    val cats = remember(vm.live) { listOf("Todos") + vm.live.map { it.category }.distinct().take(60) }
    var sel by rememberSaveable { mutableStateOf(0) }
    if (sel >= cats.size) sel = 0
    val q = vm.query
    val list = remember(vm.live, sel, q) {
        vm.live.asSequence()
            .filter { (sel == 0 || it.category == cats[sel]) && (q.isBlank() || it.title.contains(q, true)) }
            .take(500).toList()
    }
    var focusId by remember { mutableStateOf<String?>(null) }
    val cur = vm.liveCur

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 720.dp
        val player: @Composable (Modifier) -> Unit = { m -> LivePane(cur, vm, onFull, m) }
        val listPane: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                ChipRow(cats, sel, { sel = it })
                Spacer(Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                    items(list, key = { it.id }) { c ->
                        ChannelRow(
                            c, selected = cur?.id == c.id,
                            dimmed = focusId != null && focusId != c.id, fx = vm.fx,
                            onFocus = { f -> focusId = if (f) c.id else null },
                            onClick = { vm.liveCur = c; vm.addRecent(c) }
                        )
                    }
                }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listPane(Modifier.width(380.dp).fillMaxHeight())
                player(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                player(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                listPane(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LivePane(cur: Item?, vm: AppVM, onFull: (Item) -> Unit, modifier: Modifier) {
    val t = LocalLG.current
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.clip(shape).border(1.dp, t.line, shape).background(Color.Black)) {
        if (cur == null) {
            Box(Modifier.fillMaxSize().glass(shape), contentAlignment = Alignment.Center) {
                GText("Escolha um canal para assistir", 16, color = t.ink2)
            }
        } else {
            ExoView(cur.url, Modifier.fillMaxSize(), controls = false, fallbackTs = vm.fallbackTs)
            val p = vm.active
            val epg by produceState(emptyList<String>(), cur.id, p?.id) {
                value = try {
                    if (p != null && p.type == PType.XTREAM) Xtream.nowNext(p, cur.id.drop(1)) else emptyList()
                } catch (e: Exception) { emptyList() }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC000000))
                )
            )
            Row(
                Modifier.align(Alignment.TopStart).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { Pill("AO VIVO"); Pill("HD") }
            Box(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                IconBtn(Icons.Rounded.Fullscreen) { onFull(cur) }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                GText(cur.title, 22, FontWeight.ExtraBold, Color.White, maxLines = 1)
                if (epg.isNotEmpty()) GText("Agora: " + epg[0] + (if (epg.size > 1) "  ·  Depois: " + epg[1] else ""), 13, color = Color(0xCCFFFFFF), maxLines = 1)
            }
        }
    }
}

/* ============ PROVEDORES ============ */

@Composable
fun ProvidersScreen(vm: AppVM, onLoaded: () -> Unit) {
    val t = LocalLG.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(PType.XTREAM) }
    var name by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var m3u by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun build(): Provider? {
        val nm = name.ifBlank { "Meu provedor" }
        if (type == PType.XTREAM) {
            var s = server; var u = user; var p = pass
            if (s.contains("username=")) {
                Xtream.parseGetPhp(s)?.let { s = it.first; u = it.second; p = it.third; server = s; user = u; pass = p }
            }
            if (s.isBlank() || u.isBlank() || p.isBlank()) {
                status = "Preencha servidor, usuário e senha."; ok = false; return null
            }
            return Provider(UUID.randomUUID().toString(), nm, PType.XTREAM, server = s.trim(), user = u.trim(), pass = p.trim())
        }
        if (m3u.isBlank()) { status = "Cole o link da lista M3U."; ok = false; return null }
        return Provider(UUID.randomUUID().toString(), nm, PType.M3U, m3u = m3u.trim())
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 50.dp)
    ) {
        item { GText("Seus provedores", 22, FontWeight.Bold) }
        if (vm.providers.isEmpty()) {
            item { GText("Nenhum provedor cadastrado ainda. Adicione o seu logo abaixo.", 15, color = t.ink2) }
        }
        items(vm.providers, key = { it.id }) { p ->
            val isActive = p.id == vm.activeId
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(t.onBg),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Dns, null, tint = t.onInk, modifier = Modifier.size(26.dp)) }
                Column(Modifier.weight(1f)) {
                    GText(p.name, 18, FontWeight.Bold, maxLines = 1)
                    val host = if (p.type == PType.XTREAM)
                        runCatching { Uri.parse(Xtream.base(p)).host }.getOrNull() ?: "" else "Lista M3U"
                    GText((if (p.type == PType.XTREAM) "Xtream · " else "") + host, 13, color = t.ink2, maxLines = 1)
                    if (isActive) GText(if (vm.loading) "Carregando…" else "Ativo", 12, FontWeight.Bold, t.ink)
                }
                if (!isActive) GButton("Usar", primary = true) { vm.select(p); onLoaded() }
                else IconBtn(Icons.Rounded.Refresh) { vm.reload() }
                IconBtn(Icons.Rounded.Delete) { vm.remove(p) }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(34.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GText("Adicionar provedor", 20, FontWeight.Bold)
                ChipRow(listOf("Xtream Codes", "Lista M3U"), if (type == PType.XTREAM) 0 else 1, {
                    type = if (it == 0) PType.XTREAM else PType.M3U; status = null
                })
                GField(name, { name = it }, "Nome do provedor", keyboard = KeyboardType.Text)
                if (type == PType.XTREAM) {
                    GField(server, { server = it }, "Servidor (ex.: http://seu-servidor.com:8080)")
                    GField(user, { user = it }, "Usuário", keyboard = KeyboardType.Text)
                    GField(pass, { pass = it }, "Senha", password = true)
                    GText("Dica: você também pode colar o link completo get.php?username=…&password=… no campo Servidor.", 12, color = t.ink3)
                } else {
                    GField(m3u, { m3u = it }, "Link da lista M3U / M3U8")
                }
                status?.let {
                    GText(it, 14, FontWeight.SemiBold, if (ok) t.ink else t.ink2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GButton(if (busy) "Testando…" else "Testar conexão") {
                        val p = build() ?: return@GButton
                        if (busy) return@GButton
                        scope.launch {
                            busy = true; status = null
                            val err = vm.test(p)
                            ok = err == null
                            status = err ?: "Conexão OK! Provedor válido."
                            busy = false
                        }
                    }
                    GButton("Salvar e usar", icon = Icons.Rounded.Add, primary = true) {
                        val p = build() ?: return@GButton
                        vm.save(p); vm.select(p)
                        name = ""; server = ""; user = ""; pass = ""; m3u = ""
                        status = null
                        onLoaded()
                    }
                }
            }
        }
    }
}

/* ============ CONFIGURAÇÕES ============ */

@Composable
private fun SettingRow(title: String, desc: String, control: @Composable () -> Unit) {
    val t = LocalLG.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            GText(title, 17, FontWeight.SemiBold)
            GText(desc, 13, color = t.ink2)
        }
        control()
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp)).padding(horizontal = 24.dp, vertical = 18.dp)) {
        GText(title, 12, FontWeight.Bold, LocalLG.current.ink2, spacing = 1.8f)
        content()
    }
}

@Composable
fun SettingsScreen(vm: AppVM) {
    val themes = listOf("Escuro", "Claro", "Sistema")
    val idx = when (vm.theme) { "dark" -> 0; "light" -> 1; else -> 2 }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 50.dp)
    ) {
        item {
            Section("APARÊNCIA") {
                SettingRow("Tema", "Preto e branco, escuro ou claro") {
                    ChipRow(themes, idx, { vm.updateTheme(listOf("dark", "light", "system")[it]) })
                }
                SettingRow("Efeito de foco", "Desfoca e tira a cor do resto da tela ao selecionar") {
                    GToggle(vm.fx) { vm.updateFx(it) }
                }
            }
        }
        item {
            Section("REPRODUÇÃO") {
                SettingRow("Tentar formato .ts", "Se o canal HLS (.m3u8) falhar, tenta o mesmo canal em .ts") {
                    GToggle(vm.fallbackTs) { vm.updateFallbackTs(it) }
                }
            }
        }
        item {
            Section("SOBRE") {
                SettingRow("Liquid IPTV", "Versão 1.0 · interface Liquid Glass") {}
                GText(
                    "Este app é apenas um reprodutor. Ele não fornece canais, filmes nem séries. " +
                        "Use somente provedores e listas que você tem direito de usar.",
                    13, color = LocalLG.current.ink2
                )
            }
        }
    }
}
