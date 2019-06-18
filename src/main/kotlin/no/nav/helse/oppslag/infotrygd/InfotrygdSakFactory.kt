package no.nav.helse.oppslag.infotrygd

import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import org.apache.cxf.feature.Feature
import org.apache.cxf.interceptor.Interceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import javax.xml.namespace.QName

object InfotrygdSakFactory {

    private val ServiceClass = InfotrygdSakV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdSak/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/infotrygdSak/v1/Binding"
    private val ServiceName = QName(Namespace, "InfotrygdSak_v1")
    private val EndpointName = QName(Namespace, "infotrygdSak_v1Port")

    fun create(endpointUrl: String, features: List<Feature> = emptyList(), outInterceptors: List<Interceptor<Message>> = emptyList()) =
            JaxWsProxyFactoryBean().apply {
                address = endpointUrl
                wsdlURL = Wsdl
                serviceName = ServiceName
                endpointName = EndpointName
                serviceClass = ServiceClass
                this.features.addAll(features)
                this.outInterceptors.addAll(outInterceptors)
            }.create(ServiceClass)
}
