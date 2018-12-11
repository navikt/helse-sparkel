package no.nav.helse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import java.util.*

class RedisIdentCacheTest {

    @Test
    fun `fromIdent should return null when cache is empty`() {
        val jedisMock = mockk<Jedis>()
        every {
            jedisMock.get("NorskIdent_123456")
        } returns null

        val redisCache = RedisIdentCache(jedisMock)

        Assertions.assertNull(redisCache.fromIdent(Ident("123456", IdentType.NorskIdent)))

        verify {
            jedisMock.get("NorskIdent_123456")
        }
    }

    @Test
    fun `fromIdent should return value of cache when set`() {
        val jedisMock = mockk<Jedis>()
        every {
            jedisMock.get("NorskIdent_123456")
        } returns "a uuid"

        val redisCache = RedisIdentCache(jedisMock)

        Assertions.assertEquals("a uuid", redisCache.fromIdent(Ident("123456", IdentType.NorskIdent)))

        verify {
            jedisMock.get("NorskIdent_123456")
        }
    }

    @Test
    fun `setIdenter should set entry for each ident and one for uuid`() {
        val jedisMock = mockk<Jedis>(relaxed = true)

        val redisCache = RedisIdentCache(jedisMock)

        val identer = listOf(
                Ident("123456", IdentType.NorskIdent),
                Ident("654321", IdentType.AktoerId)
        )
        val result = redisCache.setIdenter(identer)

        verify {
            jedisMock.set("ident_${result}", JSONArray(identer).toString())
            jedisMock.set("NorskIdent_123456", result)
            jedisMock.set("AktoerId_654321", result)
        }
    }

    @Test
    fun `fromUUID should throw exception on unknown uuid`() {
        val anUUID = UUID.randomUUID().toString()

        val jedisMock = mockk<Jedis>()
        every {
            jedisMock.get("ident_${anUUID}")
        } returns null

        val redisCache = RedisIdentCache(jedisMock)

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            redisCache.fromUUID(anUUID)
        }

        verify {
            jedisMock.get("ident_${anUUID}")
        }
    }

    @Test
    fun `fromUUID should throw exception if json is malformed`() {
        val anUUID = UUID.randomUUID().toString()

        val jedisMock = mockk<Jedis>()
        every {
            jedisMock.get("ident_${anUUID}")
        } returns "this is not valid json"

        val redisCache = RedisIdentCache(jedisMock)

        Assertions.assertThrows(JSONException::class.java) {
            redisCache.fromUUID(anUUID)
        }

        verify {
            jedisMock.get("ident_${anUUID}")
        }
    }

    @Test
    fun `fromUUID should return result of cache when set`() {
        val anUUID = UUID.randomUUID().toString()

        val identer = listOf(
                Ident("123456", IdentType.NorskIdent),
                Ident("654321", IdentType.AktoerId)
        )

        val jedisMock = mockk<Jedis>()
        every {
            jedisMock.get("ident_${anUUID}")
        } returns JSONArray(identer).toString()

        val redisCache = RedisIdentCache(jedisMock)

        val result = redisCache.fromUUID(anUUID)
        Assertions.assertEquals(identer, result)

        verify {
            jedisMock.get("ident_${anUUID}")
        }
    }
}
