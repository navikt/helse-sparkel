package no.nav.helse.oppslag.infotrygd

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import javax.xml.namespace.QName

object InfotrygdSakFactory {

    private val ServiceClass = InfotrygdSakV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdSak/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/infotrygdSak/v1/Binding"
    private val ServiceName = QName(Namespace, "InfotrygdSak_v1")
    private val EndpointName = QName(Namespace, "infotrygdSak_v1Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
