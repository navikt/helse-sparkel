package no.nav.helse.ws;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.Policy;

public class EndpointSTSClientConfig {

    public static final String STS_SAML_POLICY = "classpath:ws/requestSamlPolicy.xml";
    public static final String STS_SAML_POLICY_NO_TRANSPORT_BINDING = "classpath:ws/requestSamlPolicyNoTransportBinding.xml";

    private final STSClient stsClient;

    public EndpointSTSClientConfig(STSClient stsClient) {
        this.stsClient = stsClient;
    }

    public <T> T configureRequestSamlToken(T port) {
        return configureRequestSamlToken(port, STS_SAML_POLICY);
    }

    public <T> T configureRequestSamlToken(T port, String policyUri) {
        Client client = ClientProxy.getClient(port);

        configureEndpointWithPolicyForSTS(client, policyUri);

        return port;
    }

    private void configureEndpointWithPolicyForSTS(Client client, String policyUri) {
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, true);

        configureEndpointPolicyReference(client, policyUri);
    }

    private void configureEndpointPolicyReference(Client client, String policyUri) {
        Policy policy = resolvePolicyReference(client.getBus(), policyUri);
        setClientEndpointPolicy(client, client.getBus(), policy);
    }

    private Policy resolvePolicyReference(Bus bus, String uri) {
        PolicyBuilder policyBuilder = bus.getExtension(PolicyBuilder.class);
        ReferenceResolver referenceResolver = new RemoteReferenceResolver("", policyBuilder);
        return referenceResolver.resolveReference(uri);
    }

    private void setClientEndpointPolicy(Client client, Bus bus, Policy policy) {
        PolicyEngine policyEngine = bus.getExtension(PolicyEngine.class);
        SoapMessage message = new SoapMessage(Soap12.getInstance());
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();
        EndpointPolicy endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, null, message);
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, message));
    }
}
