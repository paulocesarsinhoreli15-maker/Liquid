package com.liquidglass.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class DetailInfo(
    val plot: String = "",
    val year: String = "",
    val eps: List<Episode> = emptyList(),
    val loading: Boolean = false
)

/** Tela de detalhes: o fundo desfoca e fica cinza (ver App) e este painel de vidro aparece por cima. */
@Composable
fun DetailSheet(item: Item, vm: AppVM, onClose: () -> Unit, onPlay: (String, String) -> Unit) {
    val t = LocalLG.current
    val p = vm.active
    val info by produceState(DetailInfo(plot = item.plot, year = item.year), item.id, p?.id) {
        if (p != null && p.type == PType.XTREAM) {
            try {
                if (item.kind == Kind.MOVIE) {
                    val (pl, yr) = Xtream.vodInfo(p, item.id.drop(1))
                    value = value.copy(plot = pl.ifBlank { item.plot }, year = yr.ifBlank { item.year })
                } else if (item.kind == Kind.SERIES && item.url.isEmpty()) {
                    value = value.copy(loading = true)
                    val e = Xtream.episodes(p, item.id.drop(1))
                    value = DetailInfo(item.plot, item.year, e, false)
                }
            } catch (e: Exception) {
                value = value.copy(loading = false)
            }
        }
    }
    var season by remember(item.id) { mutableStateOf(0) }
    val seasons = info.eps.map { it.season }.distinct()
    val shownEps = if (seasons.isEmpty()) emptyList() else info.eps.filter { it.season == seasons[season.coerceIn(0, seasons.size - 1)] }

    fun play(title: String, url: String) {
        vm.addRecent(item)
        onPlay(title, url)
    }

    val none = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(if (t.dark) Color(0x66000000) else Color(0x66FFFFFF))
                .clickable(interactionSource = none, indication = null, onClick = onClose)
        )
        BoxWithConstraints(
            Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val wide = maxWidth > 720.dp
            val panelMod = Modifier
                .widthIn(max = 640.dp)
                .heightIn(max = maxHeight)
                .glass(RoundedCornerShape(40.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(26.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                if (wide) {
                    Box(
                        Modifier.width(240.dp).aspectRatio(0.68f).clip(RoundedCornerShape(34.dp))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(34.dp))
                    ) {
                        Art(item.id + item.title, Modifier.fillMaxSize())
                        if (item.logo != null) {
                            AsyncImage(item.logo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Column(panelMod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GText(
                            when (item.kind) { Kind.LIVE -> "CANAL"; Kind.MOVIE -> "FILME"; Kind.SERIES -> "SÉRIE" },
                            12, FontWeight.Bold, t.ink2, Modifier.weight(1f), spacing = 1.8f
                        )
                        IconBtn(Icons.Rounded.Close, onClose)
                    }
                    GText(item.title, 32, FontWeight.ExtraBold, maxLines = 4, spacing = -1f)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val r = item.rating.toDoubleOrNull()
                        if (r != null && r > 0) Pill(String.format("%.1f", r), icon = Icons.Rounded.Star)
                        if (info.year.isNotBlank()) Pill(info.year)
                        Pill(item.category)
                    }
                    if (info.plot.isNotBlank()) GText(info.plot, 15, color = t.ink2, maxLines = 8, spacing = 0f)

                    val direct = item.kind != Kind.SERIES || item.url.isNotEmpty()
                    if (direct) {
                        Spacer(Modifier.height(6.dp))
                        GButton("Assistir agora", icon = Icons.Rounded.PlayArrow, primary = true) { play(item.title, item.url) }
                    } else {
                        if (info.loading) GText("Carregando episódios…", 14, color = t.ink2)
                        if (seasons.isNotEmpty()) {
                            ChipRow(seasons.map { "Temporada $it" }, season.coerceIn(0, seasons.size - 1), { season = it })
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                shownEps.forEach { e ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                            .background(t.line)
                                            .clickable { play("${item.title} · T${e.season} E${e.num}", e.url) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        GText("${e.num}", 16, FontWeight.ExtraBold, t.ink2, Modifier.width(26.dp))
                                        GText(e.title, 15, FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                                        androidx.compose.material3.Icon(Icons.Rounded.PlayArrow, null, tint = t.ink)
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
