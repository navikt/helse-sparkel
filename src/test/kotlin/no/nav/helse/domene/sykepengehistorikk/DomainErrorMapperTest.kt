package no.nav.helse.domene.sykepengehistorikk

import no.nav.helse.Feilårsak.*
import no.nav.helse.domene.sykepengehistorikk.DomainErrorMapper.mapToError
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.xml.namespace.QName
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPFactory
import javax.xml.ws.soap.SOAPFaultException

class DomainErrorMapperTest {

    @Test
    fun `skal mappe FinnGrunnlagListeSikkerhetsbegrensning til FeilFraTjeneste`() {
        assertEquals(FeilFraTjeneste, mapToError(FinnGrunnlagListeSikkerhetsbegrensning(null, null)))
    }

    @Test
    fun `skal mappe FinnGrunnlagListeUgyldigInput til FeilFraTjeneste`() {
        assertEquals(FeilFraTjeneste, mapToError(FinnGrunnlagListeUgyldigInput(null, null)))
    }

    @Test
    fun `skal mappe FinnGrunnlagListePersonIkkeFunnet til IkkeFunnet`() {
        assertEquals(IkkeFunnet, mapToError(FinnGrunnlagListePersonIkkeFunnet(null, null)))
    }

    @Test
    fun `skal mappe feilmelding til TjenesteErUtilgjengelig når Infotrygd er utilgjengelig`() {
        val fault = SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createFault("Basene i Infotrygd er ikke tilgjengelige", QName("nameSpaceURI", "ERROR"))
        assertEquals(TjenesteErUtilgjengelig, mapToError(SOAPFaultException(fault)))
    }

    @Test
    fun `skal mappe annen feilmelding til UkjentFeil`() {
        val fault = SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createFault("Server Error", QName("nameSpaceURI", "ERROR"))
        assertEquals(UkjentFeil, mapToError(SOAPFaultException(fault)))
    }

    @Test
    fun `skal mappe exception til UkjentFeil`() {
        assertEquals(UkjentFeil, mapToError(RuntimeException("uknown error")))
    }
}
