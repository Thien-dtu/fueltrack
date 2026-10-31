package com.example.core.util

import com.example.core.model.FuelEntry

/**
 * Distance between consecutive fill-ups, resolved chronologically.
 *
 * Why a separate helper: both the dashboard and the history list display
 * "distance since the previous fill-up", but they order their lists by odometer
 * (newest reading first). Pairing neighbours in that display order silently
 * assumes odometer order equals time order, which stops being true as soon as
 * the odometer is replaced or rolled back: the pre-reset entry carries the larger
 * reading, so it becomes "the previous fill-up" for everything after the reset.
 *
 * Resolution is by timestamp, then id, and a leg whose odometer goes backwards
 * yields null instead of a bogus negative or huge distance.
 */
object FuelDistanceCalculator {

    /**
     * Maps each entry id to the distance driven since the chronologically previous
     * entry. The earliest entry maps to null. A leg that spans an odometer reset
     * also maps to null, because that distance is not measurable.
     */
    fun distanceByEntryId(entries: List<FuelEntry>): Map<Long, Long?> {
        if (entries.isEmpty()) return emptyMap()

        val chronological = entries.sortedWith(compareBy<FuelEntry> { it.timestamp }.thenBy { it.id })
        val result = LinkedHashMap<Long, Long?>(chronological.size)
        var previousOdometer: Long? = null

        for (entry in chronological) {
            val previous = previousOdometer
            result[entry.id] = if (previous == null) null else FuelCalculator.calculateDistance(previous, entry.odometerMeters)
            previousOdometer = entry.odometerMeters
        }

        return result
    }
}
