@file:JvmName("TestSocketUtils")
package io.github.ktakashi.oas.test

import java.net.InetAddress
import javax.net.ServerSocketFactory
import kotlin.random.Random

const val DEFAULT_MIN_PORT_RANGE = 10000
const val DEFAULT_MAX_PORT_RANGE = 65535

private val random = Random(System.nanoTime())

@JvmOverloads
fun findAvailableTcpPort(min: Int = DEFAULT_MIN_PORT_RANGE, max: Int = DEFAULT_MAX_PORT_RANGE): Int = findAvailablePort(::isTcpPortAvailable, min, max)

fun isTcpPortAvailable(port: Int) = try {
    ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost")).use {
        true
    }
} catch (e: Exception) {
    false
}

private fun findAvailablePort(isPortAvailable: (Int) -> Boolean, min: Int, max: Int): Int {
    val range = max - min
    tailrec fun find(counter: Int): Int {
        if (counter <= range) {
            val candidatePort = min + random.nextInt(range + 1)
            return if (isPortAvailable(candidatePort)) {
                candidatePort
            } else {
                find(counter + 1)
            }
        }
        throw IllegalStateException("Could not find an available port in the range [$min, $max] after $counter attempts")
    }
    assert(min > 0)
    assert(max <= DEFAULT_MAX_PORT_RANGE)
    assert(range > 0)
    return find(0)
}
