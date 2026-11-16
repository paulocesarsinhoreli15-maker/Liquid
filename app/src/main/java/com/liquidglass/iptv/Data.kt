package com.liquidglass.iptv

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/* ===================== MODELOS ===================== */

enum class Kind { LIVE, MOVIE, SERIES }
enum class PType { XTREAM, M3U }

data class Provider(
    val id: String,
    val name: String,
    val type: PType,
    val server: String = "",
    val user: String = "",
    val pass: String = "",
    val m3u: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("type", type.name)
        .put("server", server).put("user", user).put("pass", pass).put("m3u", m3u)

    companion object {
        fun from(o: JSONObject) = Provider(
            o.getString("id"), o.getString("name"), PType.valueOf(o.getString("type")),
            o.optString("server"), o.optString("user"), o.optString("pass"), o.optString("m3u")
        )
    }
}

data class Item(
    val id: String,
    val title: String,
    val logo: String?,
    val category: String,
    val kind: Kind,
    val url: String = "",
    val rating: String = "",
    val year: String = "",
    val plot: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("logo", logo ?: "").put("category", category)
        .put("kind", kind.name).put("url", url).put("rating", rating).put("year", year).put("plot", plot)

    companion object {
        fun from(o: JSONObject) = Item(
            o.getString("id"), o.getString("title"), o.optString("logo").ifBlank { null },
            o.optString("category"), Kind.valueOf(o.getString("kind")), o.optString("url"),
            o.optString("rating"), o.optString("year"), o.optString("plot")
        )
    }
}

data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val num: Int,
    val url: String,
    val plot: String = ""
)

data class PlayReq(val title: String, val url: String)

/* ===================== REDE ===================== */

object Net {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) LiquidIPTV/1.0")
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Servidor respondeu HTTP ${r.code}")
            r.body?.string() ?: ""
        }
    }
}

/* ===================== XTREAM CODES ===================== */

object Xtream {
    fun base(p: Provider): String {
        var s = p.server.trim().trimEnd('/')
        if (!s.startsWith("http", ignoreCase = true)) s = "http://$s"
        return s
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun api(p: Provider, action: String? = null, extra: String = ""): String =
        base(p) + "/player_api.php?username=" + enc(p.user) + "&password=" + enc(p.pass) +
            (if (action != null) "&action=$action" else "") + extra

    /** Retorna null se estiver tudo certo; senão a mensagem de erro. */
    suspend fun auth(p: Provider): String? {
        val o = JSONObject(Net.get(api(p)))
        val ui = o.optJSONObject("user_info") ?: return "Resposta inválida do servidor"
        if (ui.optString("auth") != "1") return "Usuário ou senha incorretos"
        val st = ui.optString("status")
        if (st.isNotEmpty() && !st.equals("Active", ignoreCase = true)) return "Conta com status: $st"
        return null
    }

    private suspend fun cats(p: Provider, action: String): Map<String, String> {
        val a = JSONArray(Net.get(api(p, action)))
        return withContext(Dispatchers.Default) {
            val m = HashMap<String, String>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                m[o.optString("category_id")] = o.optString("category_name")
            }
            m
        }
    }

    suspend fun live(p: Provider): List<Item> {
        val c = cats(p, "get_live_categories")
        val a = JSONArray(Net.get(api(p, "get_live_streams")))
        val b = base(p)
        return withContext(Dispatchers.Default) {
            val out = ArrayList<Item>(a.length())
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val id = o.optString("stream_id")
                out.add(
                    Item(
                        "l$id", o.optString("name"), o.optString("stream_icon").ifBlank { null },
                        c[o.optString("category_id")] ?: "Outros", Kind.LIVE,
                        "$b/live/${p.user}/${p.pass}/$id.m3u8"
                    )
                )
            }
            out
        }
    }

    suspend fun movies(p: Provider): List<Item> {
        val c = cats(p, "get_vod_categories")
        val a = JSONArray(Net.get(api(p, "get_vod_streams")))
        val b = base(p)
        return withContext(Dispatchers.Default) {
            val out = ArrayList<Item>(a.length())
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val id = o.optString("stream_id")
                val ext = o.optString("container_extension").ifBlank { "mp4" }
                out.add(
                    Item(
                        "v$id", o.optString("name"),
                        o.optString("stream_icon").ifBlank { null },
                        c[o.optString("category_id")] ?: "Outros", Kind.MOVIE,
                        "$b/movie/${p.user}/${p.pass}/$id.$ext",
                        rating = o.optString("rating")
                    )
                )
            }
            out
        }
    }

    suspend fun series(p: Provider): List<Item> {
        val c = cats(p, "get_series_categories")
        val a = JSONArray(Net.get(api(p, "get_series")))
        return withContext(Dispatchers.Default) {
            val out = ArrayList<Item>(a.length())
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val id = o.optString("series_id")
                val year = o.optString("year").ifBlank { o.optString("releaseDate").take(4) }
                out.add(
                    Item(
                        "s$id", o.optString("name"), o.optString("cover").ifBlank { null },
                        c[o.optString("category_id")] ?: "Outros", Kind.SERIES, "",
                        rating = o.optString("rating"), year = year, plot = o.optString("plot")
                    )
                )
            }
            out
        }
    }

    /** Sinopse e ano de um filme. */
    suspend fun vodInfo(p: Provider, vodId: String): Pair<String, String> {
        val o = JSONObject(Net.get(api(p, "get_vod_info", "&vod_id=$vodId")))
        val info = o.optJSONObject("info") ?: return "" to ""
        val plot = info.optString("plot").ifBlank { info.optString("description") }
        val year = info.optString("releasedate").take(4)
        return plot to year
    }

    suspend fun episodes(p: Provider, seriesId: String): List<Episode> {
        val o = JSONObject(Net.get(api(p, "get_series_info", "&series_id=$seriesId")))
        val b = base(p)
        return withContext(Dispatchers.Default) {
            val eps = o.optJSONObject("episodes") ?: return@withContext emptyList<Episode>()
            val out = ArrayList<Episode>()
            val keys = eps.keys()
            while (keys.hasNext()) {
                val sk = keys.next()
                val arr = eps.optJSONArray(sk) ?: continue
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    val id = e.optString("id")
                    val ext = e.optString("container_extension").ifBlank { "mp4" }
                    val num = e.optInt("episode_num")
                    out.add(
                        Episode(
                            id, e.optString("title").ifBlank { "Episódio $num" },
                            sk.toIntOrNull() ?: 1, num,
                            "$b/series/${p.user}/${p.pass}/$id.$ext",
                            e.optJSONObject("info")?.optString("plot") ?: ""
                        )
                    )
                }
            }
            out.sortedWith(compareBy({ it.season }, { it.num }))
        }
    }

    /** "Agora / Depois" de um canal (guia curto). */
    suspend fun nowNext(p: Provider, streamId: String): List<String> {
        val o = JSONObject(Net.get(api(p, "get_short_epg", "&stream_id=$streamId&limit=2")))
        val arr = o.optJSONArray("epg_listings") ?: return emptyList()
        val out = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i).optString("title")
            if (t.isNotBlank()) {
                out.add(
                    try { String(Base64.decode(t, Base64.DEFAULT)) } catch (e: Exception) { t }
                )
            }
        }
        return out
    }

    /** Aceita um link get.php?username=..&password=.. e extrai servidor/usuário/senha. */
    fun parseGetPhp(url: String): Triple<String, String, String>? {
        val u = Uri.parse(url.trim())
        val user = u.getQueryParameter("username")
        val pass = u.getQueryParameter("password")
        val host = u.host
        val scheme = u.scheme
        if (user == null || pass == null || host == null || scheme == null) return null
        val port = if (u.port != -1) ":${u.port}" else ""
        return Triple("$scheme://$host$port", user, pass)
    }
}

/* ===================== M3U ===================== */

object M3u {
    private fun attr(line: String, key: String): String? =
        Regex(key + "=\"([^\"]*)\"").find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

    suspend fun parse(text: String): List<Item> = withContext(Dispatchers.Default) {
        val out = ArrayList<Item>()
        var name = ""
        var logo: String? = null
        var group = "Sem categoria"
        var n = 0
        for (raw in text.lineSequence()) {
            val l = raw.trim()
            if (l.startsWith("#EXTINF")) {
                logo = attr(l, "tvg-logo")
                group = attr(l, "group-title") ?: "Sem categoria"
                name = l.substringAfterLast(",").trim()
                if (name.isEmpty()) name = attr(l, "tvg-name") ?: "Canal"
            } else if (l.isNotEmpty() && !l.startsWith("#")) {
                val kind = when {
                    l.contains("/movie/") -> Kind.MOVIE
                    l.contains("/series/") -> Kind.SERIES
                    else -> Kind.LIVE
                }
                out.add(Item("m3u${n++}", name.ifEmpty { "Canal $n" }, logo, group, kind, l))
                name = ""; logo = null; group = "Sem categoria"
            }
        }
        out
    }
}

/* ===================== PREFERÊNCIAS ===================== */

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("liquid_iptv", Context.MODE_PRIVATE)

    fun providers(): List<Provider> = try {
        val a = JSONArray(sp.getString("providers", "[]"))
        (0 until a.length()).map { Provider.from(a.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }

    fun saveProviders(l: List<Provider>) {
        val a = JSONArray()
        l.forEach { a.put(it.toJson()) }
        sp.edit().putString("providers", a.toString()).apply()
    }

    fun activeId(): String? = sp.getString("active", null)
    fun setActiveId(id: String?) { sp.edit().putString("active", id).apply() }

    fun theme(): String = sp.getString("theme", "dark") ?: "dark"
    fun setTheme(v: String) { sp.edit().putString("theme", v).apply() }
    fun fx(): Boolean = sp.getBoolean("fx", true)
    fun setFx(v: Boolean) { sp.edit().putBoolean("fx", v).apply() }
    fun fallbackTs(): Boolean = sp.getBoolean("fallbackTs", true)
    fun setFallbackTs(v: Boolean) { sp.edit().putBoolean("fallbackTs", v).apply() }

    fun recents(): List<Item> = try {
        val a = JSONArray(sp.getString("recents", "[]"))
        (0 until a.length()).map { Item.from(a.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }

    fun saveRecents(l: List<Item>) {
        val a = JSONArray()
        l.forEach { a.put(it.toJson()) }
        sp.edit().putString("recents", a.toString()).apply()
    }
}

/* ===================== VIEWMODEL ===================== */

class AppVM(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var providers by mutableStateOf(prefs.providers()); private set
    var activeId by mutableStateOf(prefs.activeId()); private set
    var theme by mutableStateOf(prefs.theme()); private set
    var fx by mutableStateOf(prefs.fx()); private set
    var fallbackTs by mutableStateOf(prefs.fallbackTs()); private set

    var live by mutableStateOf<List<Item>>(emptyList()); private set
    var movies by mutableStateOf<List<Item>>(emptyList()); private set
    var series by mutableStateOf<List<Item>>(emptyList()); private set
    var recents by mutableStateOf(prefs.recents()); private set

    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var liveCur by mutableStateOf<Item?>(null)

    val active: Provider? get() = providers.firstOrNull { it.id == activeId }
    private var job: Job? = null

    init { active?.let { load(it) } }

    fun updateTheme(v: String) { theme = v; prefs.setTheme(v) }
    fun updateFx(v: Boolean) { fx = v; prefs.setFx(v) }
    fun updateFallbackTs(v: Boolean) { fallbackTs = v; prefs.setFallbackTs(v) }

    fun save(p: Provider) {
        providers = providers.filter { it.id != p.id } + p
        prefs.saveProviders(providers)
    }

    fun select(p: Provider) {
        activeId = p.id
        prefs.setActiveId(p.id)
        liveCur = null
        load(p)
    }

    fun remove(p: Provider) {
        providers = providers.filter { it.id != p.id }
        prefs.saveProviders(providers)
        if (activeId == p.id) {
            val next = providers.firstOrNull()
            activeId = next?.id
            prefs.setActiveId(next?.id)
            if (next != null) load(next) else {
                job?.cancel()
                live = emptyList(); movies = emptyList(); series = emptyList()
                loading = false
            }
        }
    }

    fun reload() { active?.let { load(it) } }

    private fun load(p: Provider) {
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            error = null
            live = emptyList(); movies = emptyList(); series = emptyList()
            try {
                when (p.type) {
                    PType.XTREAM -> {
                        Xtream.auth(p)?.let { throw IllegalStateException(it) }
                        live = Xtream.live(p)
                        movies = Xtream.movies(p)
                        series = Xtream.series(p)
                    }
                    PType.M3U -> {
                        val all = M3u.parse(Net.get(p.m3u))
                        live = all.filter { it.kind == Kind.LIVE }
                        movies = all.filter { it.kind == Kind.MOVIE }
                        series = all.filter { it.kind == Kind.SERIES }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Falha ao carregar o conteúdo"
            } finally {
                loading = false
            }
        }
    }

    suspend fun test(p: Provider): String? = try {
        when (p.type) {
            PType.XTREAM -> Xtream.auth(p)
            PType.M3U -> if (Net.get(p.m3u).contains("#EXTINF")) null else "A lista não parece ser um M3U válido"
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.message ?: "Não foi possível conectar"
    }

    fun addRecent(i: Item) {
        recents = (listOf(i) + recents.filter { it.id != i.id }).take(15)
        prefs.saveRecents(recents)
    }
}
