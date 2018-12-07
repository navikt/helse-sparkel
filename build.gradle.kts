import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4jVersion = "1.7.25"
val ktorVersion = "1.0.0"
val prometheusVersion = "0.5.0"
val cxfVersion = "3.2.7"
val orgJsonVersion = "20180813"
val fuelVersion = "1.15.1"
val wireMockVersion = "2.19.0"
val mockkVersion = "1.8.12.kotlin13"
val tjenestespesifikasjonerVersion = "1.2018.12.07-12.38-73cf10f9640d"
val junitJupiterVersion = "5.3.1"
val mainClass = "no.nav.helse.AppKt"

plugins {
    kotlin("jvm") version "1.3.10"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    compile(kotlin("stdlib"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("net.logstash.logback:logstash-logback-encoder:5.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-auth-jwt:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    compile("org.json:json:$orgJsonVersion")
    compile("com.github.kittinunf.fuel:fuel:$fuelVersion")

    compile("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    compile("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    testCompile("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")

    compile("com.sun.xml.ws:jaxws-rt:2.3.0")
    compile("no.nav.tjenestespesifikasjoner:arbeidsforholdv3-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:person-v3-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:sakogbehandling-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:nav-fim-organisasjon-v5-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:nav-fim-inntekt-v3-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")

    testCompile("io.mockk:mockk:$mockkVersion")
    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion")
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testCompile("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
    }
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    configurations["compile"].forEach {
        val file = File("$buildDir/libs/${it.name}")
        if (!file.exists())
            it.copyTo(file)
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0"
}
