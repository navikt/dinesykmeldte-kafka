package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.serialization.jackson.jackson
import io.prometheus.client.hotspot.DefaultExports
import kotlin.collections.set
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.leaderelection.LeaderElection
import no.nav.syfo.azuread.AccessTokenClient
import no.nav.syfo.common.delete.DeleteDataDb
import no.nav.syfo.common.delete.DeleteDataService
import no.nav.syfo.common.exception.ServiceUnavailableException
import no.nav.syfo.common.kafka.CommonKafkaService
import no.nav.syfo.database.GcpDatabase
import no.nav.syfo.hendelser.HendelserService
import no.nav.syfo.hendelser.db.HendelserDb
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.narmesteleder.NarmestelederService
import no.nav.syfo.narmesteleder.db.NarmestelederDb
import no.nav.syfo.soknad.SoknadService
import no.nav.syfo.soknad.db.SoknadDb
import no.nav.syfo.syketilfelle.client.SyfoSyketilfelleClient
import no.nav.syfo.sykmelding.SykmeldingService
import no.nav.syfo.sykmelding.db.SykmeldingDb
import no.nav.syfo.sykmelding.pdl.client.PdlClient
import no.nav.syfo.sykmelding.pdl.service.PdlPersonService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.dinesykmeldte-kafka")
val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

@ExperimentalTime
@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val applicationEngine =
        createApplicationEngine(
            env,
            applicationState,
        )
    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                when (exception) {
                    is SocketTimeoutException ->
                        throw ServiceUnavailableException(exception.message)
                }
            }
        }
        install(HttpRequestRetry) {
            constantDelay(100, 0, false)
            retryOnExceptionIf(3) { request, throwable ->
                log.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(maxRetries) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    log.warn(
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}",
                    )
                    true
                } else {
                    false
                }
            }
        }
    }
    val httpClient = HttpClient(Apache, config)
    val accessTokenClient =
        AccessTokenClient(env.aadAccessTokenUrl, env.clientId, env.clientSecret, httpClient)
    val pdlClient =
        PdlClient(
            httpClient,
            env.pdlGraphqlPath,
        )
    val pdlPersonService = PdlPersonService(pdlClient, accessTokenClient, env.pdlScope)

    val syfoSyketilfelleClient =
        SyfoSyketilfelleClient(
            syketilfelleEndpointURL = env.syketilfelleEndpointURL,
            accessTokenClient = accessTokenClient,
            syketilfelleScope = env.syketilfelleScope,
            httpClient = httpClient,
        )

    val kafkaConsumer =
        KafkaConsumer(
            KafkaUtils.getAivenKafkaConfig("dinesykmeldte-backend-consumer")
                .also {
                    it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none"
                    it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 100
                }
                .toConsumerConfig("dinesykmeldte-backend", StringDeserializer::class),
            StringDeserializer(),
            StringDeserializer(),
        )
    val database = GcpDatabase(env)

    val narmestelederService = NarmestelederService(NarmestelederDb(database))
    val sykmeldingService =
        SykmeldingService(
            SykmeldingDb(database),
            pdlPersonService,
            syfoSyketilfelleClient,
            env.cluster,
        )
    val soknadService = SoknadService(SoknadDb(database), env.cluster)
    val hendelserService = HendelserService(HendelserDb(database))

    val commonKafkaService =
        CommonKafkaService(
            kafkaConsumer,
            applicationState,
            env,
            narmestelederService,
            sykmeldingService,
            soknadService,
            hendelserService,
        )
    commonKafkaService.startConsumer()
    val leaderElection = LeaderElection(httpClient, env.electorPath)
    DeleteDataService(
            DeleteDataDb(database),
            leaderElection,
            applicationState,
        )
        .start()
    ApplicationServer(applicationEngine, applicationState).start()
}
