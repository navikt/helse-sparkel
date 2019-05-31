package no.nav.helse.domene.ytelse.sykepengehistorikk

import arrow.core.right
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.JwtStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.namespace.QName
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPFactory
import javax.xml.ws.soap.SOAPFaultException

class SykepengehistorikkComponentTest {
    @Test
    fun `skal returnere 503 når infotrygd er utilgjengelig`() {
        val infotrygdBeregningsgrunnlagV1 = mockk<InfotrygdBeregningsgrunnlagV1>()
        val aktørregisterService = mockk<AktørregisterService>()

        val fnr = Fødselsnummer("11111111111")
        val aktørId = AktørId("123456789")
        val fom = LocalDate.of(2019, 5, 1)
        val tom = LocalDate.of(2019, 5, 31)

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fnr.value.right()

        every {
            infotrygdBeregningsgrunnlagV1.finnGrunnlagListe(match { request ->
                request.personident == fnr.value
                        && request.fom.toLocalDate() == fom
                        && request.tom.toLocalDate() == tom
            })
        } throws SOAPFaultException(SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createFault("Basene i Infotrygd er ikke tilgjengelige", QName("nameSpaceURI", "ERROR")))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                sykepengehistorikkService = SykepengehistorikkService(
                        aktørregisterService = aktørregisterService,
                        infotrygdService = InfotrygdService(
                                infotrygdBeregningsgrunnlagClient = InfotrygdBeregningsgrunnlagClient(infotrygdBeregningsgrunnlagV1),
                                infotrygdSakClient = mockk(),
                                probe = mockk(relaxed = true)
                        )
                )
        )}) {
            handleRequest(HttpMethod.Get, "/api/sykepengehistorikk/${aktørId.aktor}?fom=$fom&tom=$tom") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                assertJsonEquals(JSONObject(expectedJson_service_unavailable), JSONObject(response.content))
            }
        }
    }
}

private val expectedJson_service_unavailable = """
{
  "feilmelding": "Service is unavailable"
}
""".trimIndent()
