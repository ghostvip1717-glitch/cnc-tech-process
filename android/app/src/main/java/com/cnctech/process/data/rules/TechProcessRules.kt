package com.cnctech.process.data.rules

object TechProcessRules {
    val ROMAN_ORDERS = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

    fun setupOrderLabel(order: Int): String {
        return if (order in ROMAN_ORDERS.indices) {
            ROMAN_ORDERS[order]
        } else {
            (order + 1).toString()
        }
    }

    fun nextOrder(existingOrders: Collection<Int>): Int {
        if (existingOrders.isEmpty()) return 0
        return (existingOrders.maxOrNull() ?: -1) + 1
    }

    /**
     * Validate that [orderedIds] is a permutation of [existingIds].
     * Returns orderById map on success, null if rejected.
     */
    fun validateReorderIds(orderedIds: List<Long>, existingIds: List<Long>): Map<Long, Int>? {
        if (orderedIds.size != existingIds.size) return null
        val existing = existingIds.toSet()
        if (existing.size != existingIds.size) return null
        val seen = mutableSetOf<Long>()
        val orderById = LinkedHashMap<Long, Int>()
        for ((index, id) in orderedIds.withIndex()) {
            if (id !in existing || !seen.add(id)) return null
            orderById[id] = index
        }
        return orderById
    }
}
