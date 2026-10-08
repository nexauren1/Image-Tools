package com.nexauren.imagetools.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.nexauren.imagetools.data.FavoritesStore

@Composable
fun FavoritesScreen(strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoritesStore.list(context)) }
    val tools = TOOL_CATALOG.filter { it.id in favorites }
    val language = AppLanguageStore.get(context)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (language) {
                                AppLanguage.PT -> "Favoritos"
                                AppLanguage.ES -> "Favoritos"
                                AppLanguage.FR -> "Favoris"
                                else -> "Favorites"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            tools.size.toString() + " " + strings.get("tools"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (tools.isEmpty()) {
            item {
                Card(shape = MaterialTheme.shapes.medium) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.StarBorder,
                            null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            when (language) {
                                AppLanguage.PT -> "Ainda está vazio"
                                AppLanguage.ES -> "Todavía está vacío"
                                AppLanguage.FR -> "C’est encore vide"
                                else -> "Nothing here yet"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (language) {
                                AppLanguage.PT -> "Toque na estrela de uma ferramenta para a guardar nesta página."
                                AppLanguage.ES -> "Toca la estrella de una herramienta para guardarla aquí."
                                AppLanguage.FR -> "Touchez l’étoile d’un outil pour l’enregistrer ici."
                                else -> "Tap the star on any tool to save it here."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(((tools.size + 1) / 2 * 154).dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(tools) { tool ->
                        ToolGridCardV5(tool, strings, premium, openTool)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        tools.forEach { FavoritesStore.toggle(context, it.id) }
                        favorites = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (language) {
                            AppLanguage.PT -> "Limpar favoritos"
                            AppLanguage.ES -> "Limpiar favoritos"
                            AppLanguage.FR -> "Vider les favoris"
                            else -> "Clear favorites"
                        }
                    )
                }
            }
        }
    }
}
