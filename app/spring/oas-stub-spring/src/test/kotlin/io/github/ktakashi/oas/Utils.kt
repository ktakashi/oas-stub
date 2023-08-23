package io.github.ktakashi.oas

private class Dummy
fun readContent(path: String) = Dummy::class.java.getResourceAsStream(path)?.reader()?.buffered()?.use { it.readText() }
        ?: throw IllegalArgumentException("$path doesn't exist")

fun maybeContent(content: String?) = content?.let {
    if (content.isNotEmpty()) {
        fromClassPathOrContent(content)
    } else null
}

fun fromClassPathOrContent(content: String): ByteArray =
    if (content.startsWith("classpath:")) {
        readContent(content.substring("classpath:".length)).toByteArray()
    } else {
        content.toByteArray()
    }

