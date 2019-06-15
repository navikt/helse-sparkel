package no.nav.helse.oppslag

import no.nav.cxf.metrics.MetricFeature
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

class WsClientFactory(callIdGenerator: () -> String) {

    private val features = listOf(WSAddressingFeature(), LoggingFeature(), MetricFeature())
    private val outInterceptors = listOf(CallIdInterceptor(callIdGenerator))

    fun <ServicePort: Any> create(serviceClass: Class<ServicePort>,
                                  endpointUrl: String,
                                  wsdlUrl: String,
                                  serviceName: QName,
                                  endpointName: QName) =
            JaxWsProxyFactoryBean().apply {
                address = endpointUrl
                wsdlURL = wsdlUrl
                setServiceName(serviceName)
                setEndpointName(endpointName)
                setServiceClass(serviceClass)
                features.addAll(this@WsClientFactory.features)
                outInterceptors.addAll(this@WsClientFactory.outInterceptors)
            }.create(serviceClass)
}
