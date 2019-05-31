package no.nav.helse.domene.ytelse.infotrygd

import no.nav.helse.Feilårsak.*
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdBeregningsgrunnlagErrorMapper.mapToError
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.BaseneErUtilgjengeligeException
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.xml.namespace.QName
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPFactory
import javax.xml.ws.soap.SOAPFaultException

class InfotrygdBeregningsgrunnlagErrorMapperTest {

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
    fun `skal mappe BaseneErUtilgjengeligeException til TjenesteErUtilgjengelig`() {
        val fault = SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createFault("Basene i Infotrygd er ikke tilgjengelige", QName("nameSpaceURI", "ERROR"))
        assertEquals(TjenesteErUtilgjengelig, mapToError(BaseneErUtilgjengeligeException(SOAPFaultException(fault))))
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
