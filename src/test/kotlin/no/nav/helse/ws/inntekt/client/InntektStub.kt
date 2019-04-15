package no.nav.helse.ws.inntekt.client

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import no.nav.helse.ws.withSoapAction

fun hentInntektListeBolkStub(ident: String, månedFom: String, månedTom: String, filter: String, formål: String): MappingBuilder {
    return WireMock.post("/inntekt")
            .withSoapAction("http://nav.no/tjeneste/virksomhet/inntekt/v3/Inntekt_v3/hentInntektListeBolkRequest")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/v3:hentInntektListeBolk/request/identListe/aktoerId/text()",
                    inntektNamespace, WireMock.equalTo(ident)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/v3:hentInntektListeBolk/request/ainntektsfilter/text()",
                    inntektNamespace, WireMock.equalTo(filter)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/v3:hentInntektListeBolk/request/formaal/text()",
                    inntektNamespace, WireMock.equalTo(formål)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/v3:hentInntektListeBolk/request/uttrekksperiode/maanedFom/text()",
                    inntektNamespace, WireMock.equalTo(månedFom)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/v3:hentInntektListeBolk/request/uttrekksperiode/maanedTom/text()",
                    inntektNamespace, WireMock.equalTo(månedTom)))
}

private val inntektNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "v3" to "http://nav.no/tjeneste/virksomhet/inntekt/v3",
        "ns2" to "http://nav.no/tjeneste/virksomhet/inntekt/v3/informasjon/inntekt"
)
