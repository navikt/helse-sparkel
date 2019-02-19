package no.nav.helse.ws

import no.nav.cxf.metrics.MetricFeature
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.medlemskap.v2.MedlemskapV2
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPorts {

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

    fun InfotrygdBeregningsgrunnlagConsumerConfig(serviceUrl: String): InfotrygdBeregningsgrunnlagV1 {
        return createServicePort(serviceUrl,
                serviceClazz = InfotrygdBeregningsgrunnlagV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding",
                svcName = "infotrygdBeregningsgrunnlag_v1",
                portName = "infotrygdBeregningsgrunnlag_v1Port")
    }

    fun InfotrygdSakV1(serviceUrl: String): InfotrygdSakV1 {
        return createServicePort(serviceUrl,
                serviceClazz = InfotrygdSakV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdSak/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/infotrygdSak/v1/Binding",
                svcName = "InfotrygdSak_v1",
                portName = "infotrygdSak_v1Port")
    }

    fun InntektV3(serviceUrl: String): InntektV3 {
        return createServicePort(serviceUrl,
                serviceClazz = InntektV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/inntekt/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/inntekt/v3/Binding",
                svcName = "Inntekt_v3",
                portName = "Inntekt_v3Port")
    }

    fun MedlemskapV2(serviceUrl: String): MedlemskapV2 {
        return createServicePort(serviceUrl,
                serviceClazz = MedlemskapV2::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/medlemskap/v2/MedlemskapV2.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/medlemskap/v2",
                svcName = "Medlemskap_v2",
                portName = "Medlemskap_v2Port")
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

    fun MeldekortUtbetalingsgrunnlagV1(serviceUrl: String): MeldekortUtbetalingsgrunnlagV1 {
        return createServicePort(serviceUrl,
                serviceClazz = MeldekortUtbetalingsgrunnlagV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding",
                svcName = "MeldekortUtbetalingsgrunnlag_v1",
                portName = "meldekortUtbetalingsgrunnlag_v1Port")
    }

    fun <PORT_TYPE> createServicePort(serviceUrl: String, serviceClazz: Class<PORT_TYPE>, wsdl: String, namespace: String, svcName: String, portName: String): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            wsdlURL = wsdl
            serviceName = QName(namespace, svcName)
            endpointName = QName(namespace, portName)
            serviceClass = serviceClazz
            features = listOf(WSAddressingFeature(), LoggingFeature(), MetricFeature())
            outInterceptors.add(CallIdInterceptor())
        }

        return factory.create(serviceClazz)
    }
}
