package io.github.ktakashi.oas.server.config

import java.net.URI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OasStubStaticConfigParserTest {
    @Test
    fun `should parse static config`() {
        val definitions = OasStubStaticConfigParser.parse(URI.create("classpath:/static-config.yaml"))
        assertEquals(3, definitions.size)
        assertTrue(definitions.keys.contains("petstore-static"))
        assertNotNull(definitions["petstore-static"])
        assertNotNull(definitions["petstore-static"]?.configurations)
        assertNull(definitions["petstore-static"]?.configurations?.get("/v1/pets"))
    }
}