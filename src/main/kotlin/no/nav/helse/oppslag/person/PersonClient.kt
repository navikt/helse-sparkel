package no.nav.helse.oppslag.person

import arrow.core.Try
import no.nav.helse.domene.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest

class PersonClient(private val personV3: PersonV3) {

    fun personInfo(id: AktørId) =
            Try {
                personV3.hentPerson(hentPersonRequest(id, listOf(Informasjonsbehov.ADRESSE))).person
            }

    fun geografiskTilknytning(id : AktørId) =
            Try {
                personV3.hentGeografiskTilknytning(hentGeografiskTilknytningRequest(id))
            }

    fun familierelasjoner(id : AktørId) =
            Try {
                personV3.hentPerson(hentPersonRequest(id, listOf(Informasjonsbehov.FAMILIERELASJONER))).person.harFraRolleI.toList()
            }

    private fun hentPersonRequest(id: AktørId, informasjonsbehov : List<Informasjonsbehov>) =
            HentPersonRequest().apply {
                aktoer = AktoerId().apply {
                    aktoerId = id.aktor
                }
                withInformasjonsbehov(informasjonsbehov)
            }

    private fun hentGeografiskTilknytningRequest(id: AktørId) =
            HentGeografiskTilknytningRequest().apply {
                aktoer = AktoerId().apply {
                    aktoerId = id.aktor
                }
            }
}







