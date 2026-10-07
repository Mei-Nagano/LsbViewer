package sb.linux.client.ui

import android.util.LruCache

/** Bounded parsed-body cache used while topic rows leave and re-enter the viewport. */
internal val htmlBlockCache = LruCache<String, List<ContentBlock>>(48)
