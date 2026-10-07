package sb.linux.client.data

import android.net.Uri
import sb.linux.client.domain.account.UserLookupRepository

internal class UserByNameService(private val client: LsbClient) : UserLookupRepository {
    override suspend fun resolve(username: String): Long {
        val response = client.get("/user?username=${Uri.encode(username)}")
        val idInUrl = USER_ID_IN_URL.find(response.url)?.groupValues?.get(1)?.toLongOrNull()
        return idInUrl ?: UserAccountHtmlParser.parseUserProfile(response.html).userId
    }

    private companion object {
        val USER_ID_IN_URL = Regex("""/user/(\d+)""")
    }
}
