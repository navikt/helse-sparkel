package no.nav.helse.ws

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object Clients {

    fun ArbeidsfordelingV1(serviceUrl: String): ArbeidsfordelingV1 {
        return createServicePort(serviceUrl,
                serviceClazz = ArbeidsfordelingV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsfordeling/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Binding",
                svcName = "Arbeidsfordeling_v1",
                portName = "Arbeidsfordeling_v1Port")
    }

    fun ArbeidsforholdV3(serviceUrl: String): ArbeidsforholdV3 {
        return createServicePort(serviceUrl,
                serviceClazz = ArbeidsforholdV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsforhold/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding",
                svcName = "Arbeidsforhold_v3",
                portName = "Arbeidsforhold_v3Port")
    }

    fun InntektV3(serviceUrl: String): InntektV3 {
        return createServicePort(serviceUrl,
                serviceClazz = InntektV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/inntekt/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/inntekt/v3/Binding",
                svcName = "Inntekt_v3",
                portName = "Inntekt_v3Port")
    }

    fun OrganisasjonV5(serviceUrl: String): OrganisasjonV5 {
        return createServicePort(serviceUrl,
                serviceClazz = OrganisasjonV5::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/organisasjon/v5/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/organisasjon/v5/Binding",
                svcName = "Organisasjon_v5",
                portName = "Organisasjon_v5Port")
    }

    fun PersonV3(serviceUrl: String): PersonV3 {
        return createServicePort(serviceUrl,
                serviceClazz = PersonV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding",
                svcName = "Person_v3",
                portName = "Person_v3Port")
    }

    fun SakOgBehandlingV1(serviceUrl: String): SakOgBehandlingV1 {
        return createServicePort(serviceUrl,
                serviceClazz = SakOgBehandlingV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/sakOgBehandling/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1/Binding",
                svcName = "SakOgBehandling_v1",
                portName = "SakOgBehandling_v1Port")
    }

    fun SykepengerV2(serviceUrl: String): SykepengerV2 {
        return createServicePort(serviceUrl,
                serviceClazz = SykepengerV2::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/sykepenger/v2/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/sykepenger/v2/Binding",
                svcName = "Sykepenger_v2",
                portName = "Sykepenger_v2Port")
    }

    fun <PORT_TYPE> createServicePort(serviceUrl: String, serviceClazz: Class<PORT_TYPE>, wsdl: String, namespace: String, svcName: String, portName: String): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            wsdlURL = wsdl
            serviceName = QName(namespace, svcName)
            endpointName = QName(namespace, portName)
            serviceClass = serviceClazz
            features = listOf(WSAddressingFeature(), LoggingFeature())
            outInterceptors.add(CallIdInterceptor())
        }

        return factory.create(serviceClazz)
    }
}
