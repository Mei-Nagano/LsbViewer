package sb.linux.client.domain.update

data class UpdateInfo(
    val tag: String,
    val name: String,
    val url: String,
    val notes: String,
)

interface UpdateRepository {
    suspend fun latestRelease(): UpdateInfo?
}

object UpdateProjectLinks {
    const val REPO = "Mei-Nagano/LsbViewer"
    const val DEVELOPER_URL = "https://github.com/Mei-Nagano"
    const val PROJECT_URL = "https://github.com/$REPO"
    const val RELEASES_URL = "$PROJECT_URL/releases"
    const val LICENSE_URL = "$PROJECT_URL/blob/main/LICENSE"
}

fun isNewerVersion(latest: String, current: String): Boolean {
    fun normalize(value: String) = value.trim()
        .removePrefix("v").removePrefix("V")
        .substringBefore("-").substringBefore("+")
        .split(".").mapNotNull { it.toIntOrNull() }

    val latestParts = normalize(latest)
    val currentParts = normalize(current)
    if (latestParts.isEmpty() || currentParts.isEmpty()) return false
    for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latestPart = latestParts.getOrNull(index) ?: 0
        val currentPart = currentParts.getOrNull(index) ?: 0
        if (latestPart != currentPart) return latestPart > currentPart
    }
    return false
}
