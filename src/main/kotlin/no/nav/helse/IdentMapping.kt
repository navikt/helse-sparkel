package no.nav.helse

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import java.util.*


private val log = LoggerFactory.getLogger("IdentMapping")

fun Route.identMapping(identLookupFactory: () -> IdentLookup) {
    val mapper by lazy(identLookupFactory)

    get("api/ident") {
        call.parameters["fnr"]?.let {
            log.info("mapping from fnr=${it}")
            call.respond(mapOf(
                    "id" to mapper.fromIdent(Ident(it, IdentType.NorskIdent))
            ))
        } ?: call.parameters["aktorId"]?.let {
            log.info("mapping from aktørId=${it}")
            call.respond(mapOf(
                    "id" to mapper.fromIdent(Ident(it, IdentType.AktoerId))
            ))
        } ?: call.parameters["uuid"]?.let {
            log.info("mapping from uuid=${it}")
            try {
                call.respond(mapper.fromUUID(it))
            } catch (e: java.lang.IllegalArgumentException) {
                log.info("did not find ident with uuid=${it}")
                call.respond(HttpStatusCode.NotFound)
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
}

interface IdentCache {
    fun fromIdent(ident: Ident): String?

    fun setIdenter(identer: List<Ident>): String

    fun fromUUID(uuid: String): List<Ident>
}

class RedisIdentCache(private val client: Jedis): IdentCache {
    override fun fromIdent(ident: Ident): String? {
        log.info("lookup ident=${ident} in redis")
        return client.get("${ident.type.name}_${ident.ident}")?.let {
            log.info("got ${it} from redis")
            it
        } ?: run {
            log.info("did not find ident=${ident} in redis")
            null
        }
    }

    override fun setIdenter(identer: List<Ident>): String {
        val uuid = UUID.randomUUID()
        log.info("setting identer in redis with uuid=${uuid}")

        client.set("ident_${uuid}", JSONArray(identer).toString())
        identer.forEach {
            client.set("${it.type.name}_${it.ident}", uuid.toString())
        }

        return uuid.toString()
    }

    override fun fromUUID(uuid: String): List<Ident> {
        log.info("lookup uuid=${uuid} in redis")
        return client.get("ident_${uuid}")?.let {
            log.info("got ${it} from redis")
            JSONArray(it)
        }?.map {
            it as JSONObject
        }?.map {
            Ident(it.getString("ident"), it.getEnum(IdentType::class.java, "type"))
        } ?: throw IllegalArgumentException("ident not recognized")
    }
}

class IdentLookup(private val aktørregisterClient: AktørregisterClient, private val cache: IdentCache) {

    fun fromIdent(ident: Ident): String {
        log.info("lookup ident=${ident}")
        return cache.fromIdent(ident) ?: cache.setIdenter(aktørregisterClient.gjeldendeIdenter(ident.ident))
    }

    fun fromUUID(uuid: String): List<Ident> {
        log.info("lookup uuid=${uuid}")
        return cache.fromUUID(uuid)
    }
}
