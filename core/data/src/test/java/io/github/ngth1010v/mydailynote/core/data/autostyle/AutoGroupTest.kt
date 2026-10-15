package io.github.ngth1010v.mydailynote.core.data.autostyle

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class AutoGroupTest {
    /** Texts are unit vectors at an angle (degrees): similarity = cos(angle difference). */
    private val angles = mapOf(
        "drink coffee" to 0.0, "latte" to 20.0, "Food & drink" to 40.0, "Drinks" to 10.0,
        "run" to 180.0, "Sport" to 170.0, "pay bills" to 90.0, "Other" to 5.0, "misc" to 95.0, "Money" to 270.0,
    )
    private fun embed(t: String): FloatArray {
        val a = Math.toRadians(angles.getValue(t))
        return floatArrayOf(cos(a).toFloat(), sin(a).toFloat())
    }

    private val icons = mapOf("coffee" to 0, "run" to 2, "money" to 6, "star" to 10)
    private val router = AutoGroup(::embed, icons, otherCategory = 10)
    private val other = GroupCandidate(1, "", isDefault = true, category = null, actions = emptyList())

    @Test fun relatedActionJoinsItsGroup() {
        val groups = listOf(other, GroupCandidate(2, "Drinks", false, null, listOf("drink coffee")))
        assertEquals(Routing.Existing(2), router.route("latte", "coffee", groups, create = true))
    }

    @Test fun groupNameAloneCanMatch() {
        val groups = listOf(other, GroupCandidate(3, "Sport", false, null, emptyList()))
        assertEquals(Routing.Existing(3), router.route("run", "run", groups, create = true))
    }

    @Test fun builtInGroupNameIsIgnoredButItsActionsCount() {
        // "Other" (5°) is close to "drink coffee" by angle, but the built-in group's name means nothing.
        val named = GroupCandidate(1, "Other", isDefault = true, category = null, actions = emptyList())
        assertEquals(Routing.Create(0), router.route("drink coffee", "coffee", listOf(named), create = true))
        val holding = GroupCandidate(1, "", isDefault = true, category = null, actions = listOf("latte"))
        assertEquals(Routing.Existing(1), router.route("drink coffee", "coffee", listOf(holding), create = true))
    }

    @Test fun unmatchedGoesToTheGroupMadeForItsCategory() {
        val groups = listOf(other, GroupCandidate(4, "Money", false, category = 6, actions = listOf("run")))
        assertEquals(Routing.Existing(4), router.route("pay bills", "money", groups, create = true))
    }

    @Test fun unmatchedCreatesACategoryGroupOnlyWhenAllowed() {
        val groups = listOf(other, GroupCandidate(2, "Sport", false, null, listOf("run")))
        assertEquals(Routing.Create(6), router.route("pay bills", "money", groups, create = true))
        assertEquals(Routing.Default, router.route("pay bills", "money", groups, create = false))
    }

    @Test fun otherCategoryNeverGetsItsOwnGroup() {
        assertEquals(Routing.Default, router.route("misc", "star", listOf(other), create = true))
        assertEquals(Routing.Default, router.route("misc", null, listOf(other), create = true))
    }
}
