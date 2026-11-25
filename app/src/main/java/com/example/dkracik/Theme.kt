package com.example.dkracik

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF120E26)
val CardBg = Color(0xFF1C1740)
val CardHi = Color(0xFF2A2360)
val Accent = Color(0xFF7B61FF)
val Gold = Color(0xFFD8B25C)
val Line = Color(0xFF3A3270)
val Muted = Color(0xFFA7A2C9)
val Txt = Color(0xFFF1EFFF)
val PokeRed = Color(0xFFEE1515)

@Composable
fun Ball(sz: Dp) {
    Canvas(Modifier.size(sz)) {
        val r = size.minDimension / 2f
        drawArc(PokeRed, 180f, 180f, true)
        drawArc(Color.White, 0f, 180f, true)
        drawCircle(Color.Black, r, style = Stroke(width = r * 0.12f))
        drawLine(Color.Black, Offset(0f, r), Offset(size.width, r), strokeWidth = r * 0.14f)
        drawCircle(Color.Black, r * 0.3f)
        drawCircle(Color.White, r * 0.17f)
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Txt, unfocusedTextColor = Txt,
    focusedBorderColor = Accent, unfocusedBorderColor = Line,
    focusedLabelColor = Accent, unfocusedLabelColor = Muted, cursorColor = Accent
)

@Composable
fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.White, selectedTextColor = Color.White, indicatorColor = Accent,
    unselectedIconColor = Muted, unselectedTextColor = Muted
)

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CardBg).border(1.dp, Line, shape).padding(16.dp),
        content = content
    )
}

@Composable
fun Seg(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Bg).border(1.dp, Line, shape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (v, l) ->
            val on = v == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (on) Accent else Color.Transparent)
                    .clickable { onPick(v) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    l, fontSize = 13.sp, color = if (on) Color.White else Muted,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
