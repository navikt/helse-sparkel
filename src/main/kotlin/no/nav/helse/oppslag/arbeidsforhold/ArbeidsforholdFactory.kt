package no.nav.helse.oppslag.arbeidsforhold

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import javax.xml.namespace.QName

object ArbeidsforholdFactory {

    private val ServiceClass = ArbeidsforholdV3::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsforhold/v3/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding"
    private val ServiceName = QName(Namespace, "Arbeidsforhold_v3")
    private val EndpointName = QName(Namespace, "Arbeidsforhold_v3Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
