package com.example.data.local

import android.content.Context
import java.io.File

object DemoPdfGenerator {

    /**
     * Generates a valid PDF 1.4 file containing Persian university schedule text
     * and writes it to the specified file.
     */
    fun createSampleSchedulePdf(targetFile: File, term: String = "۱۴۰۴-۱۴۰۵-۱"): File {
        val scheduleContent = """
            دانشگاه صنعتی - دانشکده مهندسی کامپیوتر
            برنامه هفتگی دروس دوره کارشناسی ترم $term
            تاریخ تنظیم: ۱۴۰۴/۰۷/۰۱
            
            روز شنبه:
            ساعت 08:00 - 10:00 درس برنامه نویسی پیشرفته استاد دکتر حسینی کلاس 101 گروه 1
            ساعت 10:00 - 12:00 درس پایگاه داده استاد دکتر رضایی کلاس 204 گروه 1
            ساعت 13:30 - 15:30 درس هوش مصنوعی استاد دکتر احمدی کلاس سایت 2 گروه 1
            
            روز یکشنبه:
            ساعت 08:30 - 10:30 درس مدارهای منطقی استاد مهندس کاظمی کلاس 102 گروه 1
            ساعت 10:30 - 12:30 درس زبان تخصصی کامپیوتر استاد دکتر مرادی کلاس 205 گروه 2
            
            روز دوشنبه:
            ساعت 08:00 - 10:00 درس سیستم‌های عامل استاد دکتر طاهری کلاس 301 گروه 1
            ساعت 10:30 - 12:30 درس معماری کامپیوتر استاد دکتر کمالی کلاس 104 گروه 1
            ساعت 14:00 - 16:00 درس مهندسی نرم‌افزار استاد دکتر سلیمانی کلاس 201 گروه 2
            
            روز سه‌شنبه:
            ساعت 08:00 - 10:00 درس برنامه نویسی پیشرفته استاد دکتر حسینی کلاس 101 گروه 1
            ساعت 10:00 - 12:00 درس پایگاه داده استاد دکتر رضایی کلاس 204 گروه 1
            ساعت 13:00 - 15:00 درس آزمایشگاه پایگاه داده استاد مهندس صادقی کلاس سایت 1 گروه 1
            
            روز چهارشنبه:
            ساعت 08:30 - 10:30 درس طراحی الگوریتم استاد دکتر اکبری کلاس 105 گروه 1
            ساعت 10:30 - 12:30 درس شبکه‌های کامپیوتری استاد دکتر شریفی کلاس 106 گروه 1
            ساعت 14:00 - 16:00 درس ریاضی مهندسی استاد دکتر موسوی کلاس 302 گروه 1
        """.trimIndent()

        // Create standard PDF 1.4 binary structure
        targetFile.parentFile?.mkdirs()
        val pdfBytes = buildSimplePdfBytes(scheduleContent)
        targetFile.writeBytes(pdfBytes)
        return targetFile
    }

    /**
     * Builds a well-formed PDF 1.4 stream containing the provided text content.
     */
    private fun buildSimplePdfBytes(textContent: String): ByteArray {
        val streamContentBuilder = StringBuilder()
        streamContentBuilder.append("BT\n")
        streamContentBuilder.append("/F1 12 Tf\n")
        streamContentBuilder.append("20 750 Td\n")
        streamContentBuilder.append("16 TL\n")

        for (line in textContent.lines()) {
            val safe = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
            streamContentBuilder.append("($safe) Tj T*\n")
        }
        streamContentBuilder.append("ET\n")

        val streamBytes = streamContentBuilder.toString().toByteArray(Charsets.UTF_8)
        val streamLength = streamBytes.size

        val obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        val obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        val obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> >>\nendobj\n"
        val obj4Header = "4 0 obj\n<< /Length $streamLength >>\nstream\n"
        val obj4Footer = "\nendstream\nendobj\n"

        val header = "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n"

        val headerBytes = header.toByteArray(Charsets.ISO_8859_1)
        val obj1Bytes = obj1.toByteArray(Charsets.ISO_8859_1)
        val obj2Bytes = obj2.toByteArray(Charsets.ISO_8859_1)
        val obj3Bytes = obj3.toByteArray(Charsets.ISO_8859_1)
        val obj4HBytes = obj4Header.toByteArray(Charsets.ISO_8859_1)
        val obj4FBytes = obj4Footer.toByteArray(Charsets.ISO_8859_1)

        val o1 = headerBytes.size
        val o2 = o1 + obj1Bytes.size
        val o3 = o2 + obj2Bytes.size
        val o4 = o3 + obj3Bytes.size

        val xrefOffset = o4 + obj4HBytes.size + streamBytes.size + obj4FBytes.size

        val xref = "xref\n0 5\n0000000000 65535 f \n" +
                String.format("%010d 00000 n \n", o1) +
                String.format("%010d 00000 n \n", o2) +
                String.format("%010d 00000 n \n", o3) +
                String.format("%010d 00000 n \n", o4)

        val trailer = "trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n"
        val xrefBytes = xref.toByteArray(Charsets.ISO_8859_1)
        val trailerBytes = trailer.toByteArray(Charsets.ISO_8859_1)

        return headerBytes + obj1Bytes + obj2Bytes + obj3Bytes + obj4HBytes + streamBytes + obj4FBytes + xrefBytes + trailerBytes
    }
}
