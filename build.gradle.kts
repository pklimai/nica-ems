import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val kotlinVersion = "1.9.22"
val serializationVersion = "1.6.2"
val ktorVersion = "2.3.8"
val logbackVersion = "1.4.14"
val kotlinxHtmlVersion = "0.11.0"
val reactVersion = "18.2.0-pre.695"  // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react
val muiVersion = "5.15.7-pre.695"    // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-mui-material
val pgsqlDriverVersion = "42.7.1"    // https://mvnrepository.com/artifact/org.postgresql/postgresql
val jacksonYamlVersion = "2.16.1"    // https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml
val keycloakAuthzVersion = "23.0.6"  // https://mvnrepository.com/artifact/org.keycloak/keycloak-authz-client

plugins {
    kotlin("multiplatform") version "1.9.22"
    application //to run JVM part
    kotlin("plugin.serialization") version "1.9.22"
}

group = "ru.mipt.npm.nica.ems"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
                implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-server-compression:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
                implementation("io.ktor:ktor-server-auth:$ktorVersion")
                // implementation("io.ktor:ktor-server-auth-ldap:$ktorVersion")
                implementation("io.ktor:ktor-server-openapi:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("org.postgresql:postgresql:$pgsqlDriverVersion")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonYamlVersion")
                // implementation("com.unboundid:unboundid-ldapsdk:6.0.1")
                implementation("org.keycloak:keycloak-admin-client:23.0.6")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-auth:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.github.developer--:JwtParser:1.0.0")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-client-auth:$ktorVersion")
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-legacy:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom-legacy:$reactVersion")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-material:$muiVersion")

                implementation(npm("@emotion/react", "11.7.1"))
                implementation(npm("@emotion/styled", "11.6.0"))
                implementation(npm("@mui/x-data-grid", "5.4.0"))

                implementation(npm("highcharts-react-official", "3.1.0"))
                implementation(npm("highcharts", "10.1.0"))
            }
        }
    }
}

application {
    mainClass.set("ru.mipt.npm.nica.ems.ApplicationKt")
}

// include JS artifacts in any JAR we generate
tasks.named<Jar>("jvmJar").configure {
    val taskName = if (project.hasProperty("isProduction")
        || project.gradle.startParameter.taskNames.contains("installDist")
    ) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val webpackTask = tasks.named<KotlinWebpack>(taskName)
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(webpackTask.map { it.mainOutputFile.get().asFile }) // bring output file along into the JAR
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

// Alias "installDist" as "stage" (for cloud providers)
tasks.create("stage") {
    dependsOn(tasks.getByName("installDist"))
}

tasks.getByName<JavaExec>("run") {
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}
