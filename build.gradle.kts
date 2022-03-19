//import com.ewerk.gradle.plugins.tasks.QuerydslCompile

plugins {
    application
    idea
    id("com.google.cloud.tools.jib") version "3.2.0"
    id("io.freefair.lombok") version "6.4.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.springframework.boot") version "2.6.6"
}

group = "hk.edu.polyu.comp.vlabcontroller"
version = "1.0.3"
description = "VLabController"
java.sourceCompatibility = JavaVersion.VERSION_11

val springCloudVersion by extra("2021.0.1")

configurations {
    implementation.configure {
        exclude(module = "spring-boot-starter-tomcat")
        exclude("org.apache.tomcat")
    }
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

springBoot {
    buildInfo()
}

repositories {
    maven(url = "https://repo.spring.io/release")
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    var springBoot = run {
        compileOnly("org.springframework.boot", "spring-boot-devtools")
        implementation("org.springframework.boot", "spring-boot-configuration-processor")
        implementation("org.springframework.boot", "spring-boot-starter-actuator")
        implementation("org.springframework.boot", "spring-boot-starter-data-mongodb")
        implementation("org.springframework.boot", "spring-boot-starter-data-redis")
        implementation("org.springframework.boot", "spring-boot-starter-jdbc")
        implementation("org.springframework.boot", "spring-boot-starter-mail")
        implementation("org.springframework.boot", "spring-boot-starter-security")
        implementation("org.springframework.boot", "spring-boot-starter-thymeleaf")
        implementation("org.springframework.boot", "spring-boot-starter-undertow")
        implementation("org.springframework.boot", "spring-boot-starter-web")
        implementation("org.springframework.boot", "spring-boot-starter-websocket")
        implementation("org.springframework.cloud", "spring-cloud-context")
        implementation("org.springframework.data", "spring-data-commons")
        implementation("org.springframework.security", "spring-security-oauth2-client")
        implementation("org.springframework.security", "spring-security-oauth2-jose")
        implementation("org.springframework.security.oauth.boot", "spring-security-oauth2-autoconfigure")
        implementation("org.springframework.session", "spring-session-data-redis")

//        compile("org.springframework.data:spring-data-mongodb")

        testImplementation("org.springframework.boot", "spring-boot-starter-test")
        testImplementation("org.springframework.boot", "spring-boot-starter-webflux")
        testImplementation("org.springframework.security", "spring-security-test")
    }

    var database = run {
        implementation("mysql", "mysql-connector-java", "8.0.27")
        implementation("org.postgresql", "postgresql", "42.2.24")
        implementation("org.xerial", "sqlite-jdbc", "3.36.0.3")
        implementation("org.mongodb:mongodb-driver-sync:4.4.2")
        implementation("org.mongodb:bson:4.4.2")
    }

    var javax = run {
        implementation("javax.inject", "javax.inject", "1")
        implementation("javax.json", "javax.json-api", "1.1.4")
        implementation("javax.xml.bind", "jaxb-api", "2.3.1")
    }

    var queryDsl = run {
        annotationProcessor("com.querydsl:querydsl-apt:5.0.0:general")
        implementation("com.querydsl:querydsl-mongodb")
    }

    implementation("com.amazonaws", "aws-java-sdk-s3", "1.12.90")
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr353", "2.13.0")
    implementation("com.google.guava", "guava", "31.1-jre")
    implementation("io.fabric8", "kubernetes-client", "5.9.0")
    implementation("io.micrometer", "micrometer-registry-influx", "1.7.5")
    implementation("io.micrometer", "micrometer-registry-prometheus", "1.7.5")
    implementation("io.vavr", "vavr", "0.10.4")
    implementation("org.apache.commons", "commons-lang3", "3.12.0")
    implementation("org.glassfish", "javax.json", "1.1.4")
    implementation("org.jboss.xnio", "xnio-api", "3.8.4.Final")
    implementation("org.keycloak", "keycloak-spring-security-adapter", "15.0.2")
    implementation("org.thymeleaf.extras", "thymeleaf-extras-springsecurity5", "3.0.4.RELEASE")
    implementation("com.ea.async:ea-async:1.2.3")

    testImplementation("junit", "junit", "4.13.2")
}

jib {
    from {
        image = "ghcr.io/stevefan1999/vlab-controller-base"
    }
    to {
        image = "ghcr.io/endangeredf1sh/vlab-controller:$version"
        auth {
            username = System.getenv("REGISTRY_USERNAME")
            password = System.getenv("REGISTRY_PASSWORD")
        }
    }
    container {
        appRoot = "/opt/vlab-controller"
        workingDirectory = "/opt/vlab-controller"
        environment = mapOf(
            "VLAB_USER" to "vlab",
            "PROXY_TEMPLATEPATH" to "/opt/vlab-controller/resources/templates",
            "SERVER_ERROR_WHITELABEL_ENABLED" to "false",
            "TZ" to "Asia/Hong_Kong"
        )
        labels.put("maintainer", mapOf(
            "Aiden ZHANG Wenyi" to "im.endangeredfish@gmail.com",
            "Fan Chun Yin" to "stevefan1999@gmail.com"
        ).map { "${it.key} <${it.value}>" }.joinToString { "," })
        user = "vlab:vlab"
        args = listOf(
            "--spring.jmx.enabled=false",
            "--spring.config.location=/etc/vlab-controller/config/application.yml"
        )
        jvmFlags = listOf(
            "-server",
            "-Djava.awt.headless=true",
            "-XX:+UseStringDeduplication"
        )
    }
    extraDirectories {
        paths {
            path {
                setFrom("resources/templates")
                into = "/opt/vlab-controller/resources/templates"
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

val runEaAsyncInstrumentation by tasks.registering(JavaExec::class) {
    mainClass.set("com.ea.async.instrumentation.Main")
    classpath = sourceSets.main.get().compileClasspath
    args = listOf(buildDir.path)
}

val compileJava by tasks.existing(JavaCompile::class) {
    finalizedBy(runEaAsyncInstrumentation)
}