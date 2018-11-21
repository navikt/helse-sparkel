package no.nav.helse.ws.organisasjon

import io.grpc.stub.StreamObserver
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.oppslag.organisasjon.FinnOrganisasjonRequest
import no.nav.helse.oppslag.organisasjon.FinnOrganisasjonResponse
import no.nav.helse.oppslag.organisasjon.OrganisasjonServiceGrpc.OrganisasjonServiceImplBase

class OrganisasjonGrpc(val organisasjonClient: OrganisasjonClient) : OrganisasjonServiceImplBase() {

    override fun finnOrganisasjon(request: FinnOrganisasjonRequest, responseObserver: StreamObserver<FinnOrganisasjonResponse>) {
        val oppslagResult: OppslagResult = organisasjonClient.orgNavn(request.orgNr)

        when (oppslagResult) {
            is Success<*> -> {
                val name = oppslagResult.data as String
                responseObserver.onNext(FinnOrganisasjonResponse.newBuilder().setName(name).setOrgNr(request.orgNr).build())
                responseObserver.onCompleted()
            }
            is Failure -> {
                responseObserver.onError(Throwable("that didn't go so well..."))
            }
        }
    }
}