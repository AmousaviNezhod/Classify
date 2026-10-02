package com.example.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit

data class DiscoveredPdf(
    val url: String,
    val linkText: String,
    val surroundingContext: String,
    val suggestedFileName: String,
    val semesterOrTerm: String = "۱۴۰۴-۱۴۰۵-۱"
)

data class DownloadedPdfResult(
    val url: String,
    val fileName: String,
    val inputStream: InputStream,
    val contentLength: Long,
    val etag: String?,
    val lastModified: String?
)

class UniversityRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    companion object {
        private const val TAG = "UniversityRemoteDS"

        /**
         * Centralized default university schedule source URL.
         * Defined in ONE place only.
         */
        const val DEFAULT_SOURCE_URL = "https://www.sadjad.ac.ir/"
        private const val SADJAD_HOST = "sadjad.ac.ir"
        private const val CLASS_NEWS_TITLE = "برنامه کلاس"
    }

    /**
     * Downloads and parses the HTML page from the given URL and extracts all relevant PDF links.
     */
    suspend fun fetchAndDiscoverPdfLinks(pageUrl: String): Result<List<DiscoveredPdf>> {
        return runCatching {
            val requestedUrl = pageUrl.trim().ifEmpty { DEFAULT_SOURCE_URL }
            val isUniversitySource = isSadjadHost(requestedUrl)
            val rootUrl = if (isUniversitySource) DEFAULT_SOURCE_URL else requestedUrl
            val rootPage = fetchHtml(rootUrl)
            if (!isUniversitySource) {
                val directPdfs = parseHtmlForPdfLinks(rootPage.html, rootPage.url)
                if (directPdfs.isNotEmpty()) return@runCatching directPdfs
            }

            // The homepage lists the current weekly schedule news first; its title is stable
            // even though the URL slug changes from week to week.
            val newsPages = discoverClassNewsPageUrls(rootPage.html, rootPage.url)
            for (newsUrl in newsPages) {
                val newsPage = runCatching { fetchHtml(newsUrl) }.getOrNull() ?: continue
                val pdfs = parseHtmlForPdfLinks(newsPage.html, newsPage.url)
                if (pdfs.isNotEmpty()) return@runCatching pdfs
            }

            // If an old article URL was saved in settings, try it only after checking the homepage.
            if (isUniversitySource && requestedUrl != rootUrl) {
                val requestedPage = runCatching { fetchHtml(requestedUrl) }.getOrNull()
                requestedPage?.let { page ->
                    parseHtmlForPdfLinks(page.html, page.url).takeIf { it.isNotEmpty() }?.let { return@runCatching it }
                }
            }
            error("خبر «برنامه کلاس‌های هفته» یا فایل‌های PDF آن در صفحه اصلی دانشگاه پیدا نشد.")
        }
    }

    private data class HtmlPage(val url: String, val html: String)

    private fun fetchHtml(url: String): HtmlPage {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) UniSchedule/1.0")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("خطای ارتباط با سرور دانشگاه (کد خطا: ${response.code})")
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) error("صفحه دریافت شده از دانشگاه خالی است.")
            return HtmlPage(response.request.url.toString(), html)
        }
    }

    private fun isSadjadHost(url: String): Boolean = runCatching {
        URI(url).host.orEmpty().removePrefix("www.").equals(SADJAD_HOST, ignoreCase = true)
    }.getOrDefault(false)

    /** Find newest class-announcement links using their title, not the changing news slug. */
    internal fun discoverClassNewsPageUrls(html: String, baseUrl: String): List<String> {
        val doc = Jsoup.parse(html, baseUrl)
        return doc.select("a[href]").mapNotNull { anchor ->
            val title = normalizePersian(anchor.text())
            val href = anchor.attr("abs:href").ifBlank { anchor.attr("href") }
            val path = runCatching { URI(href).path.orEmpty() }.getOrDefault("")
            if (isSadjadHost(href) && isClassScheduleAnnouncement(title) && path.startsWith("/news/")) href else null
        }.distinct()
    }

    private fun isClassScheduleAnnouncement(title: String): Boolean {
        val normalized = normalizePersian(title)
        return Regex("^$CLASS_NEWS_TITLE(?:\\s*های)?\\s+هفته").containsMatchIn(normalized)
    }

    private fun normalizePersian(text: String): String = text
        .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
        .replace(Regex("\\s+"), " ").trim()

    /**
     * Parses HTML structure using Jsoup to discover all PDFs in a schedule page.
     */
    fun parseHtmlForPdfLinks(html: String, baseUrl: String): List<DiscoveredPdf> {
        val doc = Jsoup.parse(html, baseUrl)
        val pageHeading = normalizePersian(doc.select("h1").firstOrNull()?.text().orEmpty())
        val pageTitle = normalizePersian(doc.title() + " " + pageHeading)
        val isSchedulePage = isClassScheduleAnnouncement(pageTitle) || isClassScheduleAnnouncement(pageHeading)
        val semester = extractSemester(pageTitle + " " + doc.body()?.text().orEmpty())
        val discoveredList = mutableListOf<DiscoveredPdf>()
        val seenUrls = mutableSetOf<String>()

        // 1. Scan all <a> elements
        val links = doc.select("a[href]")
        for (link in links) {
            val href = link.attr("abs:href").trim().ifEmpty { link.attr("href").trim() }
            val linkText = link.text().trim()
            val parentText = link.parent()?.takeUnless { it.tagName() == "body" || it.tagName() == "html" }
                ?.text()?.take(150)?.trim().orEmpty()

            val isPdfUrl = href.endsWith(".pdf", ignoreCase = true) || href.contains(".pdf?", ignoreCase = true)
            val scheduleKeywords = listOf(
                "برنامه", "دروس", "ترم", "کلاس", "هفتگی", "آزمایشگاه", "کارگاه", "سایت کامپیوتری",
                "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه",
                "schedule", "timetable", "course"
            )
            val hasScheduleKeyword = scheduleKeywords.any {
                linkText.contains(it, ignoreCase = true) || parentText.contains(it, ignoreCase = true) || href.contains(it, ignoreCase = true)
            }
            val isScheduleAttachment = isPdfUrl && hasScheduleKeyword

            if (isScheduleAttachment || (hasScheduleKeyword && (href.contains("download", ignoreCase = true) || href.contains("file", ignoreCase = true)))) {
                val absoluteUrl = resolveAbsoluteUrl(baseUrl, href)
                if (absoluteUrl.isNotBlank() && seenUrls.add(absoluteUrl)) {
                    val fileName = extractFileNameFromUrlOrText(absoluteUrl, linkText)
                    discoveredList.add(
                        DiscoveredPdf(
                            url = absoluteUrl,
                            linkText = linkText,
                            surroundingContext = parentText,
                            suggestedFileName = fileName,
                            semesterOrTerm = extractSemester(parentText + " " + linkText).takeIf { it != "۱۴۰۴-۱۴۰۵-۱" } ?: semester
                        )
                    )
                }
            }
        }

        // 2. Also scan iframe, embed, or object tags if university embeds PDF
        val embeds = if (isSchedulePage) doc.select("iframe[src], embed[src], object[data]") else emptyList()
        for (embed in embeds) {
            val src = embed.attr("abs:src").ifEmpty { embed.attr("abs:data") }
            if (src.endsWith(".pdf", ignoreCase = true) && seenUrls.add(src)) {
                discoveredList.add(
                    DiscoveredPdf(
                        url = src,
                        linkText = "برنامه کلاسی ضمیمه شده",
                        surroundingContext = "فایل جاسازی شده در صفحه",
                        suggestedFileName = extractFileNameFromUrlOrText(src, "embedded_schedule"),
                        semesterOrTerm = semester
                    )
                )
            }
        }

        return discoveredList
    }

    /**
     * Downloads a PDF file over HTTPS, checking ETag / Last-Modified.
     */
    suspend fun downloadPdf(
        pdfUrl: String,
        cachedEtag: String? = null,
        cachedLastModified: String? = null
    ): Result<DownloadedPdfResult?> {
        return runCatching {
            val reqBuilder = Request.Builder()
                .url(pdfUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) UniSchedule/1.0")

            if (!cachedEtag.isNullOrBlank()) {
                reqBuilder.header("If-None-Match", cachedEtag)
            }
            if (!cachedLastModified.isNullOrBlank()) {
                reqBuilder.header("If-Modified-Since", cachedLastModified)
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.code == 304) {
                // Not Modified - cached version is up to date!
                Log.d(TAG, "PDF not modified (HTTP 304): $pdfUrl")
                return@runCatching null
            }

            if (!response.isSuccessful) {
                error("خطای دانلود فایل PDF (کد: ${response.code})")
            }

            val body = response.body ?: error("فایل دانلودی خالی است.")
            val etag = response.header("ETag")
            val lastModified = response.header("Last-Modified")
            val contentLength = body.contentLength()

            val fileName = response.header("Content-Disposition")?.let { extractFileNameFromHeader(it) }
                ?: extractFileNameFromUrlOrText(pdfUrl, "")

            DownloadedPdfResult(
                url = pdfUrl,
                fileName = fileName,
                inputStream = body.byteStream(),
                contentLength = contentLength,
                etag = etag,
                lastModified = lastModified
            )
        }
    }

    private fun resolveAbsoluteUrl(baseUrl: String, href: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(href).toString()
        } catch (e: Exception) {
            href
        }
    }

    private fun extractFileNameFromUrlOrText(url: String, linkText: String): String {
        val cleanUrl = url.substringBefore("?").substringBefore("#")
        val urlFileName = cleanUrl.substringAfterLast("/")
        if (urlFileName.endsWith(".pdf", ignoreCase = true) && urlFileName.length > 4) {
            return urlFileName
        }
        if (linkText.isNotBlank()) {
            val safe = linkText.replace(Regex("[^a-zA-Z0-9۰-۹\\u0600-\\u06FF_-]"), "_").take(30)
            return "$safe.pdf"
        }
        return "schedule_${System.currentTimeMillis()}.pdf"
    }

    private fun extractFileNameFromHeader(header: String): String? {
        val match = Regex("filename\\*?=['\"]?(?:UTF-8'')?([^;'\"]+)['\"]?", RegexOption.IGNORE_CASE).find(header)
        return match?.groupValues?.getOrNull(1)
    }

    private fun extractSemester(text: String): String {
        val normalizedText = normalizePersian(text)
        val digits = normalizedText.map { ch -> when (ch) {
            in '۰'..'۹' -> ('0'.code + ch.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + ch.code - '٠'.code).toChar()
            else -> ch
        } }.joinToString("")
        val years = Regex("(140[0-9])[-–_]?(140[0-9])").find(digits) ?: return "۱۴۰۴-۱۴۰۵-۱"
        val term = when {
            Regex("نیم\\s*سال\\s*اول").containsMatchIn(normalizedText) -> "۱"
            Regex("نیم\\s*سال\\s*دوم").containsMatchIn(normalizedText) -> "۲"
            else -> "۱"
        }
        val from = years.groupValues[1].map { ('۰'.code + it.code - '0'.code).toChar() }.joinToString("")
        val to = years.groupValues[2].map { ('۰'.code + it.code - '0'.code).toChar() }.joinToString("")
        return "$from-$to-$term"
    }
}
