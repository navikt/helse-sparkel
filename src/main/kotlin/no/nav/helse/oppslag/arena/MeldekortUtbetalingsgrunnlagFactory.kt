package no.nav.helse.oppslag.arena

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import javax.xml.namespace.QName

object MeldekortUtbetalingsgrunnlagFactory {

    private val ServiceClass = MeldekortUtbetalingsgrunnlagV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding"
    private val ServiceName = QName(Namespace, "MeldekortUtbetalingsgrunnlag_v1")
    private val EndpointName = QName(Namespace, "meldekortUtbetalingsgrunnlag_v1Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
