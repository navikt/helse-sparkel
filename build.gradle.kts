import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4jVersion = "1.7.25"
val ktorVersion = "1.0.0"
val prometheusVersion = "0.5.0"
val cxfVersion = "3.2.7"
val orgJsonVersion = "20180813"
val fuelVersion = "1.15.1"
val wireMockVersion = "2.19.0"
val mockkVersion = "1.8.12.kotlin13"
val tjenestespesifikasjonerVersion = "1.2018.11.27-14.49-88288e06ce21"
val junitJupiterVersion = "5.3.1"
val mainClass = "no.nav.helse.AppKt"

plugins {
    application
    kotlin("jvm") version "1.3.10"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

application {
    mainClassName = "$mainClass"
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

    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")

    compile("javax.xml.ws:jaxws-api:2.3.1")
    compile("no.nav.tjenestespesifikasjoner:arbeidsforholdv3-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:person-v3-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:sakogbehandling-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:nav-fim-organisasjon-v5-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")
    compile("no.nav.tjenestespesifikasjoner:nav-fim-inntekt-v3-tjenestespesifikasjon:$tjenestespesifikasjonerVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion")
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
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
