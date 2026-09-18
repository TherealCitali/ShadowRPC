import java.time.Instant
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    // AGP 9 ships built-in Kotlin support; do not apply org.jetbrains.kotlin.android here.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

// Discord Application ID: local.properties > env (CI secret) > ShadowRPC's own application.
// The ID is a public OAuth client identifier (it appears in every login URL), not a secret;
// forks should still create their own application and override it.
val discordApplicationId =
    (
        localProperties.getProperty("DISCORD_APPLICATION_ID")
            ?: System.getenv("DISCORD_APPLICATION_ID")
    )?.trim()?.takeIf { it.all(Char::isDigit) && it.isNotEmpty() } ?: "1549707822390976593"
val discordRedirectScheme = "discord-$discordApplicationId"

android {
    namespace = "dev.citali.shadowrpc"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.citali.shadowrpc"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.0"
        val commit = providers.environmentVariable("GITHUB_SHA").orNull
            ?.takeIf { it.matches(Regex("[0-9a-fA-F]{40}")) }?.take(12) ?: "local"
        buildConfigField("String", "BUILD_COMMIT", "\"$commit\"")
        buildConfigField("String", "BUILD_DATE", "\"${Instant.now()}\"")

        buildConfigField("String", "DISCORD_APPLICATION_ID", "\"$discordApplicationId\"")
        buildConfigField("long", "DISCORD_APPLICATION_ID_LONG", "${discordApplicationId}L")
        buildConfigField("String", "DISCORD_REDIRECT_SCHEME", "\"$discordRedirectScheme\"")
        manifestPlaceholders["discordRedirectScheme"] = discordRedirectScheme
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
    }

    packaging {
        resources {
            excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        optIn.add("kotlinx.coroutines.DelicateCoroutinesApi")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation(libs.miuix.ui)
    implementation(libs.core.ktx)
    implementation(libs.splashscreen)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.service)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.timber)
    implementation(libs.material.kolor)
}
