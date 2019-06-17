package no.nav.helse.oppslag.person

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.apache.cxf.feature.Feature
import org.apache.cxf.interceptor.Interceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import javax.xml.namespace.QName

object PersonFactory {

    private val ServiceClass = PersonV3::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding"
    private val ServiceName = QName(Namespace, "Person_v3")
    private val EndpointName = QName(Namespace, "Person_v3Port")

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
