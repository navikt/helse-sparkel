package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import no.nav.helse.oppslag.WsClientFactory
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import javax.xml.namespace.QName

object InfotrygdBeregningsgrunnlagFactory {

    private val ServiceClass = InfotrygdBeregningsgrunnlagV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding"
    private val ServiceName = QName(Namespace, "infotrygdBeregningsgrunnlag_v1")
    private val EndpointName = QName(Namespace, "infotrygdBeregningsgrunnlag_v1Port")

    fun create(endpointUrl: String, wsClientFactory: WsClientFactory) =
            wsClientFactory.create(ServiceClass, endpointUrl, Wsdl, ServiceName, EndpointName)
}
