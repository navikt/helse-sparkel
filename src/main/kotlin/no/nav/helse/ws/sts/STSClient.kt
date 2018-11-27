package no.nav.helse.ws.sts

import org.apache.cxf.BusFactory
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.cxf.ws.security.trust.STSClient
import org.apache.neethi.Policy

val STS_CLIENT_AUTHENTICATION_POLICY = "classpath:ws/untPolicy.xml"
val STS_SAML_POLICY = "classpath:ws/requestSamlPolicy.xml"
val STS_SAML_POLICY_NO_TRANSPORT_BINDING = "classpath:ws/requestSamlPolicyNoTransportBinding.xml";

fun stsClient(stsUrl: String, credentials: Pair<String, String>): STSClient {
    return STSClient(BusFactory.getDefaultBus()).apply {
        isEnableAppliesTo = false
        isAllowRenewing = false

        location = stsUrl
        features = listOf(LoggingFeature())

        properties = mapOf(
                SecurityConstants.USERNAME to credentials.first,
                SecurityConstants.PASSWORD to credentials.second
        )

        setPolicy(STS_CLIENT_AUTHENTICATION_POLICY)
    }
}

fun STSClient.configureFor(servicePort: Any) {
    val client = ClientProxy.getClient(servicePort)
    client.configureSTS(this)
}

fun Client.configureSTS(stsClient: STSClient, policyUri: String = STS_SAML_POLICY) {
    requestContext[SecurityConstants.STS_CLIENT] = stsClient
    requestContext[SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = true

    setClientEndpointPolicy(resolvePolicy(policyUri))
}

private fun Client.resolvePolicy(policyUri: String): Policy {
    val policyBuilder = bus.getExtension(PolicyBuilder::class.java)
    val referenceResolver = RemoteReferenceResolver("", policyBuilder)
    return referenceResolver.resolveReference(policyUri)
}

private fun Client.setClientEndpointPolicy(policy: Policy) {
    val policyEngine: PolicyEngine = bus.getExtension(PolicyEngine::class.java)
    val message = SoapMessage(Soap12.getInstance())
    val endpointPolicy = policyEngine.getClientEndpointPolicy(endpoint.endpointInfo, null, message)
    policyEngine.setClientEndpointPolicy(endpoint.endpointInfo, endpointPolicy.updatePolicy(policy, message))
}
