package no.nav.helse

import io.ktor.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

class SparkelAuth(configuration: Configuration) {
    private val someProperty = configuration.someProperty

    class Configuration {
        var someProperty = "whatever"
    }

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        // TODO: auth stuff goes here
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, SparkelAuth.Configuration, SparkelAuth> {
        override val key = AttributeKey<SparkelAuth>("SparkelAuth")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): SparkelAuth {

            val configuration = SparkelAuth.Configuration().apply(configure)

            val feature = SparkelAuth(configuration)

            pipeline.intercept(ApplicationCallPipeline.Call) {
                feature.intercept(this)
            }

            return feature
        }
    }
}