import com.android.build.gradle.internal.tasks.CompileArtProfileTask
import com.android.build.gradle.internal.tasks.ExpandArtProfileWildcardsTask
import com.android.build.gradle.internal.tasks.MergeArtProfileTask
import com.android.build.gradle.tasks.PackageApplication
import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.internal.provider.AbstractProperty
import org.gradle.api.internal.provider.Providers
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    id("com.android.application") version "8.7.3"
    kotlin("android") version "2.1.0"
}

android {
    namespace = "nya.kitsunyan.foxydroid"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "nya.kitsunyan.foxydroid"
        minSdk = 21
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
        setProperty("archivesBaseName", "${applicationId}-${versionName}")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard.pro")
            signingConfig = signingConfigs.createSigningConfigFromEnv()
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        all {
            // remove META-INF/version-control-info.textproto
            @Suppress("UnstableApiUsage")
            vcsInfo.include = false
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "/META-INF/androidx/**",
                "/kotlin/**",
                "/DebugProbesKt.bin",
                "/kotlin-tooling-metadata.json"
            )
        }
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

// remove META-INF/com/android/build/gradle/app-metadata.properties
tasks.withType<PackageApplication> {
    val valueField =
        AbstractProperty::class.java.declaredFields.find { it.name == "value" } ?: run {
            println("class AbstractProperty field value not found, something could have gone wrong")
            return@withType
        }
    valueField.isAccessible = true
    doFirst {
        valueField.set(appMetadata, Providers.notDefined<RegularFile>())
        allInputFilesWithNameOnlyPathSensitivity.removeAll { true }
    }
}

// remove assets/dexopt/baseline.prof{,m} (baseline profile)
tasks.withType<MergeArtProfileTask> { enabled = false }
tasks.withType<ExpandArtProfileWildcardsTask> { enabled = false }
tasks.withType<CompileArtProfileTask> { enabled = false }

dependencies {
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("io.reactivex.rxjava3:rxjava:3.1.10")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.squareup.picasso:picasso:2.71828")
}

fun env(name: String): String? = System.getenv(name)

private var signKeyTempFile: File? = null

fun NamedDomainObjectContainer<SigningConfig>.createSigningConfigFromEnv(): SigningConfig? {
    var signKeyFile: File? = null
    env("SIGN_KEY_FILE")?.let {
        val file = File(it)
        if (file.exists()) {
            signKeyFile = file
        }
    }
    @OptIn(ExperimentalEncodingApi::class)
    env("SIGN_KEY_BASE64")?.let {
        if (signKeyTempFile?.exists() == true) {
            signKeyFile = signKeyTempFile
        } else {
            val buildDir = layout.buildDirectory.asFile.get()
            buildDir.mkdirs()
            val file = File.createTempFile("sign-", ".ks", buildDir)
            try {
                file.writeBytes(Base64.decode(it))
                file.deleteOnExit()
                signKeyFile = file
                signKeyTempFile = file
            } catch (e: Exception) {
                file.delete()
            }
        }
    }
    signKeyFile ?: return null
    val name = "release"
    return findByName(name) ?: create(name) {
        storeFile = signKeyFile
        storePassword = env("SIGN_KEY_PWD")
        keyAlias = env("SIGN_KEY_ALIAS")
        keyPassword = env("SIGN_KEY_PWD")
    }
}
