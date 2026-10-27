package com.jantursky.stipple.components

import com.jantursky.stipple.docs.DocsSnippets
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Kotlin fences of the docs site are generated into this test source set by `generateDocsSnippets`, so reaching
 * this test means every one of them compiled. What is left to check is that the generator saw every fence.
 */
class DocsSnippetsCompileTest {
    private val docs = File("../apps/docs/docs")
    private val fence = Regex("^```kotlin", RegexOption.MULTILINE)

    @Test
    fun everyKotlinFenceIsCompiledOrMarkedNocompile() {
        val fences = docs.walkTopDown().filter { it.extension == "mdx" }.sumOf { fence.findAll(it.readText()).count() }
        assertTrue(fences > 0, "no Kotlin fences found under ${docs.absolutePath}")
        assertEquals(fences, DocsSnippets.COMPILED + DocsSnippets.SKIPPED)
    }

    @Test
    fun skippedFencesStayRare() {
        // A fence is skipped only when it cannot be code: Gradle configuration or a signature without a body.
        println("docs snippets: ${DocsSnippets.COMPILED} compiled, ${DocsSnippets.SKIPPED} skipped")
        assertTrue(DocsSnippets.SKIPPED <= DocsSnippets.COMPILED)
    }
}
