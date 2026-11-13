package de.pflegefueranfaenger.core.ecg

import de.pflegefueranfaenger.core.model.Rhythm
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Erzeugt schematische EKG-Streifen (Ableitung II) zum Üben der Rhythmuserkennung.
 * Die Kurven sind didaktisch vereinfacht, aber in Frequenz, Regelmäßigkeit,
 * P-Wellen und QRS-Breite typisch für den jeweiligen Rhythmus.
 *
 * Einheit der Werte: Millivolt. Abtastrate: [SAMPLE_RATE] Hz.
 */
object EcgGenerator {
    const val SAMPLE_RATE = 250

    fun generate(rhythm: Rhythm, seconds: Double = 6.0, seed: Int = 7): FloatArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val s = DoubleArray(n)
        val rnd = Random(seed)
        when (rhythm) {
            Rhythm.SINUS -> regularSinus(s, seconds, rate = 75.0, pq = 0.16)
            Rhythm.SINUS_BRADY -> regularSinus(s, seconds, rate = 45.0, pq = 0.16)
            Rhythm.SINUS_TACHY -> regularSinus(s, seconds, rate = 125.0, pq = 0.14)
            Rhythm.AV1 -> regularSinus(s, seconds, rate = 68.0, pq = 0.32)
            Rhythm.AFIB -> afib(s, seconds, rnd)
            Rhythm.FLUTTER -> flutter(s, seconds)
            Rhythm.AV2_WENCKEBACH -> wenckebach(s, seconds)
            Rhythm.AV3 -> av3(s, seconds)
            Rhythm.VES -> ves(s, seconds)
            Rhythm.VT -> vt(s, seconds)
            Rhythm.VF -> vf(s, rnd)
            Rhythm.ASYSTOLE -> Unit
            Rhythm.ARTIFACT -> artifact(s, seconds, rnd)
        }
        // Leichtes Grundrauschen und Grundlinienschwankung wie am echten Monitor
        for (i in s.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            s[i] += 0.015 * (rnd.nextDouble() - 0.5) + 0.02 * sin(2 * PI * 0.25 * t)
        }
        return FloatArray(n) { s[it].toFloat() }
    }

    /** Herzfrequenz aus R-Zacken-Zeitpunkten (Schläge pro Minute). */
    fun heartRate(rPeaks: List<Double>): Double {
        if (rPeaks.size < 2) return 0.0
        val rr = rPeaks.zipWithNext { a, b -> b - a }.average()
        return 60.0 / rr
    }

    // --- Bausteine ---------------------------------------------------------

    private fun gauss(s: DoubleArray, center: Double, amp: Double, width: Double) {
        val from = ((center - 4 * width) * SAMPLE_RATE).toInt().coerceAtLeast(0)
        val to = ((center + 4 * width) * SAMPLE_RATE).toInt().coerceAtMost(s.size - 1)
        for (i in from..to) {
            val t = i.toDouble() / SAMPLE_RATE
            val d = (t - center) / width
            s[i] += amp * exp(-0.5 * d * d)
        }
    }

    private fun pWave(s: DoubleArray, center: Double) = gauss(s, center, 0.15, 0.022)

    /** Schmaler QRS-Komplex mit T-Welle, R-Zacke bei [tR]. */
    private fun narrowBeat(s: DoubleArray, tR: Double, qt: Double = 0.30) {
        gauss(s, tR - 0.025, -0.10, 0.008)
        gauss(s, tR, 1.00, 0.010)
        gauss(s, tR + 0.025, -0.25, 0.010)
        gauss(s, tR + qt - 0.05, 0.30, 0.050)
    }

    /** Breiter, deformierter Komplex (ventrikulärer Ursprung) mit diskordanter T-Welle. */
    private fun wideBeat(s: DoubleArray, tR: Double, amp: Double = 1.2) {
        gauss(s, tR, amp, 0.035)
        gauss(s, tR + 0.09, -0.55 * amp, 0.045)
        gauss(s, tR + 0.30, -0.30 * amp, 0.060)
    }

    /** P-Welle passend zur PQ-Zeit vor einer R-Zacke. */
    private fun pBefore(s: DoubleArray, tR: Double, pq: Double) = pWave(s, tR - pq + 0.01)

    private fun regularSinus(s: DoubleArray, seconds: Double, rate: Double, pq: Double) {
        val rr = 60.0 / rate
        val qt = if (rate > 100) 0.26 else 0.32
        var t = 0.45
        while (t < seconds - 0.1) {
            pBefore(s, t, pq)
            narrowBeat(s, t, qt)
            t += rr
        }
    }

    private fun afib(s: DoubleArray, seconds: Double, rnd: Random) {
        // Flimmerwellen statt P-Wellen
        for (i in s.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            s[i] += 0.05 * sin(2 * PI * 6.1 * t) + 0.035 * sin(2 * PI * 7.7 * t + 1.3) +
                0.02 * sin(2 * PI * 9.3 * t + 0.4)
        }
        // absolut unregelmäßige RR-Abstände
        var t = 0.3
        while (t < seconds - 0.1) {
            narrowBeat(s, t)
            t += 0.42 + rnd.nextDouble() * 0.62
        }
    }

    private fun flutter(s: DoubleArray, seconds: Double) {
        // Sägezahn mit 300/min, Überleitung 2:1 -> Kammerfrequenz 150/min
        val f = 5.0
        for (i in s.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val phase = (t * f) % 1.0
            s[i] += if (phase < 0.8) -0.22 * (phase / 0.8) + 0.08 else -0.14 + 0.22 * ((phase - 0.8) / 0.2) * 1.0
        }
        var t = 0.35
        while (t < seconds - 0.1) {
            narrowBeat(s, t, qt = 0.26)
            t += 0.4
        }
    }

    private fun wenckebach(s: DoubleArray, seconds: Double) {
        // P-Wellen regelmäßig, PQ verlängert sich 0,16 -> 0,26 -> 0,34, dann fällt ein QRS aus
        val pp = 0.8
        val pqs = listOf(0.16, 0.26, 0.34, null)
        var tP = 0.25
        var k = 0
        while (tP < seconds - 0.4) {
            pWave(s, tP)
            val pq = pqs[k % pqs.size]
            if (pq != null) narrowBeat(s, tP + pq - 0.01)
            tP += pp
            k++
        }
    }

    private fun av3(s: DoubleArray, seconds: Double) {
        // Vorhof und Kammer schlagen unabhängig: P 85/min, Ersatzrhythmus 38/min breit
        var tP = 0.2
        while (tP < seconds) {
            pWave(s, tP); tP += 60.0 / 85.0
        }
        var tR = 0.9
        while (tR < seconds - 0.2) {
            wideBeat(s, tR, amp = 1.0); tR += 60.0 / 38.0
        }
    }

    private fun ves(s: DoubleArray, seconds: Double) {
        val rr = 0.8
        var t = 0.45
        var k = 0
        while (t < seconds - 0.1) {
            if (k % 4 == 3) {
                // vorzeitiger breiter Schlag ohne P-Welle, danach kompensatorische Pause
                val tV = t - rr + rr * 0.55
                wideBeat(s, tV)
                t += rr // nächster Sinusschlag zur regulären Zeit (kompensatorisch)
                k++
                continue
            }
            pBefore(s, t, 0.16)
            narrowBeat(s, t)
            t += rr
            k++
        }
    }

    private fun vt(s: DoubleArray, seconds: Double) {
        var t = 0.2
        while (t < seconds - 0.1) {
            wideBeat(s, t, amp = 1.3); t += 60.0 / 180.0
        }
    }

    private fun vf(s: DoubleArray, rnd: Random) {
        val f1 = 4.6 + rnd.nextDouble()
        val f2 = 6.3 + rnd.nextDouble()
        for (i in s.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = 0.45 + 0.3 * sin(2 * PI * 0.35 * t) + 0.15 * sin(2 * PI * 0.9 * t + 1.0)
            s[i] += env * (sin(2 * PI * f1 * t) + 0.6 * sin(2 * PI * f2 * t + 0.7))
        }
    }

    private fun artifact(s: DoubleArray, seconds: Double, rnd: Random) {
        regularSinus(s, seconds, rate = 78.0, pq = 0.16)
        // Zitter-/Bewegungsartefakt im mittleren Abschnitt; die QRS-Komplexe laufen darunter weiter
        val from = (2.0 * SAMPLE_RATE).toInt()
        val to = (4.2 * SAMPLE_RATE).toInt().coerceAtMost(s.size)
        for (i in from until to) {
            val t = i.toDouble() / SAMPLE_RATE
            s[i] += 0.35 * sin(2 * PI * 8.5 * t) + 0.25 * (rnd.nextDouble() - 0.5) + 0.2 * sin(2 * PI * 3.1 * t)
        }
    }
}
