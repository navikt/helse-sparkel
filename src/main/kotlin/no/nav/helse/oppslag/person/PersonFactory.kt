package no.nav.helse.oppslag.person

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import javax.xml.namespace.QName

object PersonFactory {

    private val ServiceClass = PersonV3::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding"
    private val ServiceName = QName(Namespace, "Person_v3")
    private val EndpointName = QName(Namespace, "Person_v3Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
