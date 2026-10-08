package com.nexauren.imagetools.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexauren.imagetools.auth.AuthRepository
import com.nexauren.imagetools.data.HistoryEntry
import com.nexauren.imagetools.data.HistoryStore
import com.nexauren.imagetools.data.Recipe
import com.nexauren.imagetools.data.RecipeStore
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreenV5(strings: UiText) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entries by remember { mutableStateOf(HistoryStore.list(context)) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(strings.get("history"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(strings.get("history.empty"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                HistoryStore.clear(context)
                entries = emptyList()
            }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
            }
        }
        if (entries.isEmpty()) {
            Text(
                strings.get("history.empty"),
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    HistoryCardV5(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryCardV5(entry: HistoryEntry) {
    Card(shape = RoundedCornerShape(20.dp)) {
        ListItem(
            leadingContent = {
                Icon(Icons.Default.History, contentDescription = null)
            },
            headlineContent = {
                Text(entry.label, fontWeight = FontWeight.Bold)
            },
            supportingContent = {
                Text(
                    entry.inputCount.toString() + " input(s) • " +
                        DateFormat.getDateTimeInstance().format(Date(entry.timestamp)),
                    fontSize = 10.sp
                )
            },
            trailingContent = {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
            }
        )
    }
}

@Composable
fun RecipesScreenV5(
    auth: AuthRepository,
    premium: Boolean,
    strings: UiText,
    onApplyRecipe: (Recipe) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var recipes by remember { mutableStateOf(RecipeStore.list(context)) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.get("recipes"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        strings.get("recipe.save") + " • " + strings.get("premium.unlock"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (recipes.isEmpty()) {
            item {
                Text(strings.get("recipes.empty"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(recipes) { recipe ->
            Card(shape = RoundedCornerShape(20.dp)) {
                ListItem(
                    headlineContent = { Text(recipe.name, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(
                            recipe.toolId + " • " + recipe.config.entries.joinToString(" · ") { it.key + "=" + it.value },
                            fontSize = 10.sp
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (recipe.toolId != "general") {
                                TextButton(
                                    onClick = { onApplyRecipe(recipe) },
                                    enabled = premium
                                ) {
                                    Text(strings.get("recipe.apply"))
                                }
                            }
                            TextButton(
                                onClick = {
                                    RecipeStore.delete(context, recipe.name)
                                    recipes = RecipeStore.list(context)
                                }
                            ) {
                                Text(strings.get("recipe.delete"))
                            }
                        }
                    }
                )
            }
        }
    }
}
