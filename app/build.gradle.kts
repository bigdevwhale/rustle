import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from environment variables (set by CI from repository secrets).
// Without them the release build falls back to the debug key so it still installs.
// `-Pdemo` builds app.rustle.demo with a lived-in nook (finds, furniture, outings), for screenshots, next to the real install.
val demo = project.hasProperty("demo")

val releaseKeystore = System.getenv("SIGNING_KEYSTORE_PATH")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "app.rustle"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.rustle"
        minSdk = 28
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        buildConfigField("boolean", "DEMO", demo.toString())
        // Screenshots want a still screen: no blinking, no drifting dust.
        buildConfigField("boolean", "SHOTS", project.hasProperty("shots").toString())
        // `-Pevening` (with -Pshots): the nook after seven, with the bed tile, whatever the phone's clock says.
        buildConfigField("boolean", "EVENING", project.hasProperty("evening").toString())
        // `-Plang=en` makes a screenshot build start in that language (old Androids can't switch per app from adb).
        buildConfigField("String", "LANG", "\"" + (project.findProperty("lang") ?: "") + "\"")
        if (demo) applicationIdSuffix = if (project.hasProperty("shots")) ".shots" else ".demo"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Keeps a debug install from ever replacing (or wiping) the real app on the same phone.
            if (!demo) applicationIdSuffix = ".dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        error += "MissingTranslation"
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        disable += "ObsoleteSdkInt"
        warningsAsErrors = false
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
}

// The renderers (`-Prender`) draw screens to app/build/art for a look; they're not tests.
tasks.withType<Test>().configureEach {
    if (!project.hasProperty("render")) exclude("**/Render*")
    systemProperty("render.out", layout.buildDirectory.dir("art").get().asFile.absolutePath)
}
