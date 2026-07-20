package com.csust.pocket.feature.common.compose_ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DormBindingSelectionTest {
    @Test
    fun openingExistingBindingKeepsCurrentRoom() {
        assertFalse(
            shouldClearRoomForBindingChange(
                previousContext = "云塘校区" to "至诚轩1栋B区",
                nextContext = "云塘校区" to "至诚轩1栋B区"
            )
        )
    }

    @Test
    fun changingDormClearsPreviousRoom() {
        assertTrue(
            shouldClearRoomForBindingChange(
                previousContext = "云塘校区" to "至诚轩1栋B区",
                nextContext = "云塘校区" to "至诚轩2栋"
            )
        )
    }
}
