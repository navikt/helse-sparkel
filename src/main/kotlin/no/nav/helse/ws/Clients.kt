package no.nav.helse.ws

import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean

object Clients {

    fun <PORT_TYPE> createServicePort(serviceUrl: String, serviceClazz: Class<PORT_TYPE>): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            serviceClass = serviceClazz
            features = listOf(LoggingFeature())
        }

        @Suppress("UNCHECKED_CAST")
        return factory.create() as PORT_TYPE
    }
}
