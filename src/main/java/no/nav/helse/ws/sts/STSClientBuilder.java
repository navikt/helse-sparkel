package no.nav.helse.ws.sts;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class STSClientBuilder {

    private static final String STS_CLIENT_AUTHENTICATION_POLICY = "classpath:ws/untPolicy.xml";

    private final Bus bus;

    public STSClientBuilder(Bus bus) {
        this.bus = bus;
    }

    public STSClientBuilder() {
        this(BusFactory.newInstance().createBus());
    }

    public STSClient build(STSProperties properties) {
        STSClient stsClient = new STSClient(bus);

        stsClient.setEnableAppliesTo(false);
        stsClient.setAllowRenewing(false);
        stsClient.setLocation(properties.getUrl().toString());
        stsClient.setFeatures(new ArrayList<Feature>(Arrays.asList(new LoggingFeature())));

        HashMap<String, Object> props = new HashMap<>();
        props.put(SecurityConstants.USERNAME, properties.getUsername());
        props.put(SecurityConstants.PASSWORD, properties.getPassword());

        stsClient.setProperties(props);
        // used for the STS client to authenticate itself to the STS provider.
        stsClient.setPolicy(STS_CLIENT_AUTHENTICATION_POLICY);

        return stsClient;
    }
}
