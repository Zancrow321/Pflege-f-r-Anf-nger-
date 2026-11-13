package de.pflegefueranfaenger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.core.ecg.EcgGenerator
import de.pflegefueranfaenger.core.model.Rhythm

/**
 * Zeichnet einen schematischen EKG-Streifen (6 s) auf rosa EKG-Papier.
 * Raster: kleines Kästchen = 0,04 s / 0,1 mV, großes Kästchen = 0,2 s / 0,5 mV (wie 25 mm/s, 10 mm/mV).
 */
@Composable
fun EcgStrip(rhythm: Rhythm, modifier: Modifier = Modifier, showLabel: Boolean = false) {
    val seconds = 6.0
    val samples = remember(rhythm) { EcgGenerator.generate(rhythm, seconds) }
    val description = if (showLabel) "EKG-Streifen: ${rhythm.label}" else "EKG-Streifen"
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF4F4))
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val pxPerSecond = size.width / seconds.toFloat()
            val pxPerMv = pxPerSecond * 0.4f // 10 mm/mV bei 25 mm/s
            val small = pxPerSecond * 0.04f
            val minor = Color(0xFFF8C8C8)
            val major = Color(0xFFEE9A9A)
            var i = 0
            var x = 0f
            while (x <= size.width) {
                drawLine(if (i % 5 == 0) major else minor, Offset(x, 0f), Offset(x, size.height), strokeWidth = if (i % 5 == 0) 1.5f else 0.7f)
                x += small; i++
            }
            val baseline = size.height * 0.62f
            var j = 0
            var y = baseline
            while (y >= 0) {
                drawLine(if (j % 5 == 0) major else minor, Offset(0f, y), Offset(size.width, y), strokeWidth = if (j % 5 == 0) 1.5f else 0.7f)
                y -= pxPerMv * 0.1f; j++
            }
            j = 1
            y = baseline + pxPerMv * 0.1f
            while (y <= size.height) {
                drawLine(if (j % 5 == 0) major else minor, Offset(0f, y), Offset(size.width, y), strokeWidth = if (j % 5 == 0) 1.5f else 0.7f)
                y += pxPerMv * 0.1f; j++
            }
            val path = Path()
            samples.forEachIndexed { idx, v ->
                val px = idx.toFloat() / EcgGenerator.SAMPLE_RATE * pxPerSecond
                val py = (baseline - v * pxPerMv).coerceIn(0f, size.height)
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, Color(0xFF1B1B1B), style = Stroke(width = 2.2f, join = StrokeJoin.Round))
        }
    }
}
