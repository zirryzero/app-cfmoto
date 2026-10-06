package dev.zanderp.opencfmoto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemReportTest {
    @Test
    fun subjectUsesAdaptationNameWithoutDuplicatingBikeBrand() {
        val subject = ProblemReport.subject("800NK Advanced", "2.0.23-pre")

        assertTrue(subject.startsWith("800NK ADV Link"))
        assertTrue(subject.contains("CFMOTO 800NK Advanced"))
        assertFalse(subject.contains("CFMOTO CFMOTO"))
    }

    @Test
    fun emailBodyIncludesProblemDiagnosticsAndBoundedRecentLog() {
        val body = ProblemReport.body(
            problem = "Touch does not respond",
            model = "800NK Advanced",
            year = "2025",
            diagnostics = "app=2.0.23-pre",
            log = "old-marker" + "x".repeat(7_000) + "recent-marker",
        )

        assertTrue(body.contains("Touch does not respond"))
        assertTrue(body.contains("CFMOTO 800NK Advanced, 2025"))
        assertTrue(body.contains("app=2.0.23-pre"))
        assertTrue(body.contains("recent-marker"))
        assertFalse(body.contains("old-marker"))
    }
}
