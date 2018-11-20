package no.nav.helse.ws.arbeidsforhold

import io.grpc.stub.StreamObserver
import no.nav.helse.oppslag.arbeidsforhold.CollectionOfArbeidsforhold
import no.nav.helse.oppslag.arbeidsforhold.FinnArbeidsforholdForArbeidstager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArbeidsforholdGrcpTest {

    @Test
    fun `exercise single arbeidsforhold and no errors`() {
        val service = ArbeidsforholdGrpc(arbeidsforholdClient = ArbeidsforholdClient(arbeidsforhold = ArbeidsforholdSOAPStub()))
        val streamObserver = MyStreamObserver()

        service.finnArbeidsforhold(
                FinnArbeidsforholdForArbeidstager.newBuilder().setNorskIdent("12345612345").build(),
                streamObserver
        )

        assertEquals(1, streamObserver.onNextCount)
        assertEquals(1, streamObserver.onCompleteCount)
        assertEquals(0, streamObserver.onErrorCount)
    }

}

class MyStreamObserver: StreamObserver<CollectionOfArbeidsforhold> {
    var onNextCount = 0
    var onErrorCount = 0
    var onCompleteCount = 0
    override fun onNext(value: CollectionOfArbeidsforhold) {
        onNextCount++
    }

    override fun onError(t: Throwable) {
        onErrorCount++
    }

    override fun onCompleted() {
        onCompleteCount++
    }

}