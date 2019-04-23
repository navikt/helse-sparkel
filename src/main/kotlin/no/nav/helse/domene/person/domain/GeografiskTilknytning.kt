package no.nav.helse.domene.person.domain

data class GeografiskTilknytning(
        val diskresjonskode: Diskresjonskode?,
        val geografiskOmraade: GeografiskOmraade?
)
