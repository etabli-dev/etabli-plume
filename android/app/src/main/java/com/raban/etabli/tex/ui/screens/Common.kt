// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.raban.etabli.tex.ui.theme.Coder

@Composable
fun TextInput(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = Coder.tokens
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(t.radius.sm))
            .background(t.color.paper)
            .border(1.dp, t.color.hairline, RoundedCornerShape(t.radius.sm))
            .padding(horizontal = t.space.md, vertical = t.space.sm),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = t.font.body.copy(color = t.color.faint))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = t.font.body.copy(color = t.color.ink),
            cursorBrush = SolidColor(t.color.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val t = Coder.tokens
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(t.radius.sm))
            .background(if (selected) t.color.accent.copy(alpha = 0.12f) else t.color.surface)
            .border(
                1.dp,
                if (selected) t.color.accent else t.color.hairline,
                RoundedCornerShape(t.radius.sm),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = t.space.md, vertical = t.space.sm),
    ) {
        Text(
            label,
            style = t.font.caption.copy(color = if (selected) t.color.accent else t.color.faint),
        )
    }
}

@Composable
fun Hairline() {
    val t = Coder.tokens
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(t.color.hairline))
}

@Composable
fun CodeBlock(text: String) {
    val t = Coder.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(t.radius.sm))
            .background(t.color.paper)
            .border(1.dp, t.color.hairline, RoundedCornerShape(t.radius.sm))
            .padding(t.space.md),
    ) {
        Text(text, style = t.font.mono.copy(color = t.color.ink))
    }
}


@Composable
fun CopyButton(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(value) { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
    TextButton(onClick = { clipboard.setText(AnnotatedString(value)); copied = true }) {
        Text(if (copied) "Copied" else label)
    }
}
