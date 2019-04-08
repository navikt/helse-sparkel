package no.nav.helse

import io.prometheus.client.Histogram
import io.prometheus.client.SimpleTimer

val latency = Histogram.build().name("request_latency_second").register()

class Call() {

    var before: Long = 0

    fun start(url: String, method: String): Call {
        latency.labels("url", url, "method", method)
        before = System.nanoTime()
        return this
    }

    fun after() {
        latency.observe(SimpleTimer.elapsedSecondsFromNanos(before, System.nanoTime()))
    }

}

