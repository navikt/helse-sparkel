package no.nav.helse.oppslag.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import org.apache.cxf.feature.Feature
import org.apache.cxf.interceptor.Interceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import javax.xml.namespace.QName

object OrganisasjonFactory {

    private val ServiceClass = OrganisasjonV5::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/organisasjon/v5/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/organisasjon/v5/Binding"
    private val ServiceName = QName(Namespace, "Organisasjon_v5")
    private val EndpointName = QName(Namespace, "Organisasjon_v5Port")

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
