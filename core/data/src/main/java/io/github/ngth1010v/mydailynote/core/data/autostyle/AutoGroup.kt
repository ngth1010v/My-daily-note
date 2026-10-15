package io.github.ngth1010v.mydailynote.core.data.autostyle

/** A group as [AutoGroup] sees it: its name and its actions' texts. */
class GroupCandidate(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val category: Int?,
    val actions: List<String>,
)

sealed interface Routing {
    data class Existing(val groupId: Long) : Routing
    /** No group fits: make one for icon category [category]. */
    data class Create(val category: Int) : Routing
    /** Into the built-in "Other" group. */
    data object Default : Routing
}

/**
 * Picks the group a new action belongs in:
 * - the group whose name or one of whose actions is most related to it (at least [AutoStyle.RELATED]);
 * - else the group auto-created earlier for the category of the action's icon;
 * - else, when [route]'s `create` is set, a new group for that category (not for the "Other" category);
 * - else the built-in group.
 */
class AutoGroup(
    private val embed: (String) -> FloatArray,
    /** Icon key to the index of its category (the icon picker's group). */
    private val iconCategory: Map<String, Int>,
    /** Category of miscellaneous icons: never gets a group of its own. */
    private val otherCategory: Int,
) {
    private val cache = HashMap<String, FloatArray>()

    @Synchronized
    fun route(text: String, icon: String?, groups: List<GroupCandidate>, create: Boolean): Routing {
        val v = vector(text)
        val best = groups
            .map { g -> g to similarity(v, g) }
            .maxByOrNull { it.second }
        if (best != null && best.second >= AutoStyle.RELATED) return Routing.Existing(best.first.id)

        val category = icon?.let(iconCategory::get)
        if (category == null || category == otherCategory) return Routing.Default
        groups.firstOrNull { it.category == category }?.let { return Routing.Existing(it.id) }
        return if (create) Routing.Create(category) else Routing.Default
    }

    private fun similarity(v: FloatArray, group: GroupCandidate): Float {
        // The built-in group's name ("Other") says nothing about what belongs in it.
        val name = if (group.isDefault || group.name.isBlank()) -1f else dot(v, vector(group.name))
        val member = group.actions.maxOfOrNull { dot(v, vector(it)) } ?: -1f
        return maxOf(name, member)
    }

    private fun vector(text: String) = cache.getOrPut(text) { embed(text) }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in a.indices) s += a[i] * b[i]
        return s
    }
}
