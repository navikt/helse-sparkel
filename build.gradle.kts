import com.google.protobuf.gradle.*

val slf4jVersion = "1.7.25"
val ktorVersion = "1.0.0-beta-3"
val prometheusVersion = "0.5.0"
val cxfVersion = "3.2.6"
val orgJsonVersion = "20180813"
val fuelVersion = "1.15.1"
val wireMockVersion = "2.19.0"
val mockkVersion = "1.8.12.kotlin13"

val junitJupiterVersion = "5.3.1"
val mainClass = "no.nav.helse.AppKt"

val grpcCoreVersion = "1.15.1"
val grcpProtocVersion = "3.6.1"

plugins {
    application
    kotlin("jvm") version "1.3.10"
    id("com.google.protobuf") version "0.8.7"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

application {
    mainClassName = "$mainClass"
}

sourceSets {
    getByName("main").java.srcDirs("src/main/java", "src/main/kotlin")
    getByName("test").java.srcDirs("src/test/kotlin")
}

dependencies {
    compile(kotlin("stdlib"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("net.logstash.logback:logstash-logback-encoder:5.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    compile("org.json:json:$orgJsonVersion")
    compile("com.github.kittinunf.fuel:fuel:$fuelVersion")

    compile("com.google.protobuf:protobuf-java:$grcpProtocVersion")
    compile("io.grpc:grpc-stub:$grpcCoreVersion")
    compile("io.grpc:grpc-protobuf:$grpcCoreVersion")
    compile("io.grpc:grpc-netty:$grpcCoreVersion")

    implementation("com.sun.xml.ws:jaxws-tools:2.3.0.2")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-policy:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")

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

    val mainJavaSourceSet: SourceDirectorySet = sourceSets.getByName("main").java
    mainJavaSourceSet.srcDir("$projectDir/build/generated-sources/main")
}

val wsdlDir = "$projectDir/src/main/resources/wsdl"
val wsdlsToGenerate = listOf(
        "$wsdlDir/person/Binding.wsdl",
        "$wsdlDir/arbeidsforhold/Binding.wsdl",
        "$wsdlDir/inntekt/Binding.wsdl",
        "$wsdlDir/organisasjon/Binding.wsdl"
)
val generatedDir = "$projectDir/build/generated-sources"

tasks {
    register("wsimport") {
        group = "other"
        doLast {
            mkdir("$generatedDir/main")
            wsdlsToGenerate.forEach {
                ant.withGroovyBuilder {
                    "taskdef"("name" to "wsimport", "classname" to "com.sun.tools.ws.ant.WsImport", "classpath" to sourceSets.getAt("main").runtimeClasspath.asPath)
                    "wsimport"("wsdl" to it, "sourcedestdir" to "$generatedDir/main", "xnocompile" to true) {}
                }
            }
        }
    }
}
tasks.getByName("compileKotlin").dependsOn("wsimport")

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "4.10.2"
}

protobuf {
    this.generatedFilesBaseDir = generatedDir
    protoc {
        artifact = "com.google.protobuf:protoc:$grcpProtocVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcCoreVersion"
        }
    }
    /*
    the "outputSubDir"-hullabaloo is to make everything be put in a sane folder
    where intellij can see them and that fits somewhat with the declared packages.
     */
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") {
                    outputSubDir = ""
                }
            }
            it.builtins.forEach{it.outputSubDir = ""}
        }
    }
}