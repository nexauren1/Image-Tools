package com.nexauren.imagetools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.nexauren.imagetools.data.AppCenterStore
import com.nexauren.imagetools.data.FavoritesStore

private data class CenterCard(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val text: String,
    val accent: Color
)

private fun centerCards(language: AppLanguage): List<CenterCard> {
    val pt = language == AppLanguage.PT
    val es = language == AppLanguage.ES
    val fr = language == AppLanguage.FR

    return if (pt) {
        listOf(
            CenterCard(Icons.Default.NewReleases, "Central de novidades", "O Image Tools ganhou novas páginas, favoritos e uma biblioteca visual maior.", Color(0xFF7C3AED)),
            CenterCard(Icons.Default.AutoAwesome, "Novo laboratório visual", "Experimente Grão de filme, Color Pop, Contorno, Glitch e Scanner documental.", Color(0xFF0891B2)),
            CenterCard(Icons.Default.Favorite, "Favoritos e categorias", "Guarde as ferramentas que mais usa e filtre a biblioteca por tipo.", Color(0xFFDB2777))
        )
    } else if (es) {
        listOf(
            CenterCard(Icons.Default.NewReleases, "Centro de novedades", "Image Tools gana nuevas páginas, favoritos y una biblioteca visual mayor.", Color(0xFF7C3AED)),
            CenterCard(Icons.Default.AutoAwesome, "Nuevo laboratorio visual", "Prueba Grano de película, Color Pop, Contorno, Glitch y Escáner documental.", Color(0xFF0891B2)),
            CenterCard(Icons.Default.Favorite, "Favoritos y categorías", "Guarda tus herramientas y filtra la biblioteca por tipo.", Color(0xFFDB2777))
        )
    } else if (fr) {
        listOf(
            CenterCard(Icons.Default.NewReleases, "Centre des nouveautés", "Image Tools gagne de nouvelles pages, des favoris et une bibliothèque visuelle plus riche.", Color(0xFF7C3AED)),
            CenterCard(Icons.Default.AutoAwesome, "Nouveau laboratoire visuel", "Testez Grain de film, Color Pop, Contour, Glitch et Scanner de documents.", Color(0xFF0891B2)),
            CenterCard(Icons.Default.Favorite, "Favoris et catégories", "Enregistrez vos outils préférés et filtrez la bibliothèque par type.", Color(0xFFDB2777))
        )
    } else {
        listOf(
            CenterCard(Icons.Default.NewReleases, "What's new", "Image Tools now has new pages, favorites and a larger visual library.", Color(0xFF7C3AED)),
            CenterCard(Icons.Default.AutoAwesome, "New visual lab", "Try Film Grain, Color Pop, Edge Sketch, Glitch and Document Scanner.", Color(0xFF0891B2)),
            CenterCard(Icons.Default.Favorite, "Favorites and categories", "Save the tools you use most and filter the library by type.", Color(0xFFDB2777))
        )
    }
}

@Composable
fun NotificationsCenterScreen(strings: UiText) {
    val context = LocalContext.current
    val language = AppLanguageStore.get(context)
    LaunchedEffect(Unit) {
        AppCenterStore.markAllSeen(context)
    }

    val isPt = language == AppLanguage.PT
    val isFr = language == AppLanguage.FR
    val isEs = language == AppLanguage.ES

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(30.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0B1022), Color(0xFF6D28D9), Color(0xFF0891B2))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            when {
                                isPt -> "CENTRAL DE NOVIDADES"
                                isEs -> "CENTRO DE NOVEDADES"
                                isFr -> "CENTRE DES NOUVEAUTÉS"
                                else -> "WHAT'S NEW"
                            },
                            color = Color.White.copy(alpha = .72f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            when {
                                isPt -> "O que há de novo"
                                isEs -> "Lo nuevo"
                                isFr -> "Les nouveautés"
                                else -> "What's new"
                            },
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            when {
                                isPt -> "Atualizações da aplicação, novas ferramentas e dicas num só lugar."
                                isEs -> "Actualizaciones, nuevas herramientas y consejos en un solo lugar."
                                isFr -> "Mises à jour, nouveaux outils et conseils au même endroit."
                                else -> "App updates, new tools and useful tips in one place."
                            },
                            color = Color.White.copy(alpha = .82f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        items(centerCards(language)) { card ->
            Card(shape = RoundedCornerShape(24.dp)) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier.size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(card.accent.copy(alpha = .15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(card.icon, null, tint = card.accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(card.title, fontWeight = FontWeight.ExtraBold)
                        Text(
                            card.text,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            FeatureCardV5(
                Icons.Default.TipsAndUpdates,
                when {
                    isPt -> "Dica rápida"
                    isEs -> "Consejo rápido"
                    isFr -> "Astuce rapide"
                    else -> "Quick tip"
                },
                when {
                    isPt -> "Use Favoritos para criar o seu painel pessoal e Categorias para encontrar mais depressa."
                    isEs -> "Usa Favoritos para crear tu panel personal y las categorías para encontrar herramientas más rápido."
                    isFr -> "Utilisez les favoris pour créer votre espace personnel et les catégories pour aller plus vite."
                    else -> "Use Favorites as your personal workspace and Categories to find tools faster."
                }
            )
        }
    }
}

@Composable
fun FavoritesScreen(strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoritesStore.list(context)) }
    val tools = TOOL_CATALOG.filter { it.id in favorites }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
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
                            when (AppLanguageStore.get(context)) {
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
                FeatureCardV5(
                    Icons.Default.StarBorder,
                    when (AppLanguageStore.get(context)) {
                        AppLanguage.PT -> "Ainda está vazio"
                        AppLanguage.ES -> "Todavía está vacío"
                        AppLanguage.FR -> "C’est encore vide"
                        else -> "Nothing here yet"
                    },
                    when (AppLanguageStore.get(context)) {
                        AppLanguage.PT -> "Toque na estrela de uma ferramenta para a guardar nesta página."
                        AppLanguage.ES -> "Toca la estrella de una herramienta para guardarla aquí."
                        AppLanguage.FR -> "Touchez l’étoile d’un outil pour l’enregistrer ici."
                        else -> "Tap the star on any tool to save it here."
                    }
                )
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
        }

        if (tools.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = {
                        tools.forEach { FavoritesStore.toggle(context, it.id) }
                        favorites = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (AppLanguageStore.get(context)) {
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
