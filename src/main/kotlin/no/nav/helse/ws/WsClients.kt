package no.nav.helse.ws

import no.nav.helse.http.aktør.*
import no.nav.helse.sts.*
import no.nav.helse.ws.arbeidsfordeling.*
import no.nav.helse.ws.arbeidsforhold.*
import no.nav.helse.ws.inntekt.*
import no.nav.helse.ws.meldekort.*
import no.nav.helse.ws.organisasjon.*
import no.nav.helse.ws.person.*
import no.nav.helse.ws.sakogbehandling.*
import no.nav.helse.ws.sts.*
import no.nav.helse.ws.sykepenger.*
import org.apache.cxf.ws.security.trust.*

class WsClients(val stsClientWs: STSClient, val stsClientRest: StsRestClient, val allowInsecureRequests: Boolean) {

    fun aktør(endpointUrl: String) = AktørregisterClient(endpointUrl, stsClientRest)

    fun organisasjon(endpointUrl: String): OrganisasjonClient {
        val port = SoapPorts.OrganisasjonV5(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return OrganisasjonClient(port)
    }

    fun person(endpointUrl: String): PersonClient {
        val port = SoapPorts.PersonV3(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return PersonClient(port)
    }

    fun arbeidsfordeling(endpointUrl: String): ArbeidsfordelingClient {
        val port = SoapPorts.ArbeidsfordelingV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return ArbeidsfordelingClient(port)
    }

    fun inntekt(endpointUrl: String): InntektClient {
        val port = SoapPorts.InntektV3(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return InntektClient(port)
    }

    fun arbeidsforhold(endpointUrl: String): ArbeidsforholdClient {
        val port = SoapPorts.ArbeidsforholdV3(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }

        return ArbeidsforholdClient(port)
    }

    fun sakOgBehandling(endpointUrl: String): SakOgBehandlingClient {
        val port = SoapPorts.SakOgBehandlingV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return SakOgBehandlingClient(port)
    }

    fun sykepengeliste(endpointUrl: String): SykepengerClient {
        val port = SoapPorts.SykepengerV2(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return SykepengerClient(port)
    }

    fun meldekort(endpointUrl: String): MeldekortClient {
        val port = SoapPorts.MeldekortUtbetalingsgrunnlagV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return MeldekortClient(port)
    }

}