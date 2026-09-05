package com.bsdevs.babycare.presentation.measurement

/**
 * Data points for UK-WHO Child Growth Standards (0-2 years).
 * These charts use 9 centile lines: 0.4th, 2nd, 9th, 25th, 50th, 75th, 91st, 98th, 99.6th.
 */
object WhoGrowthData {

    data class CentilePoint(
        val month: Int,
        val values: List<Double>
    )

    // --- BOYS DATA ---

    // Boys Weight-for-age (kg) - Percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
    val weightForAgeBoys = listOf(
        CentilePoint(0, listOf(2.17, 2.50, 2.83, 3.16, 3.50, 3.84, 4.17, 4.50, 4.84)),
        CentilePoint(2, listOf(3.95, 4.30, 4.70, 5.12, 5.56, 6.05, 6.55, 7.20, 7.65)),
        CentilePoint(4, listOf(5.15, 5.50, 6.00, 6.50, 7.00, 7.55, 8.15, 8.75, 9.40)),
        CentilePoint(6, listOf(5.90, 6.35, 6.85, 7.40, 7.94, 8.55, 9.20, 9.85, 10.60)),
        CentilePoint(8, listOf(6.45, 6.90, 7.45, 8.00, 8.60, 9.26, 9.95, 10.65, 11.45)),
        CentilePoint(10, listOf(6.85, 7.30, 7.90, 8.50, 9.20, 9.80, 10.60, 11.45, 12.20)),
        CentilePoint(12, listOf(7.20, 7.70, 8.30, 8.95, 9.65, 10.40, 11.20, 11.95, 12.90)),
        CentilePoint(15, listOf(7.85, 8.40, 9.10, 9.75, 10.50, 11.40, 12.20, 13.20, 14.05)),
        CentilePoint(18, listOf(8.10, 8.70, 9.40, 10.10, 10.95, 11.80, 12.70, 13.70, 14.70)),
        CentilePoint(21, listOf(8.40, 9.00, 9.75, 10.50, 11.35, 12.30, 13.20, 14.15, 15.25)),
        CentilePoint(24, listOf(9.00, 9.70, 10.40, 11.30, 12.10, 13.10, 14.20, 15.30, 16.40))
    )

    // Boys Length-for-age (cm) - Percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
    val lengthForAgeBoys = listOf(
        CentilePoint(0, listOf(44.8, 46.1, 47.9, 49.5, 51.0, 52.5, 54.1, 55.9, 57.1)),
        CentilePoint(3, listOf(53.5, 55.0, 57.0, 58.5, 60.0, 61.5, 63.0, 64.5, 66.0)),
        CentilePoint(6, listOf(62.0, 63.0, 64.8, 66.2, 67.6, 69.1, 70.5, 72.0, 73.3)),
        CentilePoint(9, listOf(66.0, 67.5, 69.5, 71.0, 72.5, 74.0, 75.5, 77.0, 78.5)),
        CentilePoint(12, listOf(69.5, 70.8, 72.5, 74.1, 75.8, 77.3, 78.9, 80.5, 82.0)),
        CentilePoint(15, listOf(72.5, 74.0, 76.0, 77.5, 79.5, 81.0, 82.5, 84.5, 86.0)),
        CentilePoint(18, listOf(75.0, 76.8, 78.6, 80.4, 82.2, 84.1, 85.8, 87.8, 89.5)),
        CentilePoint(21, listOf(77.5, 79.5, 81.5, 83.0, 85.0, 87.0, 89.0, 91.0, 93.0)),
        CentilePoint(24, listOf(79.0, 81.0, 83.0, 85.1, 87.1, 89.2, 91.2, 93.5, 95.3))
    )

    // Boys Head circumference-for-age (cm) - Percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
    val headCircumferenceForAgeBoys = listOf(
        CentilePoint(0, listOf(31.5, 32.5, 33.5, 34.5, 35.3, 36.3, 37.2, 38.2, 39.0)),
        CentilePoint(3, listOf(36.5, 37.5, 38.5, 39.5, 40.5, 41.5, 42.5, 43.5, 44.5)),
        CentilePoint(6, listOf(40.5, 41.3, 42.1, 42.8, 43.6, 44.4, 45.1, 45.9, 46.7)),
        CentilePoint(9, listOf(42.0, 42.8, 43.7, 44.5, 45.3, 46.2, 47.0, 47.8, 48.6)),
        CentilePoint(12, listOf(43.1, 44.0, 44.9, 45.7, 46.5, 47.4, 48.2, 49.0, 49.9)),
        CentilePoint(15, listOf(44.0, 44.8, 45.7, 46.5, 47.3, 48.2, 49.0, 49.8, 50.7)),
        CentilePoint(18, listOf(44.5, 45.3, 46.2, 47.0, 47.9, 48.8, 49.7, 50.5, 51.4)),
        CentilePoint(21, listOf(44.8, 45.7, 46.5, 47.4, 48.3, 49.2, 50.1, 51.0, 51.9)),
        CentilePoint(24, listOf(45.1, 46.0, 46.9, 47.8, 48.7, 49.6, 50.5, 51.4, 52.3))
    )

    // --- GIRLS DATA ---

    // Girls Weight-for-age (kg) - Percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
    val weightForAgeGirls = listOf(
        CentilePoint(0, listOf(2.10, 2.40, 2.70, 3.00, 3.40, 3.70, 4.00, 4.30, 4.60)),
        CentilePoint(2, listOf(3.60, 3.90, 4.30, 4.70, 5.10, 5.60, 6.10, 6.60, 7.20)),
        CentilePoint(4, listOf(4.60, 5.00, 5.40, 5.90, 6.40, 7.00, 7.60, 8.20, 8.90)),
        CentilePoint(6, listOf(5.30, 5.70, 6.20, 6.70, 7.30, 7.90, 8.60, 9.30, 10.10)),
        CentilePoint(8, listOf(5.80, 6.20, 6.70, 7.30, 7.90, 8.60, 9.40, 10.20, 11.00)),
        CentilePoint(10, listOf(6.20, 6.60, 7.20, 7.80, 8.50, 9.20, 10.00, 11.00, 11.80)),
        CentilePoint(12, listOf(6.50, 7.00, 7.60, 8.30, 9.00, 9.70, 10.60, 11.50, 12.50)),
        CentilePoint(15, listOf(7.00, 7.60, 8.20, 8.80, 9.60, 10.50, 11.40, 12.40, 13.50)),
        CentilePoint(18, listOf(7.50, 8.00, 8.70, 9.40, 10.20, 11.10, 12.10, 13.20, 14.40)),
        CentilePoint(21, listOf(7.90, 8.50, 9.20, 10.00, 10.90, 11.80, 12.80, 14.00, 15.30)),
        CentilePoint(24, listOf(8.30, 9.00, 9.80, 10.60, 11.50, 12.50, 13.60, 14.90, 16.20))
    )

    // Girls Length-for-age (cm) - Percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
    val lengthForAgeGirls = listOf(
        CentilePoint(0, listOf(44.7, 46.0, 47.3, 48.6, 50.0, 51.3, 52.7, 54.0, 55.3)),
        CentilePoint(2, listOf(51.6, 53.0, 54.3, 55.7, 57.1, 58.5, 59.8, 61.1, 62.5)),
        CentilePoint(4, listOf(56.3, 57.5, 59.2, 60.6, 62.1, 63.5, 65.0, 66.5, 67.9)),
        CentilePoint(6, listOf(59.7, 61.0, 62.7, 64.2, 65.7, 67.3, 68.8, 70.4, 72.0)),
        CentilePoint(8, listOf(62.5, 64.0, 65.6, 67.1, 68.8, 70.3, 71.9, 73.5, 75.0)),
        CentilePoint(10, listOf(65.0, 66.5, 68.2, 69.8, 71.5, 73.2, 74.8, 76.4, 78.0)),
        CentilePoint(12, listOf(67.2, 68.9, 70.8, 72.5, 74.0, 75.8, 77.8, 79.5, 81.2)),
        CentilePoint(15, listOf(70.3, 72.0, 73.8, 75.7, 77.5, 79.3, 81.2, 83.0, 84.7)),
        CentilePoint(18, listOf(73.0, 75.0, 76.8, 78.7, 80.7, 82.7, 84.6, 86.6, 88.5)),
        CentilePoint(21, listOf(75.5, 77.4, 79.5, 81.6, 83.6, 85.7, 87.7, 89.8, 91.7)),
        CentilePoint(24, listOf(77.2, 79.0, 81.4, 83.5, 85.7, 87.9, 90.0, 92.5, 94.4))
    )

    // Girls Head circumference-for-age (cm) - Percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
    val headCircumferenceForAgeGirls = listOf(
        CentilePoint(0, listOf(30.5, 31.5, 32.5, 33.1, 33.9, 34.7, 35.5, 36.5, 37.5)),
        CentilePoint(2, listOf(35.0, 35.8, 36.6, 37.4, 38.3, 39.1, 39.9, 40.7, 41.5)),
        CentilePoint(4, listOf(37.2, 38.0, 38.8, 39.7, 40.6, 41.4, 42.3, 43.1, 44.0)),
        CentilePoint(6, listOf(38.6, 39.5, 40.5, 41.3, 42.2, 43.1, 43.9, 45.0, 46.0)),
        CentilePoint(8, listOf(39.8, 40.6, 41.6, 42.5, 43.4, 44.3, 45.1, 46.1, 46.9)),
        CentilePoint(10, listOf(40.6, 41.5, 42.4, 43.3, 44.2, 45.1, 46.0, 47.0, 47.8)),
        CentilePoint(12, listOf(41.1, 42.1, 43.1, 44.0, 44.9, 45.8, 46.8, 47.8, 48.8)),
        CentilePoint(15, listOf(42.0, 42.8, 43.8, 44.8, 45.7, 46.6, 47.5, 48.5, 49.3)),
        CentilePoint(18, listOf(42.5, 43.4, 44.4, 45.3, 46.2, 47.1, 48.1, 49.1, 49.9)),
        CentilePoint(21, listOf(43.0, 43.9, 44.8, 45.8, 46.7, 47.7, 48.6, 49.6, 50.4)),
        CentilePoint(24, listOf(43.2, 44.2, 45.3, 46.1, 47.2, 48.2, 49.1, 50.2, 51.2))
    )

    val centileLabels = listOf("0.4", "2", "9", "25", "50", "75", "91", "98", "99.6")
}
