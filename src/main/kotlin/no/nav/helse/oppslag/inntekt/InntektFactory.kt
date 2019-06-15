package no.nav.helse.oppslag.inntekt

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import javax.xml.namespace.QName

object InntektFactory {

    private val ServiceClass = InntektV3::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/inntekt/v3/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/inntekt/v3/Binding"
    private val ServiceName = QName(Namespace, "Inntekt_v3")
    private val EndpointName = QName(Namespace, "Inntekt_v3Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
