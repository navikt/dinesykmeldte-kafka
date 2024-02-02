package no.nav.syfo.soknad.model

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.soknad.kafka.model.Sporsmal

data class Soknad(
    val id: String,
    val status: SoknadStatus,
    val fnr: String,
    val orgnummer: String?,
    val sykmeldingId: String?,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val sendtNav: LocalDateTime?,
    val sendtArbeidsgiver: LocalDateTime?,
    val sporsmal: List<Sporsmal>,
    val soknadsperioder: List<Soknadsperiode>,
)
