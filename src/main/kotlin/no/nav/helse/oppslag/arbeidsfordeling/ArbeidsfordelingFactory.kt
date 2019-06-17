package no.nav.helse.oppslag.arbeidsfordeling

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import org.apache.cxf.feature.Feature
import org.apache.cxf.interceptor.Interceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import javax.xml.namespace.QName

object ArbeidsfordelingFactory {

    private val ServiceClass = ArbeidsfordelingV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsfordeling/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Binding"
    private val ServiceName = QName(Namespace, "Arbeidsfordeling_v1")
    private val EndpointName = QName(Namespace, "Arbeidsfordeling_v1Port")

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
