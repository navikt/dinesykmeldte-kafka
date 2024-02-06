package no.nav.syfo.soknad

import no.nav.syfo.log
import no.nav.syfo.soknad.db.SoknadDb

class UpdateSoknadService(val soknadDb: SoknadDb) {
    fun updateSoknader() {
        log.info("updating soknader")
        val soknaderToUpdate = soknadDb.getSoknader()
        log.info("Updated soknader ${soknaderToUpdate.size}")
        soknaderToUpdate.forEach {
            val newSoknad = it.second.toSoknad()
            soknadDb.insertSoknad(it.first, newSoknad)
        }
        log.info("Done updating soknader")
    }
}
