package no.nav.helse.oppslag.arena

import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import org.apache.cxf.feature.Feature
import org.apache.cxf.interceptor.Interceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import javax.xml.namespace.QName

object MeldekortUtbetalingsgrunnlagFactory {

    private val ServiceClass = MeldekortUtbetalingsgrunnlagV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding"
    private val ServiceName = QName(Namespace, "MeldekortUtbetalingsgrunnlag_v1")
    private val EndpointName = QName(Namespace, "meldekortUtbetalingsgrunnlag_v1Port")

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
