package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import javax.xml.ws.soap.SOAPFaultException

class BaseneErUtilgjengeligeException(cause: SOAPFaultException): RuntimeException(cause)
