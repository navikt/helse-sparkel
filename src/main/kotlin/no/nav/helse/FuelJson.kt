package no.nav.helse

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.response
import org.json.JSONObject

class JsonDeserializer : Deserializable<JSONObject> {
    override fun deserialize(response: Response): JSONObject {
        return JSONObject(response.dataStream.bufferedReader(Charsets.UTF_8).readText())
    }
}

fun Request.responseJSON() = response(JsonDeserializer())
