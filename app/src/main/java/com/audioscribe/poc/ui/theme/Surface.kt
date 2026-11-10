package com.audioscribe.poc.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AppSurface(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surface, content: @Composable () -> Unit) {
    Surface(modifier = modifier, color = color, content = content)
}
