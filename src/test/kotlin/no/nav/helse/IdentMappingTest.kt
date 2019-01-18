package no.nav.helse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.fail

class IdentMappingTest {

    @Test
    fun `should throw exception if not found`() {
        val mock = mockk<AktørregisterClient>()

        val cache = IdentLookup({mock}, InMemoryCache())

        try {
            cache.fromUUID("foobar")
            fail("Expected fromIdent to throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun `should insert identer when empty`() {
        val expectedList = listOf(Ident("12345", IdentType.NorskIdent), Ident("54321", IdentType.AktoerId))

        val mock = mockk<AktørregisterClient>()
        every {
            mock.gjeldendeIdenter("12345")
        } returns expectedList

        val cache = IdentLookup({mock}, InMemoryCache())

        val ident = Ident("12345", IdentType.NorskIdent)

        cache.fromIdent(ident)

        verify {
            mock.gjeldendeIdenter("12345")
        }
    }

    @Test
    fun `should return list of identer`() {
        val expectedList = listOf(Ident("12345", IdentType.NorskIdent), Ident("54321", IdentType.AktoerId))

        val mock = mockk<AktørregisterClient>()
        every {
            mock.gjeldendeIdenter("12345")
        } returns expectedList

        val cache = IdentLookup({mock}, InMemoryCache())

        val ident = Ident("12345", IdentType.NorskIdent)

        Assertions.assertEquals(expectedList, cache.fromUUID(cache.fromIdent(ident)))
    }

    @Test
    fun `should return same uuid for both type of identer`() {
        val expectedList = listOf(Ident("12345", IdentType.NorskIdent), Ident("54321", IdentType.AktoerId))

        val mock = mockk<AktørregisterClient>()
        every {
            mock.gjeldendeIdenter("12345")
        } returns expectedList

        val cache = IdentLookup({mock}, InMemoryCache())

        val norskIdent = Ident("12345", IdentType.NorskIdent)
        Assertions.assertEquals(cache.fromIdent(norskIdent), cache.fromIdent(norskIdent))

        val aktørIdent = Ident("54321", IdentType.AktoerId)
        Assertions.assertEquals(cache.fromIdent(norskIdent), cache.fromIdent(aktørIdent))
    }
}

class InMemoryCache: IdentCache {
    private val identMap: MutableMap<UUID, List<Ident>> = mutableMapOf()

    override fun fromIdent(ident: Ident): String? {
        return identMap.flatMap {
            val key = it.key
            it.value.map {
                Pair(key, it)
            }
        }.firstOrNull {
            it.second.type == ident.type && it.second.ident == ident.ident
        }?.first?.toString()
    }

    override fun fromUUID(uuid: String): List<Ident> {
        return identMap.getOrElse(UUID.fromString(uuid)) {
            throw IllegalArgumentException("ident not recognized")
        }
    }

    override fun setIdenter(identer: List<Ident>): String {
        val uuid = UUID.randomUUID()
        identMap[uuid] = identer
        return uuid.toString()
    }
}

