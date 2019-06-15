package no.nav.helse.oppslag

import no.nav.cxf.metrics.MetricFeature
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPorts {

    init {
        System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")
    }

    fun ArbeidsfordelingV1(serviceUrl: String, callIdGenerator: () -> String): ArbeidsfordelingV1 {
        return createServicePort(serviceUrl,
                serviceClazz = ArbeidsfordelingV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsfordeling/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Binding",
                svcName = "Arbeidsfordeling_v1",
                portName = "Arbeidsfordeling_v1Port",
                callIdGenerator = callIdGenerator)
    }

    fun ArbeidsforholdV3(serviceUrl: String, callIdGenerator: () -> String): ArbeidsforholdV3 {
        return createServicePort(serviceUrl,
                serviceClazz = ArbeidsforholdV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsforhold/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding",
                svcName = "Arbeidsforhold_v3",
                portName = "Arbeidsforhold_v3Port",
                callIdGenerator = callIdGenerator)
    }

    fun InntektV3(serviceUrl: String, callIdGenerator: () -> String): InntektV3 {
        return createServicePort(serviceUrl,
                serviceClazz = InntektV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/inntekt/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/inntekt/v3/Binding",
                svcName = "Inntekt_v3",
                portName = "Inntekt_v3Port",
                callIdGenerator = callIdGenerator)
    }

    fun MeldekortUtbetalingsgrunnlagV1(serviceUrl: String, callIdGenerator: () -> String) =
            createServicePort(serviceUrl,
                    serviceClazz = MeldekortUtbetalingsgrunnlagV1::class.java,
                    wsdl = "wsdl/no/nav/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding.wsdl",
                    namespace = "http://nav.no/tjeneste/virksomhet/meldekortUtbetalingsgrunnlag/v1/Binding",
                    svcName = "MeldekortUtbetalingsgrunnlag_v1",
                    portName = "meldekortUtbetalingsgrunnlag_v1Port",
                    callIdGenerator = callIdGenerator
            )

    fun OrganisasjonV5(serviceUrl: String, callIdGenerator: () -> String): OrganisasjonV5 {
        return createServicePort(serviceUrl,
                serviceClazz = OrganisasjonV5::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/organisasjon/v5/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/organisasjon/v5/Binding",
                svcName = "Organisasjon_v5",
                portName = "Organisasjon_v5Port",
                callIdGenerator = callIdGenerator)
    }

    fun PersonV3(serviceUrl: String, callIdGenerator: () -> String): PersonV3 {
        return createServicePort(serviceUrl,
                serviceClazz = PersonV3::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding",
                svcName = "Person_v3",
                portName = "Person_v3Port",
                callIdGenerator = callIdGenerator)
    }

    fun InfotrygdBeregningsgrunnlagV1(serviceUrl: String, callIdGenerator: () -> String): InfotrygdBeregningsgrunnlagV1 {
        return createServicePort(serviceUrl,
                serviceClazz = InfotrygdBeregningsgrunnlagV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/Binding",
                svcName = "infotrygdBeregningsgrunnlag_v1",
                portName = "infotrygdBeregningsgrunnlag_v1Port",
                callIdGenerator = callIdGenerator)
    }

    fun InfotrygdSakV1(serviceUrl: String, callIdGenerator: () -> String): InfotrygdSakV1 {
        return createServicePort(serviceUrl,
                serviceClazz = InfotrygdSakV1::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/infotrygdSak/v1/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/infotrygdSak/v1/Binding",
                svcName = "InfotrygdSak_v1",
                portName = "infotrygdSak_v1Port",
                callIdGenerator = callIdGenerator)
    }

    private fun <PORT_TYPE> createServicePort(serviceUrl: String,
                                              serviceClazz: Class<PORT_TYPE>,
                                              wsdl: String,
                                              namespace: String,
                                              svcName: String,
                                              portName: String,
                                              callIdGenerator: () -> String): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            wsdlURL = wsdl
            serviceName = QName(namespace, svcName)
            endpointName = QName(namespace, portName)
            serviceClass = serviceClazz
            features = listOf(WSAddressingFeature(), LoggingFeature(), MetricFeature())
            outInterceptors.add(CallIdInterceptor(callIdGenerator))
        }

        return factory.create(serviceClazz)
    }
}
