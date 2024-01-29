package no.nav.syfo.soknad

import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.helse.flex.sykepengesoknad.arbeidsgiverwhitelist.whitelistetForArbeidsgiver
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.syfo.soknad.db.SoknadDbModel
import no.nav.syfo.soknad.model.Soknad

fun Soknad.toSoknadDbModel(): SoknadDbModel {
    return SoknadDbModel(
        soknadId = id,
        sykmeldingId = sykmeldingId,
        pasientFnr = fnr,
        orgnummer = arbeidsgiver?.orgnummer
                ?: throw IllegalStateException("Har mottatt sendt søknad uten orgnummer: $id"),
        soknad = tilArbeidsgiverSoknad(),
        sendtDato = sendtArbeidsgiver?.toLocalDate(),
        lest = false,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        tom = tom!!,
    )
}

fun Soknad.tilArbeidsgiverSoknad(): Soknad {
    return whitelistetForArbeidsgiver()
        .copy(
            andreInntektskilder = null,
            yrkesskade = null,
            arbeidUtenforNorge = null,
        )
}
