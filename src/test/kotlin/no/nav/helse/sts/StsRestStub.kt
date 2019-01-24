package no.nav.helse.sts

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock

fun stsRestStub(): MappingBuilder {
    return WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token"))
            .willReturn(WireMock.okJson(ok_sts_response))
}

private val ok_sts_response = """{
    "access_token": "default access token",
    "token_type": "Bearer",
    "expires_in": 3600
}""".trimIndent()
