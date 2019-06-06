package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.namespace.QName
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPFactory
import javax.xml.ws.soap.SOAPFaultException

class InfotrygdBeregningsgrunnlagClientTest {

    @Test
    fun `må håndtere at response kan være null`() {
        val infotrygdBeregningsgrunnlagV1 = mockk< InfotrygdBeregningsgrunnlagV1>()
        val client = InfotrygdBeregningsgrunnlagClient(infotrygdBeregningsgrunnlagV1)

        every {
            infotrygdBeregningsgrunnlagV1.finnGrunnlagListe(any())
        } answers { nothing }

        val actual = client.finnGrunnlagListe("11111111111", LocalDate.now(), LocalDate.now())

        actual as Try.Success

        assertNotNull(actual.value)
    }

    @Test
    fun `skal håndtere at infotrygd er utilgjengelig`() {
        val infotrygdBeregningsgrunnlagV1 = mockk< InfotrygdBeregningsgrunnlagV1>()
        val client = InfotrygdBeregningsgrunnlagClient(infotrygdBeregningsgrunnlagV1)

        every {
            infotrygdBeregningsgrunnlagV1.finnGrunnlagListe(any())
        } throws SOAPFaultException(SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL)
                .createFault("Basene i Infotrygd er ikke tilgjengelige....", QName("nameSpaceURI", "ERROR")))

        val actual = client.finnGrunnlagListe("11111111111", LocalDate.now(), LocalDate.now())

        actual as Try.Failure

        assertTrue(actual.exception is BaseneErUtilgjengeligeException)
    }
}
