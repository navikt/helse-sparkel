package no.nav.helse.ws;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import java.util.function.Supplier;

public class WsClientBuilder {

    private final Supplier<JaxWsProxyFactoryBean> factorySupplier;

    public WsClientBuilder() {
        this(JaxWsProxyFactoryBean::new);
    }

    public WsClientBuilder(Supplier<JaxWsProxyFactoryBean> factorySupplier) {
        this.factorySupplier = factorySupplier;
    }

    @SuppressWarnings("unchecked")
    public <T> T createPort(String serviceUrl, Class<T> serviceClass) {
        JaxWsProxyFactoryBean factory = factorySupplier.get();

        factory.setAddress(serviceUrl);
        factory.setServiceClass(serviceClass);
        factory.getFeatures().add(new LoggingFeature());

        return (T)factory.create();
    }
}
