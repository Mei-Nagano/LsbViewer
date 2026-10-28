package sb.linux.client.ui.feature.search

import sb.linux.client.domain.search.SearchScope
import sb.linux.client.domain.search.SearchSort

internal fun SearchScope.displayLabel(): String = when (this) {
    SearchScope.ALL -> "全部内容"
    SearchScope.TITLE -> "标题"
    SearchScope.BODY -> "主题正文"
    SearchScope.REPLY -> "回帖"
    SearchScope.USER -> "用户"
}

internal fun SearchSort.displayLabel(): String = when (this) {
    SearchSort.RELEVANCE -> "相关性"
    SearchSort.LATEST -> "最新回复"
    SearchSort.CREATED -> "最新发布"
    SearchSort.REPLIES -> "回复最多"
    SearchSort.VIEWS -> "浏览最多"
}
