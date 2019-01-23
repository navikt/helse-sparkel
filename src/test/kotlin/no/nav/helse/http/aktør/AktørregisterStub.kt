package no.nav.helse.http.aktør

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock

fun aktørregisterStub(
        ident: String,
        aktoerId : String = ident,
        fnr :  String = ident): MappingBuilder {
    return WireMock.get(WireMock.urlPathEqualTo("/api/v1/identer"))
            .willReturn(WireMock.ok("""
{
  "$ident": {
    "identer": [
      {
        "ident": "$aktoerId",
        "identgruppe": "AktoerId",
        "gjeldende": true
      },
      {
        "ident": "$fnr",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()))
}
