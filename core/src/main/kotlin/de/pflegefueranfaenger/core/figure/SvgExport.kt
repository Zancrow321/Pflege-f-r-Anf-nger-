package de.pflegefueranfaenger.core.figure

/** Farbwerte (0xRRGGBB) je [Tone] für helles und dunkles Design. */
object Palette {
    val light: Map<Tone, Long> = mapOf(
        Tone.INK to 0x1B1B1F, Tone.LINE to 0x44474F, Tone.MUTED to 0x7A7F8C, Tone.PANEL to 0xFFFFFF,
        Tone.SKIN to 0xF3CDAA, Tone.SKIN_DARK to 0xC9926B,
        Tone.ARTERY to 0xD8363A, Tone.ARTERY_SOFT to 0xF9CFCB, Tone.VEIN to 0x3467D0, Tone.VEIN_SOFT to 0xCBDAF8,
        Tone.MYOCARD to 0xB04A55, Tone.HEART_SOFT to 0xF8DADA, Tone.NODE to 0xF29C00,
        Tone.HIGHLIGHT to 0x00A88A, Tone.ON_HIGHLIGHT to 0xFFFFFF, Tone.ACCENT to 0x011753, Tone.BED to 0x8E9BB0,
        Tone.PAPER to 0xFFF4F4, Tone.GRID_MINOR to 0xF8C8C8, Tone.GRID_MAJOR to 0xEE9A9A, Tone.TRACE to 0x1B1B1B,
        Tone.EL_RED to 0xE53935, Tone.EL_YELLOW to 0xFDD835, Tone.EL_GREEN to 0x43A047, Tone.EL_BLACK to 0x212121,
        Tone.EL_WHITE to 0xFAFAFA, Tone.BLACK to 0x111111, Tone.WHITE to 0xFFFFFF,
    )

    val dark: Map<Tone, Long> = light + mapOf(
        Tone.INK to 0xE6E8F0, Tone.LINE to 0xC4C7D0, Tone.MUTED to 0x9AA0AE, Tone.PANEL to 0x101A3C,
        Tone.SKIN to 0xD9AE8A, Tone.SKIN_DARK to 0xA9744F,
        Tone.ARTERY to 0xF0605F, Tone.ARTERY_SOFT to 0x6A2C33, Tone.VEIN to 0x6E9BF2, Tone.VEIN_SOFT to 0x283F72,
        Tone.MYOCARD to 0xD9707A, Tone.HEART_SOFT to 0x4A2833, Tone.NODE to 0xFFB300,
        Tone.HIGHLIGHT to 0x38F5C3, Tone.ON_HIGHLIGHT to 0x00382B, Tone.ACCENT to 0x9FB4FF, Tone.BED to 0x7C89A0,
    )
}

/** Wandelt eine [Drawing] in SVG um (für Tests und Vorschaubilder). */
object SvgExport {
    fun toSvg(d: Drawing, palette: Map<Tone, Long> = Palette.light): String {
        fun c(t: Tone?) = if (t == null) "none" else "#%06X".format(palette.getValue(t))
        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${d.width} ${d.height}" width="${d.width * 2}" height="${d.height * 2}">""")
        sb.append("""<rect width="100%" height="100%" fill="${c(Tone.PANEL)}"/>""")
        d.elements.forEach { e ->
            when (e) {
                is Element.PathEl -> sb.append(
                    """<path d="${e.d}" fill="${c(e.fill)}" stroke="${c(e.stroke)}" stroke-width="${e.width}" stroke-linejoin="round" stroke-linecap="round"${dash(e.dashed, e.width)}/>""",
                )
                is Element.LineEl -> sb.append(
                    """<line x1="${e.x1}" y1="${e.y1}" x2="${e.x2}" y2="${e.y2}" stroke="${c(e.tone)}" stroke-width="${e.width}" stroke-linecap="round"${dash(e.dashed, e.width)}/>""",
                )
                is Element.CircleEl -> sb.append(
                    """<circle cx="${e.cx}" cy="${e.cy}" r="${e.r}" fill="${c(e.fill)}" stroke="${c(e.stroke)}" stroke-width="${e.width}"/>""",
                )
                is Element.TextEl -> {
                    val anchor = when (e.anchor) { Anchor.START -> "start"; Anchor.MIDDLE -> "middle"; Anchor.END -> "end" }
                    val weight = if (e.bold) "bold" else "normal"
                    sb.append(
                        """<text x="${e.x}" y="${e.y}" fill="${c(e.tone)}" font-size="${e.size}" font-family="Roboto, Arial, sans-serif" font-weight="$weight" text-anchor="$anchor" dominant-baseline="central">${esc(e.text)}</text>""",
                    )
                }
            }
        }
        sb.append("</svg>")
        return sb.toString()
    }

    private fun dash(dashed: Boolean, w: Float) = if (dashed) """ stroke-dasharray="${w * 3} ${w * 2.5f}"""" else ""
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
