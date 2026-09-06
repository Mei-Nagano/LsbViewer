package sb.linux.client.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomDestinationTest {
    @Test
    fun canonicalizesRootAliasesAndArguments() {
        assertEquals(BottomDestination.TOPIC_COLLECTIONS, BottomDestination.canonical("topicCollectionsRoot?tab=mine"))
        assertEquals(BottomDestination.DIRECT_MESSAGES, BottomDestination.canonical("directMessagesRoot"))
        assertEquals(BottomDestination.ME, BottomDestination.canonical("me"))
    }

    @Test
    fun mapsTabNamesToGraphDestinations() {
        assertEquals("topicCollectionsRoot", BottomDestination.destination("topicCollections"))
        assertEquals("directMessagesRoot", BottomDestination.destination("directMessages"))
        assertEquals("me", BottomDestination.destination("me"))
    }
}
