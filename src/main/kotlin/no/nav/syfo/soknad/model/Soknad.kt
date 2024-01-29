package no.nav.syfo.soknad.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Soknad(
    val id: String,
    val status: SoknadStatus,
    val fnr: String,
    val sykmeldingId: String? = null,
    val arbeidsgiver: Arbeidsgiver? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val sendtNav: LocalDateTime? = null,
    val sendtArbeidsgiver: LocalDateTime? = null,
)
