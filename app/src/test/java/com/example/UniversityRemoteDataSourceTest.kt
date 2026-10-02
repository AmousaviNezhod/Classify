package com.example

import com.example.data.remote.UniversityRemoteDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversityRemoteDataSourceTest {
    private val remote = UniversityRemoteDataSource()
    private val home = "https://www.sadjad.ac.ir/"

    @Test
    fun `finds class schedule news from title not changing url`() {
        val html = """
            <html><body>
              <a href="/news/week-2-shuffled-this-term">برنامه کلاس‌های هفته دوم آموزشی نیم‌سال اول ۱۴۰۶-۱۴۰۵</a>
              <a href="/news/week-1">برنامه کلاس‌های هفته اول آموزشی نیم‌سال دوم ۱۴۰۵-۱۴۰۴</a>
              <a href="/news/other">برنامه کلاسی آزمایشگاه‌ها و کارگاه‌ها</a>
              <a href="/news/exams">برنامه امتحانات هفته دوم</a>
            </body></html>
        """.trimIndent()

        assertEquals(
            listOf(
                "https://www.sadjad.ac.ir/news/week-2-shuffled-this-term",
                "https://www.sadjad.ac.ir/news/week-1"
            ),
            remote.discoverClassNewsPageUrls(html, home)
        )
    }

    @Test
    fun `finds pdf attachments from class schedule news and infers current term`() {
        val html = """
            <html><head><title>برنامه کلاس‌های هفته اول آموزشی نیم‌سال دوم ۱۴۰۵-۱۴۰۴ | دانشگاه سجاد</title></head>
            <body><h1>برنامه کلاس‌های هفته اول آموزشی نیم‌سال دوم ۱۴۰۵-۱۴۰۴</h1>
              <a href="/media/schedule-1.pdf">برنامه روز شنبه</a>
              <a href="/media/labs.pdf">آزمایشگاه‌ها و کارگاه‌ها</a>
              <a href="/media/other.pdf">عکس مراسم دانشگاه</a>
            </body></html>
        """.trimIndent()

        val discovered = remote.parseHtmlForPdfLinks(html, "https://www.sadjad.ac.ir/news/slug-changes")
        assertEquals(2, discovered.size)
        assertTrue("Unexpected inferred term values: ${discovered.map { it.semesterOrTerm }}", discovered.all { it.semesterOrTerm == "۱۴۰۵-۱۴۰۴-۲" })
        assertTrue(discovered.any { it.url.endsWith("schedule-1.pdf") })
        assertTrue(discovered.any { it.url.endsWith("labs.pdf") })
    }

    @Test
    fun `starts at homepage and fetches current week pdf despite stale saved article url`() = runBlocking {
        val currentNewsUrl = "https://www.sadjad.ac.ir/news/new-current-week-slug"
        val pdfUrl = "https://www.sadjad.ac.ir/sites/default/files/saturday.pdf"
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requests += request.url.toString()
                val body = when (request.url.encodedPath) {
                    "/" -> """
                        <html><body>
                          <a href="/news/new-current-week-slug">برنامه کلاس‌های هفته سوم آموزشی نیم‌سال اول ۱۴۰۶-۱۴۰۵</a>
                          <a href="/news/unrelated">خبر دیگر دانشگاه</a>
                        </body></html>
                    """.trimIndent()
                    "/news/new-current-week-slug" -> """
                        <html><head><title>برنامه کلاس‌های هفته سوم آموزشی نیم‌سال اول ۱۴۰۶-۱۴۰۵</title></head>
                        <body><h1>برنامه کلاس‌های هفته سوم آموزشی نیم‌سال اول ۱۴۰۶-۱۴۰۵</h1>
                          <article><p>مکان تشکیل کلاس‌ها:</p><p><a href="/sites/default/files/saturday.pdf">برنامه روز شنبه</a></p></article>
                        </body></html>
                    """.trimIndent()
                    else -> error("Unexpected request: ${request.url}")
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("text/html; charset=utf-8".toMediaType()))
                    .build()
            }
            .build()
        val source = UniversityRemoteDataSource(client)

        val result = source.fetchAndDiscoverPdfLinks(
            "https://www.sadjad.ac.ir/news/stale-previous-week-slug"
        )

        assertTrue("Discovery failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals("Unexpected HTTP request sequence", listOf(home, currentNewsUrl), requests)
        assertEquals("Unexpected discovered PDF URLs", listOf(pdfUrl), result.getOrThrow().map { it.url })
        assertEquals("Unexpected inferred semester", "۱۴۰۶-۱۴۰۵-۱", result.getOrThrow().single().semesterOrTerm)
    }

    @Test
    fun `does not treat unrelated homepage news as schedule announcements`() {
        val html = """
            <html><body>
              <a href="/news/labs-week">برنامه کلاسی آزمایشگاه‌ها و کارگاه‌ها</a>
              <a href="/news/exams-week">برنامه امتحانات هفته دوم</a>
              <a href="https://example.com/news/schedule">برنامه کلاس‌های هفته دوم</a>
            </body></html>
        """.trimIndent()

        assertTrue(remote.discoverClassNewsPageUrls(html, home).isEmpty())
    }
}
