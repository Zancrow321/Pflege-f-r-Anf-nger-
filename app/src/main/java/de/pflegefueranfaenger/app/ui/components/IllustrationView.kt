package de.pflegefueranfaenger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.core.figure.Anchor
import de.pflegefueranfaenger.core.figure.Drawing
import de.pflegefueranfaenger.core.figure.Element
import de.pflegefueranfaenger.core.figure.Figures
import de.pflegefueranfaenger.core.figure.Palette
import de.pflegefueranfaenger.core.figure.Tone
import de.pflegefueranfaenger.core.model.Illustration

/**
 * Zeigt ein Schaubild. Mit [interactive] erscheinen darunter Chips, mit denen man
 * einzelne Teile hervorheben kann (z.B. „Mitralklappe“ antippen → Klappe leuchtet).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustrationView(illustration: Illustration, modifier: Modifier = Modifier, interactive: Boolean = false) {
    var highlight by rememberSaveable(illustration) { mutableStateOf(illustration.highlight) }
    val drawing = remember(illustration, highlight) { Figures.draw(illustration.figure, highlight, illustration.labels) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DrawingCanvas(drawing)
        illustration.caption?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (interactive && illustration.labels) {
            Text("Antippen zum Hervorheben:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Figures.parts(illustration.figure).forEach { p ->
                    FilterChip(
                        selected = highlight == p.key,
                        onClick = { highlight = if (highlight == p.key) null else p.key },
                        label = { Text(p.label) },
                    )
                }
            }
        }
    }
}

/** Zeichnet eine [Drawing] aus dem Kern gleichmäßig skaliert auf die verfügbare Breite. */
@Composable
fun DrawingCanvas(drawing: Drawing, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = if (dark) Palette.dark else Palette.light
    fun color(t: Tone): Color = Color(0xFF000000L or palette.getValue(t))
    val measurer = rememberTextMeasurer()
    val paths = remember(drawing) {
        drawing.elements.filterIsInstance<Element.PathEl>().associateWith { PathParser().parsePathString(it.d).toPath() }
    }
    val shape = RoundedCornerShape(16.dp)
    Canvas(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color(Tone.PANEL))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(8.dp)
            .aspectRatio(drawing.width / drawing.height)
            .semantics { contentDescription = drawing.description },
    ) {
        val s = size.width / drawing.width
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            drawing.elements.forEach { e ->
                when (e) {
                    is Element.PathEl -> {
                        val p = paths.getValue(e)
                        e.fill?.let { drawPath(p, color(it), style = Fill) }
                        e.stroke?.let {
                            drawPath(
                                p, color(it),
                                style = Stroke(
                                    width = e.width, cap = StrokeCap.Round, join = StrokeJoin.Round,
                                    pathEffect = if (e.dashed) PathEffect.dashPathEffect(floatArrayOf(e.width * 3, e.width * 2.5f)) else null,
                                ),
                            )
                        }
                    }
                    is Element.LineEl -> drawLine(
                        color(e.tone), Offset(e.x1, e.y1), Offset(e.x2, e.y2), strokeWidth = e.width, cap = StrokeCap.Round,
                        pathEffect = if (e.dashed) PathEffect.dashPathEffect(floatArrayOf(e.width * 3, e.width * 2.5f)) else null,
                    )
                    is Element.CircleEl -> {
                        e.fill?.let { drawCircle(color(it), e.r, Offset(e.cx, e.cy)) }
                        e.stroke?.let { drawCircle(color(it), e.r, Offset(e.cx, e.cy), style = Stroke(e.width)) }
                    }
                    is Element.TextEl -> {
                        // Schriftgröße in Zeichnungseinheiten; die Skalierung übernimmt die Transformation.
                        val style = TextStyle(
                            color = color(e.tone),
                            fontSize = e.size.toSp(),
                            fontWeight = if (e.bold) FontWeight.Bold else FontWeight.Normal,
                        )
                        val layout = measurer.measure(e.text, style)
                        val w = layout.size.width.toFloat()
                        val x = when (e.anchor) {
                            Anchor.START -> e.x
                            Anchor.MIDDLE -> e.x - w / 2
                            Anchor.END -> e.x - w
                        }
                        drawText(layout, topLeft = Offset(x, e.y - layout.size.height / 2f))
                    }
                }
            }
        }
    }
}
