// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

package com.raban.etabli.tex.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raban.etabli.tex.data.CatalogItem
import com.raban.etabli.tex.data.Section
import com.raban.etabli.tex.desktop.APP_NAME
import com.raban.etabli.tex.desktop.PlumeState
import com.raban.etabli.tex.desktop.altLabel
import com.raban.etabli.tex.desktop.appVersion
import com.raban.etabli.tex.desktop.isMacOs
import com.raban.etabli.tex.desktop.keyAction
import com.raban.etabli.tex.desktop.perform
import com.raban.etabli.tex.desktop.primaryLabel
import com.raban.etabli.tex.desktop.ui.theme.Plume
import kotlinx.coroutines.delay

/** Focus of the search field, which drives list navigation. */
class UiFocus {
    val search = FocusRequester()
    var searchFocused by mutableStateOf(false)

    fun focusSearch() {
        runCatching { search.requestFocus() }
    }
}

fun handleWindowKey(event: KeyEvent, state: PlumeState, ui: UiFocus): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val primary = if (isMacOs) event.isMetaPressed else event.isCtrlPressed
    val action = keyAction(event.key, primary, event.isAltPressed, event.isShiftPressed, ui.searchFocused)
        ?: return false
    return state.perform(action, ui::focusSearch)
}

@Composable
fun PlumeScreen(state: PlumeState, ui: UiFocus) {
    val t = Plume.tokens
    LaunchedEffect(Unit) { ui.focusSearch() }
    Column(Modifier.fillMaxSize().background(t.color.background)) {
        TopBar(state, ui)
        Hairline()
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Sidebar(state, ui, Modifier.width(236.dp).fillMaxHeight())
            VerticalHairline()
            Column(Modifier.weight(0.46f).fillMaxHeight()) {
                SearchField(state, ui)
                ResultList(state, ui, Modifier.weight(1f))
            }
            VerticalHairline()
            DetailPane(state, ui, Modifier.weight(0.54f).fillMaxHeight())
        }
        Hairline()
        StatusBar(state)
    }
}

@Composable
private fun TopBar(state: PlumeState, ui: UiFocus) {
    val t = Plume.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(t.space.sm),
        modifier = Modifier.fillMaxWidth().background(t.color.surface)
            .padding(horizontal = t.space.lg, vertical = t.space.sm),
    ) {
        Text("~/", style = t.type.monoCallout.copy(color = t.color.accentText))
        Text("plume", style = t.type.monoCallout)
        Text("/", style = t.type.monoCallout.copy(color = t.color.faint))
        Text(state.section.label.lowercase(), style = t.type.monoCallout)
        Spacer(Modifier.weight(1f))
        PillButton(
            label = if (state.preferences.alwaysOnTop) "on top ✓" else "on top",
            hint = "Keep the window above other windows (${primaryLabel}T)",
            selected = state.preferences.alwaysOnTop,
        ) { state.toggleAlwaysOnTop(); ui.focusSearch() }
        PillButton(
            label = "theme: ${state.preferences.theme.name.lowercase()}",
            hint = "Switch between system, light and dark",
        ) { state.cycleTheme(); ui.focusSearch() }
    }
}

@Composable
private fun Sidebar(state: PlumeState, ui: UiFocus, modifier: Modifier) {
    val t = Plume.tokens
    Column(modifier.background(t.color.surface)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = t.space.sm)) {
            val firstCategory = state.catalog.sections.firstOrNull { it is Section.Category }
            items(state.catalog.sections, key = { it.toString() }) { section ->
                if (section == firstCategory) SidebarCaption("commands")
                if (section == Section.Snippets) SidebarCaption("snippets")
                SectionRow(section, state.count(section), selected = section == state.section) {
                    state.showSection(section)
                    ui.focusSearch()
                }
            }
        }
        Hairline()
        Column(
            Modifier.fillMaxWidth().padding(horizontal = t.space.md, vertical = t.space.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("$APP_NAME $appVersion", style = t.type.caption.copy(color = t.color.text))
            Text("offline · no network · no account", style = t.type.caption)
            Text("Not affiliated with the LaTeX Project or TeX.", style = t.type.caption)
        }
    }
}

@Composable
private fun SidebarCaption(text: String) {
    val t = Plume.tokens
    Text(
        text,
        style = t.type.caption,
        modifier = Modifier.padding(start = t.space.md, top = t.space.md, bottom = t.space.xs),
    )
}

@Composable
private fun SectionRow(section: Section, count: Int, selected: Boolean, onClick: () -> Unit) {
    val t = Plume.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) t.color.accentMuted else t.color.surface)
            .height(30.dp),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (selected) t.color.accent else t.color.surface))
        Text(
            section.label,
            style = t.type.body.copy(color = if (selected) t.color.accentText else t.color.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = t.space.md - 3.dp),
        )
        Text(
            count.toString(),
            style = t.type.caption.copy(color = if (count == 0) t.color.faint.copy(alpha = 0.5f) else t.color.faint),
            modifier = Modifier.padding(end = t.space.md),
        )
    }
}

@Composable
private fun SearchField(state: PlumeState, ui: UiFocus) {
    val t = Plume.tokens
    val shape = RoundedCornerShape(t.radius.sm)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(t.space.md)
            .clip(shape)
            .background(t.color.background)
            .border(1.dp, if (ui.searchFocused) t.color.accent else t.color.border, shape)
            .padding(horizontal = t.space.md, vertical = 10.dp),
    ) {
        Text("/", style = t.type.mono.copy(color = t.color.accentText))
        Spacer(Modifier.width(t.space.sm))
        Box(Modifier.weight(1f)) {
            if (state.query.isEmpty()) {
                Text(
                    "search commands, symbols, packages",
                    style = t.type.mono.copy(color = t.color.faint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = state.query,
                onValueChange = state::updateQuery,
                singleLine = true,
                textStyle = t.type.mono,
                cursorBrush = SolidColor(t.color.accent),
                modifier = Modifier.fillMaxWidth()
                    .focusRequester(ui.search)
                    .onFocusChanged { ui.searchFocused = it.isFocused }
                    .semantics { contentDescription = "Search" },
            )
        }
        Spacer(Modifier.width(t.space.sm))
        Text("${state.results.size}/${state.count(state.section)}", style = t.type.caption)
    }
}

@Composable
private fun ResultList(state: PlumeState, ui: UiFocus, modifier: Modifier) {
    val t = Plume.tokens
    val results = state.results
    val listState = rememberLazyListState()
    val selectedIndex = state.selectedIndex

    // Keep the keyboard selection visible.
    LaunchedEffect(selectedIndex, results) {
        if (selectedIndex < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val item = visible.firstOrNull { it.index == selectedIndex }
        when {
            item == null && selectedIndex < visible.first().index -> listState.scrollToItem(selectedIndex)
            item == null -> listState.scrollToItem((selectedIndex - visible.size + 2).coerceAtLeast(0))
            item.offset < info.viewportStartOffset ->
                listState.scrollBy((item.offset - info.viewportStartOffset).toFloat())
            item.offset + item.size > info.viewportEndOffset ->
                listState.scrollBy((item.offset + item.size - info.viewportEndOffset).toFloat())
        }
    }

    if (results.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(t.space.lg), contentAlignment = Alignment.TopStart) {
            Column(verticalArrangement = Arrangement.spacedBy(t.space.xs)) {
                when {
                    state.loadError != null -> Text(state.loadError, style = t.type.body.copy(color = t.color.error))
                    state.section == Section.Favourites && state.query.isBlank() -> {
                        Text("No favourites yet.", style = t.type.body)
                        Text("Press ${primaryLabel}D on a command to keep it here.", style = t.type.caption)
                    }
                    else -> {
                        Text("Nothing matches “${state.query.trim()}”.", style = t.type.body)
                        Text("Press Esc to clear the search or ${altLabel}↑↓ to change section.", style = t.type.caption)
                    }
                }
            }
        }
        return
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        itemsIndexed(results, key = { _, item -> item.key }) { index, item ->
            ResultRow(
                item = item,
                selected = index == selectedIndex,
                favourite = state.isFavourite(item),
                onClick = { state.select(index); ui.focusSearch() },
                onDoubleClick = { state.select(index); state.copySelected(); ui.focusSearch() },
            )
        }
    }
}

@Composable
private fun ResultRow(
    item: CatalogItem,
    selected: Boolean,
    favourite: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val t = Plume.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(if (selected) t.color.accentMuted else t.color.background)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .height(52.dp),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (selected) t.color.accent else t.color.background))
        Column(Modifier.weight(1f).padding(start = t.space.md - 3.dp, end = t.space.sm)) {
            val (first, second) = when (item) {
                is CatalogItem.Command -> item.command.command to item.command.name
                is CatalogItem.Snippet -> item.snippet.title to "snippet · ${item.snippet.category.lowercase()}"
            }
            Text(first, style = t.type.mono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(second, style = t.type.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (favourite) Text("★", style = t.type.body.copy(color = t.color.accentText))
        val glyph = when (item) {
            is CatalogItem.Command -> item.command.preview
            is CatalogItem.Snippet -> "{ }"
        }
        Text(
            glyph,
            style = t.type.body.copy(color = if (item is CatalogItem.Snippet) t.color.faint else t.color.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 56.dp, max = 120.dp).padding(horizontal = t.space.md),
        )
    }
}

@Composable
private fun DetailPane(state: PlumeState, ui: UiFocus, modifier: Modifier) {
    val t = Plume.tokens
    Column(
        modifier.background(t.color.background).verticalScroll(rememberScrollState()).padding(t.space.lg),
        verticalArrangement = Arrangement.spacedBy(t.space.md),
    ) {
        when (val item = state.selected) {
            null -> Text("Select a command to see its source and preview.", style = t.type.body.copy(color = t.color.faint))
            is CatalogItem.Command -> CommandDetail(item, state, ui)
            is CatalogItem.Snippet -> SnippetDetail(item, state, ui)
        }
    }
}

@Composable
private fun ColumnScope.CommandDetail(item: CatalogItem.Command, state: PlumeState, ui: UiFocus) {
    val t = Plume.tokens
    val cmd = item.command
    Text(listOfNotNull(cmd.category.lowercase(), cmd.pkg).joinToString(" · "), style = t.type.caption)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(t.space.lg)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(128.dp).clip(RoundedCornerShape(t.radius.md))
                .background(t.color.surface)
                .border(1.dp, t.color.border, RoundedCornerShape(t.radius.md)),
        ) {
            Text(cmd.preview, style = t.type.glyph.copy(fontSize = glyphSize(cmd.preview)), maxLines = 1)
        }
        Column(verticalArrangement = Arrangement.spacedBy(t.space.xs)) {
            Text(cmd.name, style = t.type.title)
            Text("preview is approximate; nothing is compiled", style = t.type.caption)
        }
    }
    FieldLabel("source")
    CodeBlock(cmd.command)
    Row(horizontalArrangement = Arrangement.spacedBy(t.space.sm)) {
        PillButton("copy  ⏎", hint = "Copy the command source", primary = true) { state.copySelected(); ui.focusSearch() }
        val favourite = state.isFavourite(item)
        PillButton(
            if (favourite) "★ favourite" else "☆ favourite",
            hint = "Toggle favourite (${primaryLabel}D)",
            selected = favourite,
        ) { state.toggleFavourite(item); ui.focusSearch() }
    }
    cmd.usage?.let { usage ->
        FieldLabel("usage")
        CodeBlock(usage)
        PillButton("copy usage  ⇧⏎", hint = "Copy the usage example") { state.copySelectedUsage(); ui.focusSearch() }
    }
    cmd.pkg?.let { pkg ->
        FieldLabel("package")
        CodeBlock("\\usepackage{$pkg}")
        PillButton("copy \\usepackage", hint = "Copy the package line for the preamble") {
            state.copyPackageLine(item); ui.focusSearch()
        }
    }
}

@Composable
private fun ColumnScope.SnippetDetail(item: CatalogItem.Snippet, state: PlumeState, ui: UiFocus) {
    val t = Plume.tokens
    Text("snippet · ${item.snippet.category.lowercase()}", style = t.type.caption)
    Text(item.snippet.title, style = t.type.title)
    PillButton("copy snippet  ⏎", hint = "Copy the complete snippet", primary = true) {
        state.copySelected(); ui.focusSearch()
    }
    CodeBlock(item.snippet.code)
}

private fun glyphSize(preview: String): TextUnit = when {
    preview.length <= 2 -> 56.sp
    preview.length <= 5 -> 32.sp
    else -> 18.sp
}

@Composable
private fun FieldLabel(text: String) {
    val t = Plume.tokens
    Text(text, style = t.type.caption)
}

@Composable
private fun CodeBlock(text: String) {
    val t = Plume.tokens
    val shape = RoundedCornerShape(t.radius.sm)
    SelectionContainer {
        Box(
            Modifier.fillMaxWidth().clip(shape)
                .background(t.color.surface)
                .border(1.dp, t.color.border, shape)
                .horizontalScroll(rememberScrollState())
                .padding(t.space.md),
        ) {
            Text(text, style = t.type.monoCallout, softWrap = false)
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    hint: String,
    selected: Boolean = false,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val t = Plume.tokens
    val shape = RoundedCornerShape(t.radius.sm)
    val background = when {
        primary -> t.color.accent
        selected -> t.color.accentMuted
        else -> t.color.surface
    }
    val foreground = when {
        primary -> Color.White
        selected -> t.color.accentText
        else -> t.color.text
    }
    Box(
        Modifier.clip(shape)
            .background(background)
            .border(1.dp, if (primary || selected) t.color.accent else t.color.border, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label. $hint" }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, style = t.type.mono.copy(color = foreground))
    }
}

@Composable
private fun StatusBar(state: PlumeState) {
    val t = Plume.tokens
    var showMessage by remember { mutableStateOf(true) }
    LaunchedEffect(state.statusSerial) {
        showMessage = true
        if (!state.statusIsError) {
            delay(2500)
            showMessage = false
        }
    }
    val message = state.status?.takeIf { showMessage }
    val hints = "↑↓ select · ⏎ copy · ⇧⏎ usage · ${primaryLabel}D favourite · " +
        "${altLabel}↑↓ section · ${primaryLabel}T on top · Esc clear"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(t.color.surface)
            .padding(horizontal = t.space.lg, vertical = 6.dp),
    ) {
        Text(
            message ?: hints,
            style = t.type.caption.copy(
                color = when {
                    message == null -> t.color.faint
                    state.statusIsError -> t.color.error
                    else -> t.color.accentText
                },
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Hairline() {
    val t = Plume.tokens
    Box(Modifier.fillMaxWidth().height(1.dp).background(t.color.border))
}

@Composable
private fun VerticalHairline() {
    val t = Plume.tokens
    Box(Modifier.width(1.dp).fillMaxHeight().background(t.color.border))
}
