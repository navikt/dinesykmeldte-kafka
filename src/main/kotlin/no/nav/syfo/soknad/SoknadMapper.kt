package no.nav.syfo.soknad

import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SporsmalDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SvartypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykmeldingstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.VisningskriteriumDTO
import no.nav.syfo.soknad.db.SoknadDbModel
import no.nav.syfo.soknad.kafka.model.Sporsmal
import no.nav.syfo.soknad.kafka.model.Svar
import no.nav.syfo.soknad.model.Soknad
import no.nav.syfo.soknad.model.SoknadStatus
import no.nav.syfo.soknad.model.Soknadsperiode
import no.nav.syfo.soknad.model.Svartype
import no.nav.syfo.soknad.model.Sykmeldingstype
import no.nav.syfo.soknad.model.Visningskriterium

fun SykepengesoknadDTO.toSoknad(): Soknad {
    return Soknad(
        id = id,
        fnr = fnr,
        orgnummer = arbeidsgiver?.orgnummer,
        status = getStatus(status),
        sykmeldingId = sykmeldingId,
        sendtArbeidsgiver = sendtArbeidsgiver,
        fom = fom,
        tom = tom,
        sendtNav = sendtNav,
        sporsmal = sporsmal?.map { it.toSporsmal() } ?: emptyList(),
        soknadsperioder = soknadsperioder.toSoknadsperioder(),
    )
}

private fun List<SoknadsperiodeDTO>?.toSoknadsperioder(): List<Soknadsperiode> {
    if (this == null) {
        throw IllegalStateException("soknadsperioder is null")
    } else {
        return this.map { it.toSoknadsperiode() }
    }
}

private fun SoknadsperiodeDTO.toSoknadsperiode(): Soknadsperiode {
    return Soknadsperiode(
        fom = fom ?: throw IllegalStateException("soknadsperide.fom is null"),
        tom = tom ?: throw IllegalStateException("soknadsperide.tom is null"),
        sykmeldingsgrad = sykmeldingsgrad,
        sykmeldingstype = sykmeldingstype(),
    )
}

private fun SoknadsperiodeDTO.sykmeldingstype() =
    when (sykmeldingstype) {
        SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG -> Sykmeldingstype.AKTIVITET_IKKE_MULIG
        SykmeldingstypeDTO.GRADERT -> Sykmeldingstype.GRADERT
        SykmeldingstypeDTO.BEHANDLINGSDAGER -> Sykmeldingstype.BEHANDLINGSDAGER
        SykmeldingstypeDTO.AVVENTENDE -> Sykmeldingstype.AVVENTENDE
        SykmeldingstypeDTO.REISETILSKUDD -> Sykmeldingstype.REISETILSKUDD
        null -> throw IllegalStateException("sykmeldingstype is null")
    }

private fun SporsmalDTO.toSporsmal(): Sporsmal {
    return Sporsmal(
        id = id ?: throw IllegalStateException("sporsmal.id is null"),
        tag = tag ?: throw IllegalStateException("sporsmal.tag is null"),
        min = min,
        max = max,
        sporsmalstekst = sporsmalstekst,
        undertekst = undertekst,
        kriterieForVisningAvUndersporsmal = kriterieForVisningAvUndersporsmal.toVisningskriterium(),
        svarType = svartype.toSvarType(),
        svar = svar.toSvarList(),
        undersporsmal = undersporsmal?.map { it.toSporsmal() } ?: emptyList(),
    )
}

private fun List<SvarDTO>?.toSvarList(): List<Svar> {
    if (this == null) {
        throw IllegalStateException("Svar list is null")
    } else {
        return this.map { it.toSvar() }
    }
}

private fun SvarDTO.toSvar(): Svar {
    return Svar(verdi ?: throw IllegalStateException("svar.verdi is null"))
}

private fun SvartypeDTO?.toSvarType(): Svartype {
    return when (this) {
        SvartypeDTO.JA_NEI -> Svartype.JA_NEI
        SvartypeDTO.CHECKBOX -> Svartype.CHECKBOX
        SvartypeDTO.CHECKBOX_GRUPPE -> Svartype.CHECKBOX_GRUPPE
        SvartypeDTO.CHECKBOX_PANEL -> Svartype.CHECKBOX_PANEL
        SvartypeDTO.DATO -> Svartype.DATO
        SvartypeDTO.PERIODE -> Svartype.PERIODE
        SvartypeDTO.PERIODER -> Svartype.PERIODER
        SvartypeDTO.TIMER -> Svartype.TIMER
        SvartypeDTO.FRITEKST -> Svartype.FRITEKST
        SvartypeDTO.IKKE_RELEVANT -> Svartype.IKKE_RELEVANT
        SvartypeDTO.GRUPPE_AV_UNDERSPORSMAL -> Svartype.GRUPPE_AV_UNDERSPORSMAL
        SvartypeDTO.BEKREFTELSESPUNKTER -> Svartype.BEKREFTELSESPUNKTER
        SvartypeDTO.PROSENT -> Svartype.PROSENT
        SvartypeDTO.RADIO_GRUPPE -> Svartype.RADIO_GRUPPE
        SvartypeDTO.RADIO_GRUPPE_TIMER_PROSENT -> Svartype.RADIO_GRUPPE_TIMER_PROSENT
        SvartypeDTO.RADIO -> Svartype.RADIO
        SvartypeDTO.TALL -> Svartype.TALL
        SvartypeDTO.RADIO_GRUPPE_UKEKALENDER -> Svartype.RADIO_GRUPPE_UKEKALENDER
        SvartypeDTO.LAND -> Svartype.LAND
        SvartypeDTO.COMBOBOX_SINGLE -> Svartype.COMBOBOX_SINGLE
        SvartypeDTO.COMBOBOX_MULTI -> Svartype.COMBOBOX_MULTI
        SvartypeDTO.INFO_BEHANDLINGSDAGER -> Svartype.INFO_BEHANDLINGSDAGER
        SvartypeDTO.KVITTERING -> Svartype.KVITTERING
        SvartypeDTO.DATOER -> Svartype.DATOER
        SvartypeDTO.BELOP -> Svartype.BELOP
        SvartypeDTO.KILOMETER -> Svartype.KILOMETER
        null -> throw IllegalStateException("Svartype is null")
    }
}

private fun VisningskriteriumDTO?.toVisningskriterium(): Visningskriterium? {
    return when (this) {
        VisningskriteriumDTO.NEI -> Visningskriterium.NEI
        VisningskriteriumDTO.JA -> Visningskriterium.JA
        VisningskriteriumDTO.CHECKED -> Visningskriterium.CHECKED
        null -> null
    }
}

fun getStatus(status: SoknadsstatusDTO): SoknadStatus {
    return when (status) {
        SoknadsstatusDTO.NY -> SoknadStatus.NY
        SoknadsstatusDTO.SENDT -> SoknadStatus.SENDT
        SoknadsstatusDTO.FREMTIDIG -> SoknadStatus.FREMTIDIG
        SoknadsstatusDTO.KORRIGERT -> SoknadStatus.KORRIGERT
        SoknadsstatusDTO.AVBRUTT -> SoknadStatus.AVBRUTT
        SoknadsstatusDTO.SLETTET -> SoknadStatus.SLETTET
        SoknadsstatusDTO.UTGAATT -> SoknadStatus.UTGAATT
    }
}

fun SykepengesoknadDTO.toSoknadDbModel(): SoknadDbModel {
    val soknad = tilArbeidsgiverSoknad()
    return SoknadDbModel(
        soknadId = id,
        sykmeldingId = sykmeldingId,
        pasientFnr = fnr,
        orgnummer = arbeidsgiver?.orgnummer
                ?: throw IllegalStateException("Har mottatt sendt søknad uten orgnummer: $id"),
        soknad = soknad,
        sendtDato = sendtArbeidsgiver?.toLocalDate(),
        lest = false,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        tom = tom!!,
        sykepengesoknad = soknad.toSoknad(),
    )
}

fun SykepengesoknadDTO.tilArbeidsgiverSoknad(): SykepengesoknadDTO {
    return whitelistetForArbeidsgiver()
        .copy(
            andreInntektskilder = null,
            yrkesskade = null,
            arbeidUtenforNorge = null,
        )
}
