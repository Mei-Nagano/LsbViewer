import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "sb.linux.client"
    compileSdk = 37

    defaultConfig {
        applicationId = "sb.linux.client"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 版本号：CI 构建时由工作流通过 -PappVersionName / -PappVersionCode 覆盖
        // （标签触发取标签名；手动触发取下方默认值）。versionCode 采用
        // major*10000+minor*100+patch 编码（1.0.7 → 10007），随版本名单调递增。
        // 发布成功后工作流会把下一开发版本（patch+1）写回下面两行的默认值。
        // 注意：行尾的数字/字符串字面量被 release.yml 的 sed 匹配，勿加行尾注释
        versionCode = (project.findProperty("appVersionCode") as String?)?.toInt() ?: 10063
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.63"
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
    buildFeatures {
        compose = true
    }

    // JNA AAR 附带 mips/mips64/armeabi 等已废弃 ABI 的 libjnidispatch.so，
    // 仅 universal APK 会被塞入（分包不受影响），排除掉省约 400KB 死重
    packaging {
        jniLibs {
            excludes += listOf("lib/mips/**", "lib/mips64/**", "lib/armeabi/**")
        }
    }
}

// APK 命名带版本号：LinuxSB-v{versionName}-{abi}.apk（4.1）
// AGP 9 移除了旧 Variant API（applicationVariants.all / BaseVariantOutputImpl），
// onAllVariants 也改为 onVariants(selector().all()) 新签名
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.firstOrNull {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier
            val abiLabel = when (abi) {
                "arm64-v8a" -> "armv8a"
                "armeabi-v7a" -> "armv7a"
                else -> "universal"
            }
            val vName = output.versionName.orNull ?: "0.0.0"
            output.outputFileName.set("LinuxSB-v$vName-$abiLabel.apk")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.webkit:webkit:1.12.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    // Cronet（Chromium 网络栈）：QUIC/HTTP3 回退传输层。部分网络对 linux.sb 的
    // TCP TLS 握手做 SNI 阻断（ClientHello 后 RST），QUIC(UDP/443) 不受影响；
    // OkHttp 不支持 HTTP/3，TCP 失败后经 Cronet 重试同一请求。用 embedded 版
    // 自带 native 库，不依赖设备上的 Google Play Services（国产 ROM 常无 GMS）。
    // 固定当前已接入的 embedded 版本，避免依赖用户安装的 Google Play 服务。
    implementation("org.chromium.net:cronet-embedded:143.7445.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // resvg：Rust 实现的 SVG 渲染引擎（uniffi 生成绑定 + JNA 加载 .so），直接把 SVG
    // 头像字节渲染为 PNG。替代 androidsvg / coil-svg：对 DiceBear bottts 等现代 SVG
    // 特性（mask-type、裸 href、缺 width/height、SVG2 语法）原生支持，无需清洗兼容。
    // 库 manifest 声明 minSdk 28 但未实际使用 28+ API，已在 AndroidManifest 用
    // tools:overrideLibrary 覆盖以兼容项目 API 26 基线
    implementation("io.github.dweb-channel:lib_resvg_render-android:1.2.1")

    // ColorBlendr 同款 HCT 主题引擎（material-color-utilities 的 Kotlin 封装）
    implementation("com.materialkolor:material-kolor:2.0.2")

    // 液态玻璃（23）：Kyant0 Backdrop 2.0.0 高级效果版——lens 边缘折射支持
    // depthEffect 深度折射与 chromaticAberration 色散，drawBackdrop 默认附带
    // highlight 高光描边与 shadow 投影。其 Kotlin 元数据 2.3.0 可被 AGP 9.2.1
    // 内置 Kotlin 2.2.10 读取（n+1 规则）；传递依赖 io.github.kyant0:shapes
    // 提供 RoundedRectangularShape；Compose 依赖与 BOM 2026.06.01（1.11）对齐，
    // 无需再 exclude
    implementation("io.github.kyant0:backdrop:2.0.0")

    testImplementation("org.jsoup:jsoup:1.18.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
