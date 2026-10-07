package sb.linux.client.common.ui

/** Optional UI cache invalidations requested by app-level data cleanup. */
fun interface SessionUiInvalidation {
    fun clearBodyImageLayoutCache()
}
