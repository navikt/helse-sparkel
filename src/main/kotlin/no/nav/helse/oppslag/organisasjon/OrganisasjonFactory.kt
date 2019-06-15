package no.nav.helse.oppslag.organisasjon

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import javax.xml.namespace.QName

object OrganisasjonFactory {

    private val ServiceClass = OrganisasjonV5::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/organisasjon/v5/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/organisasjon/v5/Binding"
    private val ServiceName = QName(Namespace, "Organisasjon_v5")
    private val EndpointName = QName(Namespace, "Organisasjon_v5Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
