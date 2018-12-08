package no.nav.helse

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.features.suitableCharset
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.receive
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.toInputStream
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

class JsonContentConverter : ContentConverter {
    private val log = LoggerFactory.getLogger("JsonContentConverter")

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null

        return JSONObject(channel.toInputStream().bufferedReader().readText())
    }

    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any? {
        val json = when (value) {
            is Map<*, *> -> JSONObject(value)
            is List<*> -> JSONArray(value)
            else -> JSONObject(value)
        }.toString()

        log.info("sending json, {} to {}", value, json)
        return TextContent(json, contentType.withCharset(context.call.suitableCharset()))
    }

}

suspend fun ApplicationCall.receiveJson(): JSONObject = receive()
