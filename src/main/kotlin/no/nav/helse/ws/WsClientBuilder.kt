package no.nav.helse.ws

import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean

class WsClientBuilder(private val sts: EndpointSTSClientConfig) {
    private fun <PORT_TYPE> createServicePort(serviceUrl: String, serviceClass: Class<PORT_TYPE>): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean()

        factory.address = serviceUrl
        factory.serviceClass = serviceClass
        factory.features.add(LoggingFeature())

        @Suppress("UNCHECKED_CAST")
        return factory.create() as PORT_TYPE
    }

    fun <PORT_TYPE, CLIENT_TYPE> createSOAPClient(
            serviceLocation: String,
            serviceClass: Class<PORT_TYPE>,
            clientClass: Class<CLIENT_TYPE>
    ): CLIENT_TYPE {
        val port: PORT_TYPE = createServicePort(serviceLocation, serviceClass)
        sts.configureRequestSamlToken(port, EndpointSTSClientConfig.STS_SAML_POLICY)

        @Suppress("UNCHECKED_CAST") // if T's constructor doesn't produce a T, then we have bigger problems than a class cast exception
        return clientClass.constructors[0].newInstance(port) as CLIENT_TYPE
    }
}