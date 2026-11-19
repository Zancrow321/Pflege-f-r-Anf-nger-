package de.pflegefueranfaenger.core.figure

import de.pflegefueranfaenger.core.model.Figure
import de.pflegefueranfaenger.core.model.Illustration
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Ein hervorhebbarer Teil eines Schaubilds. */
data class FigurePart(val key: String, val label: String)

/**
 * Alle Schaubilder der App, selbst gezeichnet als schematische Vektorgrafik.
 * Schemata, keine maßstabsgetreuen Abbildungen.
 *
 * Wichtig für alle Ansichten von vorne: Die Person schaut dich an. Ihre rechte Seite (dexter)
 * liegt für dich links im Bild – wie bei Patientinnen und Patienten im Bett.
 */
object Figures {

    fun parts(figure: Figure): List<FigurePart> = when (figure) {
        Figure.HEART_ANATOMY -> listOf(
            FigurePart("rechter_vorhof", "Rechter Vorhof"),
            FigurePart("rechte_kammer", "Rechte Kammer"),
            FigurePart("linker_vorhof", "Linker Vorhof"),
            FigurePart("linke_kammer", "Linke Kammer"),
            FigurePart("hohlvenen", "Hohlvenen"),
            FigurePart("lungenarterie", "Lungenarterie"),
            FigurePart("lungenvenen", "Lungenvenen"),
            FigurePart("aorta", "Aorta"),
            FigurePart("trikuspidalklappe", "Trikuspidalklappe"),
            FigurePart("pulmonalklappe", "Pulmonalklappe"),
            FigurePart("mitralklappe", "Mitralklappe"),
            FigurePart("aortenklappe", "Aortenklappe"),
        )
        Figure.CONDUCTION -> listOf(
            FigurePart("sinusknoten", "Sinusknoten"),
            FigurePart("av_knoten", "AV-Knoten"),
            FigurePart("his", "His-Bündel"),
            FigurePart("tawara", "Tawara-Schenkel"),
            FigurePart("purkinje", "Purkinje-Fasern"),
        )
        Figure.CORONARIES -> listOf(
            FigurePart("rca", "Rechte Koronararterie (RCA)"),
            FigurePart("hauptstamm", "Hauptstamm (LCA)"),
            FigurePart("lad", "RIVA / LAD"),
            FigurePart("rcx", "RCX"),
        )
        Figure.ECG_WAVES -> listOf(
            FigurePart("p", "P-Welle"),
            FigurePart("qrs", "QRS-Komplex"),
            FigurePart("t", "T-Welle"),
            FigurePart("pq", "PQ-Zeit"),
            FigurePart("st", "ST-Strecke"),
            FigurePart("qt", "QT-Zeit"),
        )
        Figure.ELECTRODES -> listOf(
            FigurePart("rot", "Rot (R)"),
            FigurePart("gelb", "Gelb (L)"),
            FigurePart("gruen", "Grün (F)"),
            FigurePart("schwarz", "Schwarz (N)"),
            FigurePart("weiss", "Weiß (C)"),
        )
        Figure.BODY_FRONT -> listOf(
            FigurePart("kranial", "kranial"),
            FigurePart("kaudal", "kaudal"),
            FigurePart("dexter", "dexter (rechts)"),
            FigurePart("sinister", "sinister (links)"),
            FigurePart("medial", "medial"),
            FigurePart("lateral", "lateral"),
            FigurePart("median", "median"),
        )
        Figure.BODY_SIDE -> listOf(
            FigurePart("ventral", "ventral / anterior"),
            FigurePart("dorsal", "dorsal / posterior"),
            FigurePart("superior", "superior"),
            FigurePart("inferior", "inferior"),
        )
        Figure.LIMB -> listOf(
            FigurePart("proximal", "proximal"),
            FigurePart("distal", "distal"),
        )
        Figure.POSITIONING -> listOf(
            FigurePart("flach", "Flachlage"),
            FigurePart("oberkoerper", "Oberkörperhochlage"),
            FigurePart("herzbett", "Herzbettlage"),
        )
    }

    fun draw(ill: Illustration): Drawing = draw(ill.figure, ill.highlight, ill.labels)

    fun draw(figure: Figure, highlight: String? = null, labels: Boolean = true): Drawing {
        require(highlight == null || parts(figure).any { it.key == highlight }) {
            "Unbekannter Teil „$highlight“ für $figure"
        }
        val ctx = Ctx(highlight, labels)
        return when (figure) {
            Figure.HEART_ANATOMY -> heartAnatomy(ctx)
            Figure.CONDUCTION -> conduction(ctx)
            Figure.CORONARIES -> coronaries(ctx)
            Figure.ECG_WAVES -> ecgWaves(ctx)
            Figure.ELECTRODES -> electrodes(ctx)
            Figure.BODY_FRONT -> bodyFront(ctx)
            Figure.BODY_SIDE -> bodySide(ctx)
            Figure.LIMB -> limb(ctx)
            Figure.POSITIONING -> positioning(ctx)
        }
    }

    private class Ctx(val highlight: String?, val labels: Boolean) {
        fun hl(key: String) = highlight == key
        fun tone(key: String, normal: Tone) = if (hl(key)) Tone.HIGHLIGHT else normal
        fun width(key: String, normal: Float) = if (hl(key)) normal * 1.6f else normal
        /** Pfeil/Beschriftung zeigen? Ohne Beschriftung nur der gesuchte Teil. */
        fun show(key: String) = labels || hl(key)
        fun description(title: String, parts: List<FigurePart>): String {
            val h = parts.firstOrNull { it.key == highlight }
            return when {
                h != null && !labels -> "$title. Ein Teil ist mit Fragezeichen markiert."
                h != null -> "$title. Hervorgehoben: ${h.label}."
                else -> "$title. Beschriftet: ${parts.joinToString { it.label }}."
            }
        }
    }

    // ---------------------------------------------------------------- Herz: Aufbau

    private fun heartAnatomy(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 400f)
        // Lungenarterie (Truncus pulmonalis) mit Ästen zu beiden Lungen
        val pa = c.tone("lungenarterie", Tone.VEIN)
        b.path("M174,212 L174,98 L46,98 L46,84 L300,84 L300,98 L200,98 L200,212 Z", fill = pa, stroke = Tone.LINE, width = 1.5f)
        // Hohlvenen
        val hv = c.tone("hohlvenen", Tone.VEIN)
        b.path("M112,34 L142,34 L142,116 L112,116 Z", fill = hv, stroke = Tone.LINE, width = 1.5f)
        b.path("M56,330 L56,186 Q56,170 72,170 L100,170 L100,196 L82,196 L82,330 Z", fill = hv, stroke = Tone.LINE, width = 1.5f)
        // Lungenvenen
        val lv = c.tone("lungenvenen", Tone.ARTERY)
        b.path("M312,122 L352,122 L352,134 L312,134 Z", fill = lv, stroke = Tone.LINE, width = 1.5f)
        b.path("M312,152 L352,152 L352,164 L312,164 Z", fill = lv, stroke = Tone.LINE, width = 1.5f)
        // Vorhöfe und Kammern (Wandstärke = Strichbreite: linke Kammer am dicksten)
        b.path("M98,124 Q98,108 114,108 L168,108 L170,198 L96,198 Q90,160 98,124 Z",
            fill = c.tone("rechter_vorhof", Tone.VEIN_SOFT), stroke = Tone.MYOCARD, width = 3f)
        b.path("M240,114 L302,114 Q318,114 318,130 Q320,160 310,190 L240,190 Z",
            fill = c.tone("linker_vorhof", Tone.ARTERY_SOFT), stroke = Tone.MYOCARD, width = 3f)
        b.path("M98,206 L200,206 L210,302 Q176,322 146,296 Q106,258 98,206 Z",
            fill = c.tone("rechte_kammer", Tone.VEIN_SOFT), stroke = Tone.MYOCARD, width = 5f)
        b.path("M212,198 L308,198 Q322,250 286,298 Q256,330 218,318 Z",
            fill = c.tone("linke_kammer", Tone.ARTERY_SOFT), stroke = Tone.MYOCARD, width = 10f)
        // Aorta: aus der linken Kammer nach oben, Bogen, absteigend zum Körper
        val ao = c.tone("aorta", Tone.ARTERY)
        b.path("M208,204 L208,70 Q208,26 256,26 L346,26 Q390,26 390,70 L390,340 L366,340 L366,72 Q366,50 344,50 L256,50 Q234,50 234,72 L234,204 Z",
            fill = ao, stroke = Tone.LINE, width = 1.5f)
        // Klappen
        val valves = listOf(
            Triple("trikuspidalklappe", floatArrayOf(108f, 202f, 160f, 202f), Pair(134f, 222f)),
            Triple("pulmonalklappe", floatArrayOf(176f, 204f, 198f, 204f), Pair(187f, 226f)),
            Triple("mitralklappe", floatArrayOf(250f, 194f, 300f, 194f), Pair(276f, 216f)),
            Triple("aortenklappe", floatArrayOf(210f, 196f, 232f, 196f), Pair(222f, 222f)),
        )
        valves.forEachIndexed { i, (key, l, m) ->
            b.line(l[0], l[1], l[2], l[3], c.tone(key, Tone.LINE), width = c.width(key, 4f))
            if (c.labels) {
                b.circle(m.first, m.second, 9f, fill = c.tone(key, Tone.ACCENT))
                b.text(m.first, m.second, "${i + 1}", Tone.WHITE, size = 11f, bold = true)
            }
        }
        // Flussrichtung
        b.arrow(127f, 44f, 127f, 100f, Tone.WHITE, width = 2f, head = 7f)
        b.arrow(150f, 168f, 150f, 244f, Tone.LINE, width = 2f, head = 7f)
        b.arrow(187f, 190f, 187f, 112f, Tone.WHITE, width = 2f, head = 7f)
        b.arrow(290f, 166f, 290f, 238f, Tone.LINE, width = 2f, head = 7f)
        b.arrow(221f, 186f, 221f, 64f, Tone.WHITE, width = 2f, head = 7f)
        b.arrow(350f, 128f, 322f, 128f, Tone.WHITE, width = 2f, head = 6f)
        b.arrow(350f, 158f, 322f, 158f, Tone.WHITE, width = 2f, head = 6f)

        val anchors = mapOf(
            "rechter_vorhof" to Pair(130f, 142f), "rechte_kammer" to Pair(150f, 268f),
            "linker_vorhof" to Pair(270f, 142f), "linke_kammer" to Pair(258f, 268f),
            "hohlvenen" to Pair(69f, 300f), "lungenarterie" to Pair(70f, 91f),
            "lungenvenen" to Pair(332f, 104f), "aorta" to Pair(300f, 38f),
            "trikuspidalklappe" to Pair(134f, 222f), "pulmonalklappe" to Pair(187f, 226f),
            "mitralklappe" to Pair(276f, 216f), "aortenklappe" to Pair(222f, 222f),
        )
        if (c.labels) {
            b.text(127f, 18f, "obere Hohlvene", size = 12f)
            b.text(69f, 346f, "untere Hohlvene", size = 12f)
            b.text(56f, 70f, "Lungenarterie", size = 12f)
            b.text(300f, 12f, "Aorta", size = 12f, bold = true)
            b.text(336f, 96f, "Lungen-", size = 12f)
            b.text(336f, 110f, "venen", size = 12f)
            b.text(130f, 134f, "rechter", size = 12f, bold = c.hl("rechter_vorhof"))
            b.text(130f, 150f, "Vorhof", size = 12f, bold = c.hl("rechter_vorhof"))
            b.text(270f, 134f, "linker", size = 12f, bold = c.hl("linker_vorhof"))
            b.text(270f, 150f, "Vorhof", size = 12f, bold = c.hl("linker_vorhof"))
            b.text(150f, 262f, "rechte", size = 12f, bold = c.hl("rechte_kammer"))
            b.text(150f, 278f, "Kammer", size = 12f, bold = c.hl("rechte_kammer"))
            b.text(258f, 262f, "linke", size = 12f, bold = c.hl("linke_kammer"))
            b.text(258f, 278f, "Kammer", size = 12f, bold = c.hl("linke_kammer"))
            b.text(10f, 362f, "1 Trikuspidalklappe · 2 Pulmonalklappe", size = 11f, anchor = Anchor.START, tone = Tone.MUTED)
            b.text(10f, 376f, "3 Mitralklappe · 4 Aortenklappe", size = 11f, anchor = Anchor.START, tone = Tone.MUTED)
            b.text(10f, 390f, "blau = sauerstoffarm · rot = sauerstoffreich", size = 11f, anchor = Anchor.START, tone = Tone.MUTED)
        } else {
            c.highlight?.let { anchors[it] }?.let { (x, y) -> b.questionBadge(x, y) }
        }
        return b.build(c.description(Figure.HEART_ANATOMY.title, parts(Figure.HEART_ANATOMY)))
    }

    // ---------------------------------------------------------------- Herz-Umriss für Reizleitung/Koronarien

    private const val HEART_OUTLINE =
        "M124,92 Q150,70 204,82 Q262,68 294,94 Q332,124 326,184 Q318,252 232,318 Q210,334 194,318 Q108,250 92,190 Q84,120 124,92 Z"

    private fun heartBase(b: DrawingBuilder, chambers: Boolean) {
        b.path(HEART_OUTLINE, fill = Tone.HEART_SOFT, stroke = Tone.MYOCARD, width = 6f)
        if (chambers) {
            b.path("M96,174 Q200,190 324,172", stroke = Tone.MYOCARD, width = 3f)
            b.line(204f, 84f, 204f, 180f, Tone.MYOCARD, width = 3f)
            b.path("M204,182 Q206,250 212,318", stroke = Tone.MYOCARD, width = 8f)
        }
    }

    // ---------------------------------------------------------------- Erregungsleitung

    private fun conduction(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 350f)
        // Gefäßstümpfe
        b.path("M118,40 L146,40 L146,92 L118,96 Z", fill = Tone.VEIN, stroke = Tone.LINE, width = 1.5f)
        b.path("M212,40 L240,40 L240,80 L212,84 Z", fill = Tone.ARTERY, stroke = Tone.LINE, width = 1.5f)
        heartBase(b, chambers = true)
        if (c.labels) {
            b.text(124f, 150f, "re. Vorhof", Tone.MUTED, 11f)
            b.text(272f, 150f, "li. Vorhof", Tone.MUTED, 11f)
            b.text(150f, 214f, "re. Kammer", Tone.MUTED, 11f)
            b.text(270f, 214f, "li. Kammer", Tone.MUTED, 11f)
        }
        // Internodale Bahnen
        b.path("M140,106 Q150,140 190,166", stroke = Tone.NODE, width = 2f, dashed = true)
        b.path("M140,106 Q186,112 192,160", stroke = Tone.NODE, width = 2f, dashed = true)
        // Purkinje-Fasern
        val pk = c.tone("purkinje", Tone.NODE)
        val pw = c.width("purkinje", 2f)
        b.path("M196,286 Q150,280 118,226", stroke = pk, width = pw)
        b.path("M160,282 Q140,262 142,240", stroke = pk, width = pw)
        b.path("M132,252 Q116,250 108,236", stroke = pk, width = pw)
        b.path("M218,288 Q270,282 304,214", stroke = pk, width = pw)
        b.path("M258,280 Q280,262 280,236", stroke = pk, width = pw)
        b.path("M290,244 Q306,244 314,226", stroke = pk, width = pw)
        // Tawara-Schenkel
        val tw = c.tone("tawara", Tone.NODE)
        b.path("M200,196 Q196,240 198,288", stroke = tw, width = c.width("tawara", 4f))
        b.path("M210,196 Q214,240 216,288", stroke = tw, width = c.width("tawara", 4f))
        // His-Bündel
        b.line(196f, 168f, 205f, 196f, c.tone("his", Tone.NODE), width = c.width("his", 6f))
        // AV-Knoten und Sinusknoten
        b.circle(194f, 166f, if (c.hl("av_knoten")) 11f else 8f, fill = c.tone("av_knoten", Tone.NODE), stroke = Tone.LINE, width = 1.5f)
        b.circle(138f, 104f, if (c.hl("sinusknoten")) 12f else 9f, fill = c.tone("sinusknoten", Tone.NODE), stroke = Tone.LINE, width = 1.5f)

        val anchors = mapOf(
            "sinusknoten" to Pair(110f, 82f), "av_knoten" to Pair(166f, 186f), "his" to Pair(230f, 176f),
            "tawara" to Pair(232f, 252f), "purkinje" to Pair(112f, 270f),
        )
        if (c.labels) {
            fun label(key: String, x: Float, y: Float, sub: String?, anchor: Anchor, lx: Float, ly: Float, tx: Float, ty: Float) {
                b.line(lx, ly, tx, ty, Tone.MUTED, 1.2f)
                b.text(x, y, parts(Figure.CONDUCTION).first { it.key == key }.label, c.tone(key, Tone.INK), 13f, anchor, bold = true)
                sub?.let { b.text(x, y + 16f, it, Tone.MUTED, 11f, anchor) }
            }
            label("sinusknoten", 10f, 24f, "60–100/min", Anchor.START, 60f, 50f, 132f, 98f)
            label("av_knoten", 10f, 196f, "40–60/min", Anchor.START, 76f, 196f, 186f, 170f)
            label("his", 390f, 30f, null, Anchor.END, 330f, 40f, 206f, 186f)
            label("tawara", 390f, 300f, "rechter + linker Schenkel", Anchor.END, 290f, 296f, 216f, 262f)
            label("purkinje", 10f, 300f, "Kammern: 20–40/min", Anchor.START, 84f, 294f, 140f, 262f)
            b.text(390f, 340f, "Zahlen = Eigenfrequenz", Tone.MUTED, 10f, Anchor.END)
        } else {
            c.highlight?.let { anchors[it] }?.let { (x, y) -> b.questionBadge(x, y) }
        }
        return b.build(c.description(Figure.CONDUCTION.title, parts(Figure.CONDUCTION)))
    }

    // ---------------------------------------------------------------- Herzkranzgefäße

    private fun coronaries(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 350f)
        heartBase(b, chambers = false)
        // Aortenwurzel
        b.path("M190,20 L230,20 L230,92 Q210,100 190,92 Z", fill = Tone.ARTERY, stroke = Tone.LINE, width = 1.5f)
        // RCA: nach rechts (im Bild links) in der Herzkranzfurche, dann zur Hinterwand
        b.path("M192,90 Q146,100 118,128 Q92,158 98,206 Q108,256 160,296",
            stroke = c.tone("rca", Tone.ARTERY), width = c.width("rca", 6f))
        b.path("M160,296 Q186,312 204,316", stroke = c.tone("rca", Tone.ARTERY), width = c.width("rca", 4f), dashed = true)
        // Hauptstamm
        b.path("M228,90 Q238,96 248,100", stroke = c.tone("hauptstamm", Tone.ARTERY), width = c.width("hauptstamm", 7f))
        // RIVA/LAD: vorne zwischen den Kammern zur Herzspitze
        b.path("M248,100 Q240,150 232,214 Q226,270 214,314",
            stroke = c.tone("lad", Tone.ARTERY), width = c.width("lad", 6f))
        b.path("M238,170 Q266,186 284,214", stroke = c.tone("lad", Tone.ARTERY), width = c.width("lad", 3f))
        b.path("M232,224 Q252,240 262,262", stroke = c.tone("lad", Tone.ARTERY), width = c.width("lad", 3f))
        // RCX: nach links (im Bild rechts) in der Herzkranzfurche zur Seitenwand
        b.path("M248,100 Q296,108 318,150 Q326,180 316,226",
            stroke = c.tone("rcx", Tone.ARTERY), width = c.width("rcx", 6f))
        b.path("M316,226 Q306,262 282,288", stroke = c.tone("rcx", Tone.ARTERY), width = c.width("rcx", 4f), dashed = true)

        val anchors = mapOf(
            "rca" to Pair(80f, 200f), "hauptstamm" to Pair(250f, 76f), "lad" to Pair(206f, 230f), "rcx" to Pair(346f, 170f),
        )
        if (c.labels) {
            b.text(210f, 10f, "Aorta", Tone.MUTED, 11f)
            b.text(10f, 262f, "RCA", c.tone("rca", Tone.INK), 14f, Anchor.START, bold = true)
            b.text(10f, 278f, "rechte Kranzarterie", Tone.MUTED, 11f, Anchor.START)
            b.text(10f, 294f, "meist Hinterwand", Tone.MUTED, 11f, Anchor.START)
            b.line(46f, 256f, 112f, 236f, Tone.MUTED, 1.2f)
            b.text(262f, 62f, "Hauptstamm", c.tone("hauptstamm", Tone.INK), 13f, Anchor.START, bold = true)
            b.line(260f, 70f, 244f, 96f, Tone.MUTED, 1.2f)
            b.text(390f, 292f, "RIVA / LAD", c.tone("lad", Tone.INK), 14f, Anchor.END, bold = true)
            b.text(390f, 308f, "Vorderwand, Septum", Tone.MUTED, 11f, Anchor.END)
            b.line(310f, 286f, 232f, 250f, Tone.MUTED, 1.2f)
            b.text(390f, 104f, "RCX", c.tone("rcx", Tone.INK), 14f, Anchor.END, bold = true)
            b.text(390f, 120f, "Seitenwand", Tone.MUTED, 11f, Anchor.END)
            b.text(10f, 340f, "von vorne · gestrichelt = Rückseite · vereinfacht", Tone.MUTED, 11f, Anchor.START)
        } else {
            c.highlight?.let { anchors[it] }?.let { (x, y) -> b.questionBadge(x, y) }
        }
        return b.build(c.description(Figure.CORONARIES.title, parts(Figure.CORONARIES)))
    }

    // ---------------------------------------------------------------- EKG-Kurve

    private fun ecgWaves(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 280f)
        val x0 = 30f; val x1 = 370f; val y0 = 30f; val y1 = 190f
        b.path("M$x0,$y0 L$x1,$y0 L$x1,$y1 L$x0,$y1 Z", fill = Tone.PAPER)
        // Raster: kleines Kästchen 8 px = 0,04 s / 0,1 mV
        var i = 0
        var x = x0
        while (x <= x1 + 0.1f) {
            b.line(x, y0, x, y1, if (i % 5 == 0) Tone.GRID_MAJOR else Tone.GRID_MINOR, if (i % 5 == 0) 1.2f else 0.6f)
            x += 8f; i++
        }
        i = 0
        var y = y1
        while (y >= y0 - 0.1f) {
            b.line(x0, y, x1, y, if (i % 5 == 0) Tone.GRID_MAJOR else Tone.GRID_MINOR, if (i % 5 == 0) 1.2f else 0.6f)
            y -= 8f; i++
        }
        // Grundlinie und Kurve
        b.path("M30,150 L62,150 M102,150 L124,150 M158,150 L198,150 M268,150 L370,150", stroke = Tone.TRACE, width = 2.4f)
        b.path("M62,150 C72,150 74,136 82,136 C90,136 92,150 102,150", stroke = c.tone("p", Tone.TRACE), width = c.width("p", 2.4f))
        b.path("M124,150 L130,160 L140,48 L150,174 L158,150", stroke = c.tone("qrs", Tone.TRACE), width = c.width("qrs", 2.4f))
        b.path("M198,150 C213,150 220,118 233,118 C246,118 253,150 268,150", stroke = c.tone("t", Tone.TRACE), width = c.width("t", 2.4f))
        if (c.hl("st")) b.line(158f, 150f, 198f, 150f, Tone.HIGHLIGHT, 4f)

        // Zeiten als Klammern unter dem Raster
        fun bracket(key: String, xa: Float, xb: Float, yy: Float, label: String) {
            if (!c.show(key)) return
            val t = c.tone(key, Tone.INK)
            b.line(xa, yy, xb, yy, t, c.width(key, 1.6f))
            b.line(xa, yy - 5f, xa, yy + 5f, t, 1.6f)
            b.line(xb, yy - 5f, xb, yy + 5f, t, 1.6f)
            b.line(xa, y1, xa, yy - 5f, Tone.MUTED, 0.8f, dashed = true)
            b.line(xb, y1, xb, yy - 5f, Tone.MUTED, 0.8f, dashed = true)
            if (c.labels) b.text((xa + xb) / 2, yy + 14f, label, t, 12f, bold = c.hl(key))
            else b.questionBadge((xa + xb) / 2, yy + 16f, 11f)
        }
        bracket("pq", 62f, 124f, 206f, "PQ")
        bracket("qrs", 124f, 158f, 206f, "QRS")
        bracket("st", 158f, 198f, 206f, "ST")
        bracket("qt", 124f, 268f, 244f, "QT-Zeit")

        if (c.labels) {
            b.text(82f, 122f, "P", c.tone("p", Tone.TRACE), 16f, bold = true)
            b.text(124f, 168f, "Q", Tone.TRACE, 13f, bold = true)
            b.text(140f, 38f, "R", c.tone("qrs", Tone.TRACE), 16f, bold = true)
            b.text(158f, 178f, "S", Tone.TRACE, 13f, bold = true)
            b.text(233f, 102f, "T", c.tone("t", Tone.TRACE), 16f, bold = true)
            b.text(370f, 14f, "1 großes Kästchen = 0,2 s", Tone.MUTED, 11f, Anchor.END)
            b.text(30f, 14f, "25 mm/s", Tone.MUTED, 11f, Anchor.START)
        } else {
            when (c.highlight) {
                "p" -> b.questionBadge(82f, 112f, 11f)
                "qrs" -> b.questionBadge(166f, 70f, 11f)
                "t" -> b.questionBadge(233f, 96f, 11f)
            }
        }
        return b.build(c.description(Figure.ECG_WAVES.title, parts(Figure.ECG_WAVES)))
    }

    // ---------------------------------------------------------------- Elektroden

    private fun electrodes(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 410f)
        // Oberkörper von vorne
        b.path("M180,6 L220,6 L224,48 L176,48 Z", fill = Tone.SKIN)
        b.path("M78,104 Q84,58 148,50 L180,44 L220,44 L252,50 Q316,58 322,104 L306,250 Q298,312 310,384 L90,384 Q102,312 94,250 Z", fill = Tone.SKIN, stroke = Tone.SKIN_DARK, width = 2f)
        // Schlüsselbeine, Brustbein, Rippenbögen
        b.path("M196,62 Q160,56 116,68", stroke = Tone.SKIN_DARK, width = 3f)
        b.path("M204,62 Q240,56 284,68", stroke = Tone.SKIN_DARK, width = 3f)
        b.line(200f, 64f, 200f, 222f, Tone.SKIN_DARK, 5f)
        b.path("M120,300 Q150,250 200,226 Q250,250 280,300", stroke = Tone.SKIN_DARK, width = 3f)
        // Merkhilfe Ampel: im Uhrzeigersinn ab rechter Schulter
        if (c.labels) {
            b.path("M152,80 Q200,62 246,80", stroke = Tone.MUTED, width = 2f, dashed = true)
            b.arrowHead(250f, 82f, 0.9f, 0.43f, Tone.MUTED, 8f)
            b.path("M292,114 Q312,210 290,296", stroke = Tone.MUTED, width = 2f, dashed = true)
            b.arrowHead(288f, 302f, -0.3f, 0.95f, Tone.MUTED, 8f)
        }
        data class El(val key: String, val x: Float, val y: Float, val tone: Tone, val letter: String, val word: String, val letterTone: Tone)
        val els = listOf(
            El("rot", 132f, 92f, Tone.EL_RED, "R", "rot", Tone.WHITE),
            El("gelb", 268f, 92f, Tone.EL_YELLOW, "L", "gelb", Tone.BLACK),
            El("gruen", 272f, 322f, Tone.EL_GREEN, "F", "grün", Tone.WHITE),
            El("schwarz", 128f, 322f, Tone.EL_BLACK, "N", "schwarz", Tone.WHITE),
            El("weiss", 184f, 172f, Tone.EL_WHITE, "C", "weiß (V1)", Tone.BLACK),
        )
        els.forEach { e ->
            if (!c.labels && c.hl(e.key)) {
                b.circle(e.x, e.y, 18f, fill = Tone.WHITE, stroke = Tone.HIGHLIGHT, width = 4f)
                b.questionBadge(e.x, e.y, 12f)
                return@forEach
            }
            b.circle(e.x, e.y, if (c.hl(e.key)) 17f else 14f, fill = e.tone, stroke = if (c.hl(e.key)) Tone.HIGHLIGHT else Tone.LINE, width = if (c.hl(e.key)) 4f else 1.5f)
            b.circle(e.x, e.y, 4f, fill = Tone.MUTED)
            if (c.labels) {
                b.text(e.x + 15f, e.y - 16f, e.letter, Tone.INK, 11f, bold = true)
            }
        }
        if (c.labels) {
            b.text(76f, 92f, "rot", c.tone("rot", Tone.INK), 13f, Anchor.END, bold = true)
            b.text(324f, 92f, "gelb", c.tone("gelb", Tone.INK), 13f, Anchor.START, bold = true)
            b.text(328f, 322f, "grün", c.tone("gruen", Tone.INK), 13f, Anchor.START, bold = true)
            b.text(72f, 322f, "schwarz", c.tone("schwarz", Tone.INK), 13f, Anchor.END, bold = true)
            b.line(80f, 172f, 168f, 172f, Tone.MUTED, 1.2f)
            b.text(76f, 166f, "weiß", c.tone("weiss", Tone.INK), 13f, Anchor.END, bold = true)
            b.text(76f, 182f, "V1", Tone.MUTED, 11f, Anchor.END)
            b.text(200f, 398f, "Ampel: rot → gelb → grün im Uhrzeigersinn", Tone.MUTED, 11f)
        }
        b.text(14f, 16f, "rechts", Tone.MUTED, 11f, Anchor.START)
        b.text(386f, 16f, "links", Tone.MUTED, 11f, Anchor.END)
        b.text(14f, 30f, "(Patient)", Tone.MUTED, 10f, Anchor.START)
        b.text(386f, 30f, "(Patient)", Tone.MUTED, 10f, Anchor.END)
        return b.build(c.description(Figure.ELECTRODES.title, parts(Figure.ELECTRODES)))
    }

    // ---------------------------------------------------------------- Körper von vorne

    private fun personFront(b: DrawingBuilder, cx: Float, top: Float) {
        val t = top
        b.circle(cx, t + 28f, 28f, fill = Tone.SKIN)
        b.path("M${cx - 10},${t + 50} L${cx + 10},${t + 50} L${cx + 10},${t + 72} L${cx - 10},${t + 72} Z", fill = Tone.SKIN)
        b.line(cx - 46f, t + 82f, cx - 70f, t + 204f, Tone.SKIN, 20f)
        b.line(cx + 46f, t + 82f, cx + 70f, t + 204f, Tone.SKIN, 20f)
        b.circle(cx - 72f, t + 212f, 11f, fill = Tone.SKIN)
        b.circle(cx + 72f, t + 212f, 11f, fill = Tone.SKIN)
        b.line(cx - 22f, t + 218f, cx - 26f, t + 352f, Tone.SKIN, 24f)
        b.line(cx + 22f, t + 218f, cx + 26f, t + 352f, Tone.SKIN, 24f)
        b.path(
            "M${cx - 52},${t + 78} Q${cx - 52},${t + 68} ${cx - 40},${t + 68} L${cx + 40},${t + 68} " +
                "Q${cx + 52},${t + 68} ${cx + 52},${t + 78} L${cx + 46},${t + 196} Q${cx + 44},${t + 218} ${cx + 34},${t + 222} " +
                "L${cx - 34},${t + 222} Q${cx - 44},${t + 218} ${cx - 46},${t + 196} Z",
            fill = Tone.SKIN,
        )
        // Herz: links im Körper = rechts im Bild
        val hx = cx + 14f
        val hy = t + 128f
        b.path(
            "M$hx,${hy + 12} C${hx - 14},${hy + 2} ${hx - 16},${hy - 10} ${hx - 8},${hy - 13} " +
                "C${hx - 3},${hy - 15} $hx,${hy - 11} $hx,${hy - 8} C$hx,${hy - 11} ${hx + 3},${hy - 15} ${hx + 8},${hy - 13} " +
                "C${hx + 16},${hy - 10} ${hx + 14},${hy + 2} $hx,${hy + 12} Z",
            fill = Tone.ARTERY,
        )
    }

    private fun bodyFront(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 480f)
        val cx = 200f
        personFront(b, cx, 94f)
        // Medianlinie
        if (c.show("median")) {
            b.line(cx, 86f, cx, 462f, c.tone("median", Tone.MUTED), c.width("median", 1.8f), dashed = true)
        }
        fun dirArrow(key: String, x1: Float, y1: Float, x2: Float, y2: Float, lx: Float, ly: Float, sub: String, anchor: Anchor = Anchor.MIDDLE) {
            if (!c.show(key)) return
            b.arrow(x1, y1, x2, y2, c.tone(key, Tone.ACCENT), width = c.width(key, 3.5f), head = 13f)
            if (c.labels) {
                b.text(lx, ly, parts(Figure.BODY_FRONT).first { it.key == key }.label.substringBefore(" ("), c.tone(key, Tone.INK), 15f, anchor, bold = true)
                b.text(lx, ly + 16f, sub, Tone.MUTED, 11f, anchor)
            } else {
                b.questionBadge(lx, ly + 6f)
            }
        }
        dirArrow("dexter", 170f, 42f, 50f, 42f, 100f, 14f, "rechts")
        dirArrow("sinister", 230f, 42f, 350f, 42f, 300f, 14f, "links")
        dirArrow("kranial", 362f, 250f, 362f, 120f, 362f, 90f, "kopfwärts")
        dirArrow("kaudal", 362f, 300f, 362f, 430f, 362f, 444f, "fußwärts")
        dirArrow("medial", 40f, 372f, 150f, 372f, 92f, 340f, "zur Mitte")
        dirArrow("lateral", 254f, 372f, 330f, 372f, 290f, 340f, "zur Seite")
        b.text(cx, 70f, "aus Sicht der Person", Tone.MUTED, 11f)
        if (c.labels) {
            b.text(cx, 474f, "Medianlinie", c.tone("median", Tone.MUTED), 11f, bold = c.hl("median"))
        } else if (c.hl("median")) {
            b.questionBadge(cx, 452f)
        }
        return b.build(c.description(Figure.BODY_FRONT.title, parts(Figure.BODY_FRONT)) + " Die Person schaut dich an.")
    }

    // ---------------------------------------------------------------- Körper von der Seite

    private fun bodySide(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 480f)
        // Person im Profil, Blick nach rechts
        b.circle(200f, 122f, 28f, fill = Tone.SKIN)
        b.path("M226,112 L238,126 L226,132 Z", fill = Tone.SKIN)
        b.circle(214f, 116f, 2.8f, fill = Tone.INK)
        b.path("M190,146 L210,146 L212,168 L188,168 Z", fill = Tone.SKIN)
        b.line(204f, 312f, 204f, 444f, Tone.SKIN, 26f)
        b.line(204f, 452f, 228f, 452f, Tone.SKIN, 12f)
        b.path(
            "M184,162 L222,162 Q238,170 238,200 Q240,240 230,268 Q234,298 226,318 L180,318 Q170,290 176,258 Q168,222 176,190 Q176,166 184,162 Z",
            fill = Tone.SKIN,
        )
        b.line(200f, 178f, 206f, 296f, Tone.SKIN_DARK, 16f)
        b.circle(207f, 304f, 9f, fill = Tone.SKIN_DARK)
        fun dirArrow(key: String, x1: Float, y1: Float, x2: Float, y2: Float, lx: Float, ly: Float, title: String, sub: String) {
            if (!c.show(key)) return
            b.arrow(x1, y1, x2, y2, c.tone(key, Tone.ACCENT), width = c.width(key, 3.5f), head = 13f)
            if (c.labels) {
                b.text(lx, ly, title, c.tone(key, Tone.INK), 14f, bold = true)
                b.text(lx, ly + 16f, sub, Tone.MUTED, 11f)
            } else {
                b.questionBadge(lx, ly + 6f)
            }
        }
        dirArrow("ventral", 252f, 236f, 372f, 236f, 316f, 198f, "ventral / anterior", "bauchwärts, vorne")
        dirArrow("dorsal", 158f, 236f, 34f, 236f, 90f, 198f, "dorsal / posterior", "rückenwärts, hinten")
        dirArrow("superior", 316f, 160f, 316f, 46f, 316f, 20f, "superior", "oben")
        dirArrow("inferior", 316f, 300f, 316f, 420f, 316f, 438f, "inferior", "unten")
        return b.build(c.description(Figure.BODY_SIDE.title, parts(Figure.BODY_SIDE)) + " Die Person schaut nach rechts.")
    }

    // ---------------------------------------------------------------- Arm: proximal/distal

    private fun limb(c: Ctx): Drawing {
        val b = DrawingBuilder(400f, 250f)
        // Rumpf mit Kopf, Arm waagerecht zur Seite
        b.circle(48f, 34f, 24f, fill = Tone.SKIN)
        b.path("M10,74 Q10,60 24,60 L76,60 Q94,62 96,84 L96,250 L10,250 Z", fill = Tone.SKIN)
        b.line(96f, 92f, 200f, 100f, Tone.SKIN, 26f)
        b.line(200f, 100f, 302f, 106f, Tone.SKIN, 21f)
        b.circle(318f, 107f, 15f, fill = Tone.SKIN)
        b.line(326f, 98f, 350f, 96f, Tone.SKIN, 8f)
        b.line(328f, 106f, 356f, 106f, Tone.SKIN, 8f)
        b.line(328f, 114f, 352f, 116f, Tone.SKIN, 8f)
        b.line(316f, 94f, 330f, 82f, Tone.SKIN, 8f)
        b.circle(98f, 92f, 5f, fill = Tone.SKIN_DARK)
        b.circle(200f, 100f, 5f, fill = Tone.SKIN_DARK)
        b.circle(304f, 106f, 5f, fill = Tone.SKIN_DARK)
        if (c.labels) {
            b.text(120f, 64f, "Schulter", Tone.MUTED, 11f)
            b.text(200f, 72f, "Ellenbogen", Tone.MUTED, 11f)
            b.text(304f, 72f, "Handgelenk", Tone.MUTED, 11f)
        }
        fun dirArrow(key: String, x1: Float, x2: Float, lx: Float, title: String, sub: String) {
            if (!c.show(key)) return
            b.arrow(x1, 160f, x2, 160f, c.tone(key, Tone.ACCENT), width = c.width(key, 3.5f), head = 13f)
            if (c.labels) {
                b.text(lx, 188f, title, c.tone(key, Tone.INK), 15f, bold = true)
                b.text(lx, 206f, sub, Tone.MUTED, 11f)
            } else {
                b.questionBadge(lx, 192f)
            }
        }
        dirArrow("proximal", 192f, 108f, 150f, "proximal", "zum Rumpf hin")
        dirArrow("distal", 208f, 346f, 277f, "distal", "vom Rumpf weg")
        if (c.labels) b.text(200f, 236f, "Der Ellenbogen liegt proximal der Hand.", Tone.MUTED, 11f)
        return b.build(c.description(Figure.LIMB.title, parts(Figure.LIMB)))
    }

    // ---------------------------------------------------------------- Lagerungen

    private fun positioning(c: Ctx): Drawing {
        val keys = c.highlight?.let { listOf(it) } ?: parts(Figure.POSITIONING).map { it.key }
        val sceneH = 220f
        val b = DrawingBuilder(400f, sceneH * keys.size)
        keys.forEachIndexed { i, key -> positionScene(b, c, key, i * sceneH) }
        return b.build(c.description(Figure.POSITIONING.title, parts(Figure.POSITIONING)))
    }

    private fun positionScene(b: DrawingBuilder, c: Ctx, key: String, dy: Float) {
        if (!c.labels && c.hl(key)) b.questionBadge(370f, 30f + dy)
        // Matratze als Linienzug vom Kopfende (links) über die Hüfte bis zu den Füßen
        val hip = Pair(210f, 130f + dy)
        fun polar(from: Pair<Float, Float>, len: Float, deg: Double) =
            Pair(from.first + (len * cos(Math.toRadians(deg))).toFloat(), from.second - (len * sin(Math.toRadians(deg))).toFloat())
        val (headEnd, legs) = when (key) {
            "flach" -> Pair(polar(hip, 150f, 180.0), listOf(polar(hip, 150f, 0.0)))
            "oberkoerper" -> Pair(polar(hip, 150f, 150.0), listOf(polar(hip, 150f, 0.0)))
            else -> {
                val knee = polar(hip, 75f, -12.0)
                Pair(polar(hip, 105f, 115.0), listOf(knee, polar(knee, 40f, -60.0)))
            }
        }
        // Bettgestell
        b.line(50f, 190f + dy, 370f, 190f + dy, Tone.LINE, 4f)
        b.line(64f, 190f + dy, 64f, 208f + dy, Tone.LINE, 4f)
        b.line(356f, 190f + dy, 356f, 208f + dy, Tone.LINE, 4f)
        b.circle(64f, 212f + dy, 5f, fill = Tone.LINE)
        b.circle(356f, 212f + dy, 5f, fill = Tone.LINE)
        b.line(hip.first, hip.second + 6f, hip.first, 190f + dy, Tone.LINE, 3f)
        val mattress = listOf(headEnd, hip) + legs
        val tone = if (c.hl(key)) Tone.HIGHLIGHT else Tone.BED
        mattress.zipWithNext().forEach { (p, q) -> b.line(p.first, p.second, q.first, q.second, tone, 12f) }
        // Person, etwas oberhalb der Matratze
        fun offset(p: Pair<Float, Float>, q: Pair<Float, Float>, d: Float): Pair<Pair<Float, Float>, Pair<Float, Float>> {
            val len = hypot(q.first - p.first, q.second - p.second)
            var nx = (q.second - p.second) / len
            var ny = -(q.first - p.first) / len
            if (ny > 0) { nx = -nx; ny = -ny }
            return Pair(Pair(p.first + nx * d, p.second + ny * d), Pair(q.first + nx * d, q.second + ny * d))
        }
        val (tHip, tHead) = offset(hip, headEnd, 20f)
        val shoulder = Pair(tHip.first + (tHead.first - tHip.first) * 0.72f, tHip.second + (tHead.second - tHip.second) * 0.72f)
        b.line(tHip.first, tHip.second, shoulder.first, shoulder.second, Tone.SKIN, 30f)
        val headC = Pair(tHip.first + (tHead.first - tHip.first) * 0.95f, tHip.second + (tHead.second - tHip.second) * 0.95f)
        b.circle(headC.first, headC.second, 17f, fill = Tone.SKIN)
        var prev = hip
        legs.forEach { q ->
            val (a, z) = offset(prev, q, 13f)
            b.line(a.first, a.second, z.first, z.second, Tone.SKIN, 20f)
            prev = q
        }
        if (c.labels) {
            val (title, sub) = when (key) {
                "flach" -> Pair("Flachlage", "Rückenlage, Kopfteil flach")
                "oberkoerper" -> Pair("Oberkörperhochlage", "Kopfteil ca. 30° hoch")
                else -> Pair("Herzbettlage", "Oberkörper hoch, Beine tief")
            }
            b.text(384f, 22f + dy, title, c.tone(key, Tone.INK), 15f, Anchor.END, bold = true)
            b.text(384f, 40f + dy, sub, Tone.MUTED, 11f, Anchor.END)
            if (key == "oberkoerper") {
                b.path("M${hip.first - 50},${hip.second} A50,50 0 0 1 ${hip.first - 43.3f},${hip.second - 25}", stroke = Tone.MUTED, width = 1.5f)
                b.text(hip.first - 70f, hip.second - 12f, "30°", Tone.MUTED, 11f)
            }
        }
    }
}
