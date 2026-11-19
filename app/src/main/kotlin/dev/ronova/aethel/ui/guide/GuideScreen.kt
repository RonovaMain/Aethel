package dev.ronova.aethel.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ronova.aethel.data.DATA_DATE
import dev.ronova.aethel.data.GAME_VERSION
import dev.ronova.aethel.data.GameId
import dev.ronova.aethel.data.GenshinGuide
import dev.ronova.aethel.data.GuideBlock
import dev.ronova.aethel.data.GuideCard
import dev.ronova.aethel.data.GuideSection
import dev.ronova.aethel.ui.common.EmptyHint
import dev.ronova.aethel.ui.common.HorizontalChips
import dev.ronova.aethel.ui.common.gameBrush

@Composable
fun GuideScreen(padding: PaddingValues) {
    var sectionIdx by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    val sections = GuideSection.entries
    val section = sections[sectionIdx.coerceIn(0, sections.size - 1)]
    val q = query.trim()
    val searching = q.isNotEmpty()
    val cards = remember(section, q) {
        if (searching) GenshinGuide.search(q) else GenshinGuide.cards(section)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(gameBrush(GameId.GENSHIN))
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Руководство Genshin Impact",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Версия $GAME_VERSION · данные на $DATA_DATE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                    Text(
                        text = "Реакции, Лунные и Звёздные механики, ресурсы и советы. Работает офлайн.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск по руководству") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Очистить")
                        }
                    }
                },
            )
        }

        if (!searching) {
            item {
                HorizontalChips {
                    sections.forEachIndexed { index, s ->
                        FilterChip(
                            selected = index == sectionIdx,
                            onClick = { sectionIdx = index },
                            label = { Text(s.title, maxLines = 1) },
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Найдено карточек: ${cards.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (cards.isEmpty()) {
            item { EmptyHint("Ничего не найдено") }
        }

        itemsIndexed(cards) { _, card ->
            GuideCardView(card)
        }
    }
}

@Composable
private fun GuideCardView(card: GuideCard) {
    var expanded by rememberSaveable(card.title) { mutableStateOf(card.expandedByDefault) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (card.tag.isNotEmpty()) {
                            Box(
                                Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = card.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (card.subtitle.isNotEmpty()) {
                        Text(
                            text = card.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    card.blocks.forEach { BlockView(it) }
                }
            }
        }
    }
}

@Composable
private fun BlockView(block: GuideBlock) {
    when (block) {
        is GuideBlock.Para -> Text(block.text, style = MaterialTheme.typography.bodyMedium)

        is GuideBlock.Sub -> Text(
            text = block.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        is GuideBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }

        is GuideBlock.Note -> {
            val bg = if (block.warning) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
            val fg = if (block.warning) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(bg)
                    .padding(12.dp),
            ) {
                Text(
                    text = (if (block.warning) "Важно: " else "") + block.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg,
                )
            }
        }

        is GuideBlock.Table -> TableView(block)
    }
}

@Composable
private fun TableView(table: GuideBlock.Table) {
    val cols = table.header.size
    val weights = if (table.weights.size == cols) table.weights else List(cols) { 1f }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TableRow(
            cells = table.header,
            weights = weights,
            background = MaterialTheme.colorScheme.surfaceContainerHighest,
            header = true,
        )
        table.rows.forEachIndexed { i, r ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            TableRow(
                cells = r,
                weights = weights,
                background = Color.Transparent,
                header = false,
            )
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    weights: List<Float>,
    background: Color,
    header: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cells.forEachIndexed { index, text ->
            val w = weights.getOrElse(index) { 1f }
            Text(
                text = text,
                modifier = Modifier.weight(w),
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (header || index == 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
