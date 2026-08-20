import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "sb.linux.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "sb.linux.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    // 4.1 CI 构建 armv8a / armv7a / 通用版三个 APK
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    // APK 命名带版本号：LinuxSB-v{versionName}-{abi}.apk（4.1）
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abi = output.getFilter(com.android.build.OutputFile.ABI)
            val abiLabel = when (abi) {
                "arm64-v8a" -> "armv8a"
                "armeabi-v7a" -> "armv7a"
                else -> "universal"
            }
            outputFileName = "LinuxSB-v${variant.versionName}-$abiLabel.apk"
        }
    }

    // 本地签名材料读根目录 keystore.properties（已 gitignore 不入库）；
    // 优先级：-P 参数（CI 用）> 环境变量 > keystore.properties（本地）> 兜底默认值
    val ksProps = rootProject.file("keystore.properties").takeIf { it.exists() }
        ?.inputStream()?.use { Properties().apply { load(it) } }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(
                (project.findProperty("LSB_STORE_FILE") as String?)
                    ?: ksProps?.getProperty("storeFile") ?: "lsb-release.jks"
            )
            storePassword = (project.findProperty("LSB_STORE_PASSWORD") as String?)
                ?: System.getenv("LSB_STORE_PASSWORD")
                ?: ksProps?.getProperty("storePassword") ?: "lsb123456"
            keyAlias = (project.findProperty("LSB_KEY_ALIAS") as String?)
                ?: System.getenv("LSB_KEY_ALIAS")
                ?: ksProps?.getProperty("keyAlias") ?: "lsb"
            keyPassword = (project.findProperty("LSB_KEY_PASSWORD") as String?)
                ?: System.getenv("LSB_KEY_PASSWORD")
                ?: ksProps?.getProperty("keyPassword") ?: "lsb123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // ColorBlendr 同款 HCT 主题引擎（material-color-utilities 的 Kotlin 封装）
    implementation("com.materialkolor:material-kolor:2.0.2")
}
