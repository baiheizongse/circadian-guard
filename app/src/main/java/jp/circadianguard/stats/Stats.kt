package jp.circadianguard.stats

import kotlin.math.sqrt

/** Online statistics helpers for baseline learning and deviation detection. */
object Stats {

    /** Welford running state: mean, sum of squared deviations (M2), and sample count. */
    data class WelfordState(val mean: Float, val m2: Float, val n: Int)

    /** Incorporate one new value using Welford's online algorithm. */
    fun update(state: WelfordState, x: Float): WelfordState {
        val n1 = state.n + 1
        val delta = x - state.mean
        val mean1 = state.mean + delta / n1
        val delta2 = x - mean1
        val m2_1 = state.m2 + delta * delta2
        return WelfordState(mean1, m2_1, n1)
    }

    /** Population standard deviation from a Welford state (0 when n < 2). */
    fun std(state: WelfordState): Float =
        if (state.n < 2) 0f else sqrt((state.m2 / state.n).toDouble()).toFloat()

    /** Median of a list of values (0 for an empty list). */
    fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }
}
