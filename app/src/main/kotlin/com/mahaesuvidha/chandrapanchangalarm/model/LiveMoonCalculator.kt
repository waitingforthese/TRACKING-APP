package com.mahaesuvidha.chandrapanchangalarm.model

import swisseph.SweConst
import swisseph.SweDate
import swisseph.SwissEph
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object LiveMoonCalculator {

    // ==========================================
    // RASHI NAMES
    // ==========================================

    private val rashiNames = arrayOf(
        "मेष",
        "वृषभ",
        "मिथुन",
        "कर्क",
        "सिंह",
        "कन्या",
        "तुळ",
        "वृश्चिक",
        "धनु",
        "मकर",
        "कुंभ",
        "मीन"
    )

    // ==========================================
    // NAKSHATRA NAMES
    // ==========================================

    private val nakshatraNames = arrayOf(
        "अश्विनी",
        "भरणी",
        "कृत्तिका",
        "रोहिणी",
        "मृगशीर्ष",
        "आर्द्रा",
        "पुनर्वसू",
        "पुष्य",
        "आश्लेषा",
        "मघा",
        "पूर्वाफाल्गुनी",
        "उत्तराफाल्गुनी",
        "हस्त",
        "चित्रा",
        "स्वाती",
        "विशाखा",
        "अनुराधा",
        "ज्येष्ठा",
        "मूळ",
        "पूर्वाषाढा",
        "उत्तराषाढा",
        "श्रवण",
        "धनिष्ठा",
        "शततारका",
        "पूर्वाभाद्रपदा",
        "उत्तराभाद्रपदा",
        "रेवती"
    )

    // ==========================================
    // DATE → JULIAN DAY
    // ==========================================

    private fun getJulianDay(
        millis: Long = System.currentTimeMillis()
    ): Double {

        val calendar =
            Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            )

        calendar.timeInMillis =
            millis

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH) + 1

        val day =
            calendar.get(Calendar.DAY_OF_MONTH)

        val hour =
            calendar.get(Calendar.HOUR_OF_DAY) +
                    calendar.get(Calendar.MINUTE) / 60.0 +
                    calendar.get(Calendar.SECOND) / 3600.0 +
                    calendar.get(Calendar.MILLISECOND) / 3600000.0

        return SweDate.getJulDay(
            year,
            month,
            day,
            hour,
            SweDate.SE_GREG_CAL
        )
    }

    // ==========================================
    // MOON LONGITUDE
    // ==========================================

    private fun getMoonLongitude(
        millis: Long = System.currentTimeMillis()
    ): Double {

        val swe =
            SwissEph()

        swe.swe_set_sid_mode(
            SweConst.SE_SIDM_LAHIRI,
            0.0,
            0.0
        )

        val xx =
            DoubleArray(6)

        val serr =
            StringBuffer()

        swe.swe_calc_ut(
            getJulianDay(millis),
            SweConst.SE_MOON,
            SweConst.SEFLG_SWIEPH or
                    SweConst.SEFLG_SIDEREAL,
            xx,
            serr
        )

        return xx[0]
    }

    // ==========================================
    // CURRENT RASHI
    // ==========================================

    fun getCurrentRashi(): String {

        val longitude =
            getMoonLongitude()

        val index =
            (longitude / 30.0)
                .toInt()
                .coerceIn(0, 11)

        return rashiNames[index]
    }

    // ==========================================
    // CURRENT NAKSHATRA
    // ==========================================

    fun getCurrentNakshatra(): String {

        val longitude =
            getMoonLongitude()

        val nakshatraSize =
            360.0 / 27.0

        val index =
            (longitude / nakshatraSize)
                .toInt()
                .coerceIn(0, 26)

        return nakshatraNames[index]
    }

    // ==========================================
    // CURRENT CHARAN
    // ==========================================

    fun getCurrentCharan(): Int {

        val longitude =
            getMoonLongitude()

        val nakshatraSize =
            360.0 / 27.0

        val padaSize =
            nakshatraSize / 4.0

        return (
            (longitude % nakshatraSize) /
                    padaSize
        ).toInt()
            .coerceIn(0, 3) + 1
    }

    // ==========================================
    // FORMAT TIME
    // ==========================================

    private fun formatDateTime(
        millis: Long
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd-MM-yyyy HH:mm",
                Locale.getDefault()
            )

        formatter.timeZone =
            TimeZone.getDefault()

        return formatter.format(
            millis
        )
    }

    // ==========================================
    // FIND NEXT RASHI
    // ==========================================

    private fun findNextRashiChange(
        now: Long,
        currentRashi: Int
    ): Pair<String, Long> {

        var check =
            now

        // Moon change साधारण 2.5 दिवसात होतो
        repeat(5000) {

            check += 60_000L

            val longitude =
                getMoonLongitude(check)

            val index =
                (longitude / 30.0)
                    .toInt()
                    .coerceIn(0, 11)

            if (index != currentRashi) {

                return Pair(
                    "${rashiNames[currentRashi]} → ${rashiNames[index]}",
                    check
                )
            }
        }

        return Pair(
            "पुढील राशी बदल शोधत आहे",
            now + (3L * 24 * 60 * 60 * 1000)
        )
    }

    // ==========================================
    // FIND NEXT NAKSHATRA
    // ==========================================

    private fun findNextNakshatraChange(
        now: Long,
        currentNakshatra: Int
    ): Pair<String, Long> {

        var check =
            now

        repeat(3000) {

            check += 60_000L

            val longitude =
                getMoonLongitude(check)

            val nakshatraSize =
                360.0 / 27.0

            val index =
                (longitude / nakshatraSize)
                    .toInt()
                    .coerceIn(0, 26)

            if (index != currentNakshatra) {

                return Pair(
                    "${nakshatraNames[currentNakshatra]} → ${nakshatraNames[index]}",
                    check
                )
            }
        }

        return Pair(
            "पुढील नक्षत्र बदल शोधत आहे",
            now + (2L * 24 * 60 * 60 * 1000)
        )
    }

    // ==========================================
    // FIND NEXT CHARAN
    // ==========================================

    private fun findNextCharanChange(
        now: Long,
        currentNakshatra: Int,
        currentCharan: Int
    ): Pair<String, Long> {

        var check =
            now

        repeat(1000) {

            check += 60_000L

            val longitude =
                getMoonLongitude(check)

            val nakshatraSize =
                360.0 / 27.0

            val padaSize =
                nakshatraSize / 4.0

            val nakshatra =
                (longitude / nakshatraSize)
                    .toInt()
                    .coerceIn(0, 26)

            val charan =
                (
                    (longitude % nakshatraSize) /
                            padaSize
                ).toInt()
                    .coerceIn(0, 3) + 1

            if (
                nakshatra != currentNakshatra ||
                charan != currentCharan
            ) {

                val nextCharan =
                    if (
                        nakshatra != currentNakshatra
                    ) {
                        1
                    } else {
                        charan
                    }

                return Pair(
                    "चरण $currentCharan → चरण $nextCharan",
                    check
                )
            }
        }

        return Pair(
            "पुढील चरण बदल शोधत आहे",
            now + (8L * 60 * 60 * 1000)
        )
    }

    // ==========================================
    // MAIN MOON STATE
    // ==========================================

    fun getCurrentMoonState(): MoonState {

        val now =
            System.currentTimeMillis()

        val longitude =
            getMoonLongitude(now)

        val rashiIndex =
            (longitude / 30.0)
                .toInt()
                .coerceIn(0, 11)

        val nakshatraSize =
            360.0 / 27.0

        val nakshatraIndex =
            (longitude / nakshatraSize)
                .toInt()
                .coerceIn(0, 26)

        val padaSize =
            nakshatraSize / 4.0

        val currentPada =
            (
                (longitude % nakshatraSize) /
                        padaSize
            ).toInt()
                .coerceIn(0, 3) + 1

        val nextRashi =
            findNextRashiChange(
                now,
                rashiIndex
            )

        val nextNakshatra =
            findNextNakshatraChange(
                now,
                nakshatraIndex
            )

        val nextCharan =
            findNextCharanChange(
                now,
                nakshatraIndex,
                currentPada
            )

        return MoonState(

            location = "भारत",

            rashi =
                Rashi.entries[rashiIndex],

            nakshatra =
                Nakshatra.entries[nakshatraIndex],

            pada =
                currentPada,

            nextRashi =
                nextRashi.first,

            nextRashiTime =
                formatDateTime(
                    nextRashi.second
                ),

            nextRashiMillis =
                nextRashi.second,

            nextNakshatra =
                nextNakshatra.first,

            nextNakshatraTime =
                formatDateTime(
                    nextNakshatra.second
                ),

            nextNakshatraMillis =
                nextNakshatra.second,

            nextCharan =
                nextCharan.first,

            nextCharanTime =
                formatDateTime(
                    nextCharan.second
                ),

            nextCharanMillis =
                nextCharan.second
        )
    }
}
