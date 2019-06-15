package no.nav.helse.oppslag.arbeidsfordeling

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import javax.xml.namespace.QName

object ArbeidsfordelingFactory {

    private val ServiceClass = ArbeidsfordelingV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsfordeling/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Binding"
    private val ServiceName = QName(Namespace, "Arbeidsfordeling_v1")
    private val EndpointName = QName(Namespace, "Arbeidsfordeling_v1Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
