package io.github.ktakashi.oas.engine

private class Dummy
fun readStringContent(path: String) = Dummy::class.java.getResourceAsStream(path)?.bufferedReader().use {
    it?.readText()
} ?: throw IllegalArgumentException("$path doesn't exist")

fun readBinaryContent(path: String) = Dummy::class.java.getResourceAsStream(path)?.use {
    it.readAllBytes()
} ?: throw IllegalArgumentException("$path doesn't exist")

