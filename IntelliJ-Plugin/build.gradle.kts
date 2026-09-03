import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")

    // Jit za stub dependecy
    implementation("com.github.RAFSoftLab.raflms-modular:studentstub:master-SNAPSHOT")
    implementation("com.github.RAFSoftLab.raflms-modular:trackingstub:master-SNAPSHOT")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // Unified IntelliJ IDEA distribution (IC is no longer a separate artifact since 2025.3 / 253).
        // It ships the Ultimate-only plugins too; without a licence they can't load and spam the
        // log on every runIde - the `runIde` task below disables them in the dev sandbox.
        intellijIdea(properties("platformVersion"))

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        bundledPlugins(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// JVM toolchain used to build the plugin. Platform 2026.1 (261) requires JDK 21.
kotlin {
    @Suppress("UnstableApiUsage")
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            // Empty `pluginUntilBuild` -> no upper bound (compatible with all future IDE versions)
            val until = properties("pluginUntilBuild").orNull
            untilBuild = if (until.isNullOrBlank()) provider { null } else provider { until }
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    pluginVerification {
        ides {
            // `recommended()` povlači sve najnovije patch/EAP build-ove (2026.1.5, ...) i skida ih zasebno.
            // `current()` verifikuje protiv iste platforme na kojoj se plugin gradi (platformVersion, 2026.1.3) -
            // bez dodatnog preuzimanja.
            current()
        }
    }

    buildSearchableOptions = false
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    publishPlugin {
        dependsOn("patchChangelog")
    }

    test {
        // Nema jos test klasa; Gradle 9 podrazumevano obara `test` kad se nista ne otkrije.
        failOnNoDiscoveredTests = false
    }

    prepareSandbox {
        // Dev sandbox only. The unified IntelliJ IDEA distribution bundles every Ultimate plugin
        // (Spring Boot/Cloud/..., Jakarta EE, JS debugger/Node/Karma/Next, Kubernetes, Docker
        // gateway, FreeMarker, Velocity, ...). Without an Ultimate licence they fail to load and
        // each one logs "has dependency on 'JetBrains Ultimate' which cannot be loaded" on every
        // launch. None are used by the student workflow, so list them here - the IDE then treats
        // them as intentionally disabled and stays quiet. Written to
        // <sandbox>/config/disabled_plugins.txt; unknown ids are ignored.
        //
        // This is the closure of "bundled plugins that hard-depend on com.intellij.modules.ultimate"
        // for platform 2026.1; re-check with `printBundledPlugins` after a platform bump. The
        // Community-flavoured base plugins they build on (JavaScript, Spring, Database Tools,
        // Jakarta EE core) still load, so React/Vue/Thymeleaf/Flyway/... are unaffected.
        disabledPlugins.addAll(
            "Refactor-X",
            "com.intellij.LineProfiler",
            "com.intellij.aop",
            "com.intellij.cron",
            "com.intellij.freemarker",
            "com.intellij.velocity",
            "com.intellij.hibernate",
            "com.intellij.persistence",
            "com.intellij.micronaut",
            "com.intellij.quarkus",
            "com.intellij.kubernetes",
            "com.intellij.tailwindcss",
            "com.intellij.tasks.timeTracking",
            "com.intellij.beanValidation",
            "com.intellij.cdi",
            "com.intellij.jsp",
            "com.intellij.javaee.web",
            "com.intellij.javaee.jpa",
            "com.intellij.javaee.extensions",
            "com.intellij.javaee.jakarta.data",
            "com.intellij.javaee.reverseEngineering",
            "com.intellij.javaee.app.servers.integration",
            "com.intellij.spring.mvc",
            "com.intellij.spring.data",
            "com.intellij.spring.cloud",
            "com.intellij.spring.security",
            "com.intellij.spring.messaging",
            "com.intellij.spring.modulith",
            "com.intellij.spring.integration",
            "com.deadlock.scsyntax",
            "com.jetbrains.gateway",
            "com.jetbrains.restWebServices",
            "com.jetbrains.plugins.webDeployment",
            "intellij.debuggerMcp",
            "intellij.nextjs",
            "JavaScriptDebugger",
            "NodeJS",
            "Karma",
            "JBoss",
            "Tomcat",
            "org.intellij.plugins.postcss",
            "org.jetbrains.plugins.less",
            "org.jetbrains.plugins.sass",
            "org.jetbrains.plugins.remote-run",
            "org.jetbrains.plugins.docker.gateway",
            "org.jetbrains.plugins.node-remote-interpreter",
        )
    }

    runIde {
        // The sandbox IDE runs on JBR 25. The platform's bundled Netty calls
        // sun.misc.Unsafe::allocateMemory, which prints a "terminally deprecated method in
        // sun.misc.Unsafe has been called" warning on every launch. The call is inside JetBrains'
        // own Netty, not this plugin, so opt in to the access to silence the noise. Dev-sandbox
        // only - this flag is not part of the built/published plugin.
        jvmArgs("--sun-misc-unsafe-memory-access=allow")

        // JBR prints "[warning][cds] Archived non-system classes are disabled ..." on every
        // launch because the platform sets a custom system class loader. Silence that log tag.
        jvmArgs("-Xlog:cds=off")
    }
}
