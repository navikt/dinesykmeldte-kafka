package no.nav.syfo.soknad.kafka.model

import no.nav.syfo.soknad.model.Svartype
import no.nav.syfo.soknad.model.Visningskriterium

data class Sporsmal(
    val id: String,
    val tag: String,
    val min: String?,
    val max: String?,
    val kriterieForVisningAvUndersporsmal: Visningskriterium?,
    val svarType: Svartype,
    val sporsmalstekst: String?,
    val undertekst: String?,
    val svar: List<Svar>,
    val undersporsmal: List<Sporsmal>,
)
