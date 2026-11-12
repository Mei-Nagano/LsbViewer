package sb.linux.client.ui.screens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.theme.colorSeed
import sb.linux.client.ui.theme.colorToKey
import sb.linux.client.ui.theme.isCustomColorKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThemeSettingsScreen(session: Session, nav: NavHostController) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var pickingRole by remember { mutableStateOf<PreviewRole?>(null) }
    var pickingElement by remember { mutableStateOf<String?>(null) }
    val dark = isPreviewDark(session.themeMode)
    // 当前生效的整体配色方案（直接取自 LsbTheme 应用的主题，已含元素级覆盖）
    val currentScheme = MaterialTheme.colorScheme

    // ---------- 自定义字体（24）：导入器与预览字体 ----------
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customFontFamily = remember(session.customFontName) {
        session.preferences.customFontFile.takeIf { it.exists() }?.let { f ->
            runCatching { FontFamily(Font(f)) }.getOrNull()
        }
    }
    var backgroundImportBusy by remember { mutableStateOf(false) }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            backgroundImportBusy = true
            try { session.preferences.importBackgroundImage(uri); session.showToast("背景图片已保存到应用内") }
            catch (e: Exception) { session.showToast(e.message ?: "导入背景失败") }
            finally { backgroundImportBusy = false }
        }
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                if (session.preferences.saveCustomFont(uri, queryDisplayName(context, uri))) {
                    session.showToast("已应用自定义字体")
                } else {
                    session.showToast("导入失败：不是有效的字体文件")
                }
            }
        }
    }

    // 自定义主题色（种子色）取色对话框
    if (showColorPicker) {
        ColorPickerDialog(
            title = "自定义主题色",
            initial = colorSeed(
                if (isCustomColorKey(session.themeColorKey)) session.themeColorKey else "default"
            ),
            presets = session.themeCustomColors,
            onDismiss = { showColorPicker = false },
            onSavePreset = { c -> session.preferences.addThemeCustomColor(colorToKey(c)) },
            onRemovePreset = { k ->
                session.preferences.removeThemeCustomColor(k)
                session.showToast("已移除该预设颜色")
            },
            onPick = { c ->
                session.preferences.saveTheme(session.themeMode, false, colorToKey(c))
                session.preferences.addThemeCustomColor(colorToKey(c))
                showColorPicker = false
            }
        )
    }
    if (showBackgroundPicker) {
        ColorPickerDialog(
            title = "自定义页面背景",
            initial = session.themeBackground ?: currentScheme.background,
            presets = session.themeCustomColors,
            onDismiss = { showBackgroundPicker = false },
            onReset = {
                session.preferences.saveThemeBackground(null)
                showBackgroundPicker = false
            },
            onPick = { color ->
                session.preferences.saveThemeBackground(color)
                showBackgroundPicker = false
            },
        )
    }
    // 配色角色覆盖取色对话框
    pickingRole?.let { role ->
        ColorPickerDialog(
            title = "自定义${role.label}",
            initial = previewRoleColor(currentScheme, role),
            presets = session.themeCustomColors,
            onDismiss = { pickingRole = null },
            onSavePreset = { c -> session.preferences.addThemeCustomColor(colorToKey(c)) },
            onRemovePreset = { k ->
                session.preferences.removeThemeCustomColor(k)
                session.showToast("已移除该预设颜色")
            },
            onReset = {
                when (role) {
                    PreviewRole.PRIMARY -> session.preferences.saveThemePrimary(null)
                    PreviewRole.SECONDARY -> session.preferences.saveThemeSecondary(null)
                    PreviewRole.TERTIARY -> session.preferences.saveThemeTertiary(null)
                }
                pickingRole = null
            },
            onPick = { c ->
                when (role) {
                    PreviewRole.PRIMARY -> session.preferences.saveThemePrimary(c)
                    PreviewRole.SECONDARY -> session.preferences.saveThemeSecondary(c)
                    PreviewRole.TERTIARY -> session.preferences.saveThemeTertiary(c)
                }
                pickingRole = null
            }
        )
    }
    // 帖子卡片元素覆盖取色对话框（预览卡点按元素触发）
    pickingElement?.let { key ->
        val el = CardElements.firstOrNull { it.key == key }
        if (el != null) {
            ColorPickerDialog(
                title = "自定义${el.label}",
                initial = cardElementColor(session, currentScheme, key),
                presets = session.themeCustomColors,
                onDismiss = { pickingElement = null },
                onSavePreset = { c -> session.preferences.addThemeCustomColor(colorToKey(c)) },
                onRemovePreset = { k ->
                    session.preferences.removeThemeCustomColor(k)
                    session.showToast("已移除该预设颜色")
                },
                onReset = {
                    session.preferences.saveCardColor(key, null)
                    pickingElement = null
                },
                onPick = { c ->
                    session.preferences.saveCardColor(key, c)
                    pickingElement = null
                }
            )
        }
    }

    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = "外观与布局",
        onBack = { nav.popBackStack() },
    ) { pad ->
        val spacing = LocalAppSpacing.current
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap)
        ) {
            item {
                Column {
                    GroupLabel("显示模式")
                    DayNightToggle(session.themeMode) { session.preferences.saveTheme(it, session.dynamicColor) }
                }
            }
            // 原三级子页（主题与配色 / 页面背景 / 字体与文字 / 导航与布局）直接在此页展示
            themeBackgroundPalette(
                section = "colors",
                session = session,
                currentScheme = currentScheme,
                backgroundImportBusy = backgroundImportBusy,
                onOpenBackgroundPicker = { showBackgroundPicker = true },
                onSelectBackgroundImage = { backgroundPicker.launch("image/*") },
                onPickRole = { pickingRole = it },
                onPickElement = { pickingElement = it },
                onOpenThemeColorPicker = { showColorPicker = true },
            )
            themeStyleSettings(session, dark)
            themeBackgroundPalette(
                section = "background",
                session = session,
                currentScheme = currentScheme,
                backgroundImportBusy = backgroundImportBusy,
                onOpenBackgroundPicker = { showBackgroundPicker = true },
                onSelectBackgroundImage = { backgroundPicker.launch("image/*") },
                onPickRole = { pickingRole = it },
                onPickElement = { pickingElement = it },
                onOpenThemeColorPicker = { showColorPicker = true },
            )
            themeFontSettings(
                session = session,
                customFontFamily = customFontFamily,
                onChooseCustomFont = {
                    fontPicker.launch(
                        arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"),
                    )
                },
            )
            themeLayoutSettings(session)
            item { SettingsResetRow("重置外观设置") { session.preferences.resetThemeSettings() } }
        }
    }
}

// ==================== 常规设置（3.20） ====================

/** 当前应用版本名（包管理器读取，避免依赖 BuildConfig 配置） */
