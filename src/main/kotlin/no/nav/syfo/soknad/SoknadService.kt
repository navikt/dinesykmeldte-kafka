package no.nav.syfo.soknad

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.syfo.application.metrics.SOKNAD_TOPIC_COUNTER
import no.nav.syfo.log
import no.nav.syfo.objectMapper
import no.nav.syfo.soknad.db.SoknadDb
import no.nav.syfo.soknad.model.Soknad
import no.nav.syfo.soknad.model.SoknadStatus
import org.apache.kafka.clients.consumer.ConsumerRecord

class SoknadService(
    private val soknadDb: SoknadDb,
    private val cluster: String,
) {
    suspend fun handleSykepengesoknad(record: ConsumerRecord<String, String>) {
        try {
            handleSykepengesoknad(objectMapper.readValue<Soknad>(record.value()))
        } catch (exception: InvalidFormatException) {
            if (cluster != "dev-gcp") {
                log.error("Noe gikk galt ved mottak av sykepengesøknad med id ${record.key()}")
                throw exception
            } else {
                log.info(
                    "Ignoring sykepengesøknad when error InvalidFormatException med id: ${record.key()}"
                )
            }
        } catch (exception: Exception) {
            log.error("Noe gikk galt ved mottak av sykepengesøknad med id ${record.key()}")
            throw exception
        }
    }

    suspend fun handleSykepengesoknad(sykepengesoknad: Soknad) {
        if (shouldHandleSoknad(sykepengesoknad)) {
            when (sykepengesoknad.status) {
                SoknadStatus.NY -> soknadDb.insertOrUpdate(sykepengesoknad.toSoknadDbModel())
                SoknadStatus.FREMTIDIG -> soknadDb.insertOrUpdate(sykepengesoknad.toSoknadDbModel())
                SoknadStatus.SENDT -> handleSendt(sykepengesoknad)
                SoknadStatus.KORRIGERT -> handleSendt(sykepengesoknad)
                SoknadStatus.AVBRUTT -> soknadDb.deleteSoknad(sykepengesoknad.id)
                SoknadStatus.SLETTET -> soknadDb.deleteSoknad(sykepengesoknad.id)
                SoknadStatus.UTGAATT -> soknadDb.deleteSoknad(sykepengesoknad.id)
            }
        }
        SOKNAD_TOPIC_COUNTER.inc()
    }

    private fun handleSendt(sykepengesoknad: SykepengesoknadDTO) {
        when (sykepengesoknad.sendtArbeidsgiver != null) {
            true -> {
                soknadDb.insertOrUpdate(sykepengesoknad.toSoknadDbModel())
            }
            else -> soknadDb.deleteSoknad(sykepengesoknad.id)
        }
    }

    private fun hasArbeidsgiver(sykepengesoknad: Soknad): Boolean {
        return sykepengesoknad.arbeidsgiver?.orgnummer != null
    }

    private fun shouldHandleSoknad(sykepengesoknad: Soknad) =
        hasArbeidsgiver(sykepengesoknad) &&
            sykepengesoknad.tom?.isAfter(LocalDate.now().minusMonths(4)) == true
}
