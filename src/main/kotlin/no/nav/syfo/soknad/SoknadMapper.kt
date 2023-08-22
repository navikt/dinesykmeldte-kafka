package no.nav.syfo.soknad

import no.nav.helse.flex.sykepengesoknad.arbeidsgiverwhitelist.whitelistetForArbeidsgiver
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.syfo.soknad.db.SoknadDbModel


fun SykepengesoknadDTO.toSoknadDbModel(): SoknadDbModel {
    return SoknadDbModel(
        soknadId = id,
        sykmeldingId = sykmeldingId,
        pasientFnr = fnr,
        orgnummer = arbeidsgiver?.orgnummer
            ?: throw IllegalStateException("Har mottatt sendt søknad uten orgnummer: $id"),
        soknad = tilArbeidsgiverSoknad(),
        sendtDato = sendtArbeidsgiver?.toLocalDate(),
        lest = false, // oppdateres fra strangler
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        tom = tom!!,
    )
}

fun SykepengesoknadDTO.tilArbeidsgiverSoknad(): SykepengesoknadDTO {
    return whitelistetForArbeidsgiver().copy(
            andreInntektskilder = null,
            yrkesskade = null,
            arbeidUtenforNorge = null,
    )
}



