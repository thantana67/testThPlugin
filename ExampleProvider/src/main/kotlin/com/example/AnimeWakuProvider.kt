package com.toycs

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.Episode

class AnimeWakuProvider : MainAPI() {
    override var mainUrl = "https://anime-waku.com/" // ใส่ URL หลักปัจจุบันของเว็บ
    override var name = "AnimeWaku"
    override val hasMainPage = true
    override var lang = "th"
    override val supportedTypes = setOf(TvType.Anime)

    // 1. หน้าแรก (Home)
    // 1. หน้าแรก (Home)
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val animeList = document.select("div.animepost").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = "อนิเมะอัปเดตล่าสุด",
                    list = animeList,
                    isHorizontalImages = false
                )
            ),
            hasNext = false
        )
    }

    // 2. ค้นหา (Search)
    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("div.animepost").mapNotNull { it.toSearchResult() }
    }

    // ฟังก์ชันช่วยแปลงข้อมูล Element ของ HTML ไปเป็น SearchResponse
    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title, h4")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    // 3. หน้ารวมตอนและรายละเอียด (Load)
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.entry-title")?.text() ?: "Unknown"
        val poster = fixUrlNull(doc.selectFirst("div.thumb img")?.attr("src"))
        val description = doc.selectFirst("div.entry-content")?.text()

        val episodes = doc.select("ul.episodelist li a").mapIndexed { index, ep ->
            newEpisode(fixUrl(ep.attr("href"))) {
                this.name = ep.text().ifBlank { "ตอนที่ ${index + 1}" }
                this.episode = index + 1
            }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    // 4. ดึงวิดีโอ (Load Links)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        // หา iframe ตัวเล่นวิดีโอในหน้าเล่นตอน
        val iframeUrl = doc.selectFirst("iframe")?.attr("src") ?: return false

        // Cloudstream มีตัวถอดลิงก์ iframe ของเว็บสตรีมมิ่งยอดนิยมในตัว
        loadExtractor(fixUrl(iframeUrl), subtitleCallback, callback)
        return true
    }
}