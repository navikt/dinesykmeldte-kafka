package no.nav.syfo.soknad.db

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.syfo.objectMapper
import no.nav.syfo.soknad.model.Soknad
import org.postgresql.util.PGobject

data class SoknadDbModel(
    val soknadId: String,
    val sykmeldingId: String?,
    val pasientFnr: String,
    val orgnummer: String,
    val sendtDato: LocalDate?,
    val lest: Boolean,
    val timestamp: OffsetDateTime,
    val tom: LocalDate,
    val sykepengesoknad: Soknad
)

fun Any.toPGObject() =
    PGobject().also {
        it.type = "json"
        it.value = objectMapper.writeValueAsString(this)
    }
