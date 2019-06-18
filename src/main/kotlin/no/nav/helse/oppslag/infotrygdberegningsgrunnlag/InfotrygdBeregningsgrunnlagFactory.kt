package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import org.apache.cxf.feature.Feature
import org.apache.cxf.interceptor.Interceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import javax.xml.namespace.QName

object InfotrygdBeregningsgrunnlagFactory {

    private val ServiceClass = InfotrygdBeregningsgrunnlagV1::class.java
    private val Wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding.wsdl"
    private val Namespace = "http://nav.no/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding"
    private val ServiceName = QName(Namespace, "infotrygdBeregningsgrunnlag_v1")
    private val EndpointName = QName(Namespace, "infotrygdBeregningsgrunnlag_v1Port")

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
