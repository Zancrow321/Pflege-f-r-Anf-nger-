package de.pflegefueranfaenger.core.figure

import kotlin.math.hypot

/**
 * Plattformunabhängige Vektorzeichnung. Die App zeichnet sie mit Compose-Canvas,
 * Tests und Vorschaubilder nutzen [SvgExport]. Koordinaten sind in Einheiten der
 * Zeichenfläche ([width] × [height]); die App skaliert gleichmäßig auf die verfügbare Breite.
 */
data class Drawing(
    val width: Float,
    val height: Float,
    val elements: List<Element>,
    /** Beschreibung für Screenreader. */
    val description: String,
)

/** Farbrollen. Die konkreten Farben legt die App je nach hellem/dunklem Design fest. */
enum class Tone {
    INK, LINE, MUTED, PANEL,
    SKIN, SKIN_DARK,
    ARTERY, ARTERY_SOFT, VEIN, VEIN_SOFT, MYOCARD, HEART_SOFT, NODE,
    HIGHLIGHT, ON_HIGHLIGHT, ACCENT, BED,
    PAPER, GRID_MINOR, GRID_MAJOR, TRACE,
    EL_RED, EL_YELLOW, EL_GREEN, EL_BLACK, EL_WHITE,
    BLACK, WHITE,
}

enum class Anchor { START, MIDDLE, END }

sealed interface Element {
    /** SVG-Pfad (Befehle M, L, H, V, C, Q, A, Z). */
    data class PathEl(
        val d: String,
        val fill: Tone? = null,
        val stroke: Tone? = null,
        val width: Float = 2f,
        val dashed: Boolean = false,
    ) : Element

    /** Linie mit runden Enden. */
    data class LineEl(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val tone: Tone,
        val width: Float = 2f,
        val dashed: Boolean = false,
    ) : Element

    data class CircleEl(
        val cx: Float, val cy: Float, val r: Float,
        val fill: Tone? = null,
        val stroke: Tone? = null,
        val width: Float = 2f,
    ) : Element

    /** Text; [y] ist die vertikale Mitte der Zeile. */
    data class TextEl(
        val x: Float, val y: Float,
        val text: String,
        val tone: Tone = Tone.INK,
        val size: Float = 13f,
        val anchor: Anchor = Anchor.MIDDLE,
        val bold: Boolean = false,
    ) : Element
}

/** Hilfsklasse zum Aufbau einer [Drawing]. */
class DrawingBuilder(private val width: Float, private val height: Float) {
    private val elements = mutableListOf<Element>()

    fun path(d: String, fill: Tone? = null, stroke: Tone? = null, width: Float = 2f, dashed: Boolean = false) {
        elements += Element.PathEl(d.trimIndent().replace(Regex("\\s+"), " ").trim(), fill, stroke, width, dashed)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, tone: Tone, width: Float = 2f, dashed: Boolean = false) {
        elements += Element.LineEl(x1, y1, x2, y2, tone, width, dashed)
    }

    fun circle(cx: Float, cy: Float, r: Float, fill: Tone? = null, stroke: Tone? = null, width: Float = 2f) {
        elements += Element.CircleEl(cx, cy, r, fill, stroke, width)
    }

    fun text(
        x: Float, y: Float, text: String, tone: Tone = Tone.INK, size: Float = 13f,
        anchor: Anchor = Anchor.MIDDLE, bold: Boolean = false,
    ) {
        elements += Element.TextEl(x, y, text, tone, size, anchor, bold)
    }

    /** Pfeil von (x1,y1) nach (x2,y2); mit [both] an beiden Enden eine Spitze. */
    fun arrow(x1: Float, y1: Float, x2: Float, y2: Float, tone: Tone, width: Float = 3f, head: Float = 11f, both: Boolean = false, dashed: Boolean = false) {
        val len = hypot(x2 - x1, y2 - y1)
        val ux = (x2 - x1) / len
        val uy = (y2 - y1) / len
        val sx = if (both) x1 + ux * head * 0.8f else x1
        val sy = if (both) y1 + uy * head * 0.8f else y1
        line(sx, sy, x2 - ux * head * 0.8f, y2 - uy * head * 0.8f, tone, width, dashed)
        arrowHead(x2, y2, ux, uy, tone, head)
        if (both) arrowHead(x1, y1, -ux, -uy, tone, head)
    }

    /** Pfeilspitze an (x,y) in Richtung (ux,uy) (Einheitsvektor). */
    fun arrowHead(x: Float, y: Float, ux: Float, uy: Float, tone: Tone, head: Float = 11f) {
        val px = -uy
        val py = ux
        val bx = x - ux * head
        val by = y - uy * head
        val w = head * 0.6f
        path("M${f(x)},${f(y)} L${f(bx + px * w)},${f(by + py * w)} L${f(bx - px * w)},${f(by - py * w)} Z", fill = tone)
    }

    /** Runde Markierung mit „?“ – zeigt bei Bildfragen auf den gesuchten Teil. */
    fun questionBadge(x: Float, y: Float, r: Float = 13f) {
        circle(x, y, r, fill = Tone.HIGHLIGHT, stroke = Tone.PANEL, width = 2.5f)
        text(x, y, "?", Tone.ON_HIGHLIGHT, size = r * 1.35f, bold = true)
    }

    fun build(description: String) = Drawing(width, height, elements.toList(), description)

    private fun f(v: Float): String = "%.1f".format(java.util.Locale.ROOT, v)
}
