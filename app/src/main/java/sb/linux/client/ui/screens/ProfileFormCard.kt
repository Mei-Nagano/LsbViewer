package sb.linux.client.ui.screens
import sb.linux.client.common.network.Endpoints

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.domain.profile.AvatarPickerData
import sb.linux.client.domain.profile.ProfileSettingsForm
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
import java.io.File

@Composable
internal fun ProfileFormCard(
    form: ProfileSettingsForm,
    busy: Boolean,
    context: android.content.Context,
    session: Session,
    avatarPicker: AvatarPickerData?,
    onSubmit: (Map<String, String>, File?, String, () -> Unit) -> Unit,
) {
    // 各字段当前值（初始为解析值）
    val values = remember(form) { mutableStateMapOf(*form.fields.map { it.name to it.value }.toTypedArray()) }
    // 密码组默认折叠：点击"修改密码"才显示输入框
    var pwdExpanded by remember(form) { mutableStateOf(false) }
    // 头像上传：选中的临时文件
    var pickedFile by remember(form) { mutableStateOf<File?>(null) }
    var pickedMime by remember(form) { mutableStateOf("image/jpeg") }
    var localBusy by remember(form) { mutableStateOf(false) }
    // 源站预置头像：选中的「风格_编号」（写回表单字段随表单提交，与源站行为一致）
    var pickedPreset by remember(form) { mutableStateOf<String?>(null) }
    // 选择器只挂在含头像字段的表单上
    val picker = avatarPicker?.takeIf { p ->
        form.fields.any { it.name == p.seedField || it.name == p.styleField }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mime) {
                    "image/png" -> "png"; "image/webp" -> "webp"; "image/gif" -> "gif"; else -> "jpg"
                }
                val f = File(context.cacheDir, "avatar_upload_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
                pickedFile = f; pickedMime = mime; pickedPreset = null
            } catch (_: Exception) {
                session.showToast("读取图片失败")
            }
        }
    }

    // 预置头像 URL：基址来自服务端 data-avatar-base；无基址时回退 DiceBear CDN（源站远程模式）
    fun presetUrl(style: String, n: Int): String = when {
        picker!!.base.startsWith("http") -> picker.base + "${style}_$n.svg"
        picker.base.isNotBlank() -> Endpoints.abs(picker.base + "${style}_$n.svg")
        else -> "https://api.dicebear.com/10.x/$style/svg?seed=$n"
    }

    fun pickPreset(style: String, n: Int) {
        values[picker!!.seedField] = n.toString()
        values[picker.styleField] = style
        pickedPreset = "${style}_$n"
        session.showToast("已选择预置头像 $pickedPreset")
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!(form.title.contains("账号") && form.submitText.contains("退出"))) {
                Text(
                    form.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 头像上传（file 字段）
            if (form.hasFile) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !localBusy) {
                        Icon(Icons.Filled.Upload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (pickedFile == null) "选择头像图片" else "重新选择")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
                            pickedFile != null -> "已选择：${pickedFile?.name}"
                            pickedPreset != null -> "已选择预置头像 $pickedPreset"
                            else -> "未选择时提交不修改头像"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 源站预置头像选择器：风格与编号均来自服务端解析（.avatar-picker），选中写回表单字段提交
            if (picker != null && picker.styles.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "或选择源站预置头像",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 风格切换（dylan / bottts-neutral…选项由服务端下发）
                val currentStyle = values[picker.styleField] ?: picker.currentStyle
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(picker.styles) { (value, label) ->
                        FilterChip(
                            selected = currentStyle == value,
                            onClick = { values[picker.styleField] = value },
                            label = { Text(label.ifBlank { value }) },
                        )
                    }
                }
                // 当前风格的 48 个预置头像
                val currentSeed = values[picker.seedField] ?: picker.currentSeed
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items((1..48).toList()) { n ->
                        val selected = currentSeed == n.toString()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(enabled = !localBusy) { pickPreset(currentStyle, n) }
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                AsyncImage(
                                    model = presetUrl(currentStyle, n),
                                    contentDescription = "预置头像 $n",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Text(
                                "$n",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 普通字段（text/email/number/select/textarea/checkbox…）；头像字段已由上方选择器承载，不再重复渲染
            form.visibles
                .filterNot { f -> picker != null && (f.name == picker.seedField || f.name == picker.styleField) }
                .forEach { f ->
                    ProfileFieldInput(f, values[f.name] ?: f.value) { values[f.name] = it }
                }

            // 密码字段：点击"修改密码"展开
            if (form.passwords.isNotEmpty()) {
                if (!pwdExpanded) {
                    TextButton(onClick = { pwdExpanded = true }) { Text("修改密码") }
                } else {
                    form.passwords.forEach { f ->
                        OutlinedTextField(
                            value = values[f.name] ?: "",
                            onValueChange = { values[f.name] = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(f.label) },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    TextButton(onClick = {
                        form.passwords.forEach { values[it.name] = "" }
                        pwdExpanded = false
                    }) { Text("收起") }
                }
            }

            Button(
                onClick = {
                    localBusy = true
                    // 全量字段（含 hidden）：预置头像选择器会把新值写入 avatar_style/avatar_seed
                    val payload = form.fields.associate { it.name to (values[it.name] ?: it.value) }
                    onSubmit(payload, pickedFile, pickedMime) { localBusy = false; pickedFile = null; pickedPreset = null }
                },
                enabled = !busy && !localBusy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (localBusy) "提交中…" else form.submitText) }
        }
    }
}

