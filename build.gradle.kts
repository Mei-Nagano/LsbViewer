plugins {
    id("com.android.application") version "9.2.1" apply false
    // AGP 9 内置 Kotlin 编译（9.2.1 内嵌 Kotlin 2.2.10，无需再应用
    // org.jetbrains.kotlin.android）；Compose 编译器插件版本必须与内置
    // Kotlin 版本一致，故用 2.2.10
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
