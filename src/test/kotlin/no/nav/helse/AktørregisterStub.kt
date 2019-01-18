package no.nav.helse

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock

fun aktørregisterStub(ident: String): MappingBuilder {
    return WireMock.get(WireMock.urlPathEqualTo("/api/v1/identer"))
            .willReturn(WireMock.ok("""
{
  "${ident}": {
    "identer": [
      {
        "ident": "654321",
        "identgruppe": "AktoerId",
        "gjeldende": true
      },
      {
        "ident": "${ident}",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()))
}
