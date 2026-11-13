package de.pflegefueranfaenger.app.ui.lookup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.app.LoadedContent
import de.pflegefueranfaenger.app.ui.components.HouseSpecificBadge
import de.pflegefueranfaenger.app.ui.components.UnreviewedBadge
import de.pflegefueranfaenger.app.ui.components.WordPartsBreakdown
import de.pflegefueranfaenger.core.model.Source
import de.pflegefueranfaenger.core.search.SearchHit

/** Nachschlagewerk: Suche über Artikel und Fachbegriffe, ohne Suche nach Kategorien bzw. A–Z. */
@Composable
fun LookupScreen(
    content: LoadedContent,
    onOpenArticle: (String) -> Unit,
    onOpenTerm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val bundle = content.bundle
    val hits = remember(query) { content.search.search(query) }

    Column(modifier.fillMaxSize()) {
        Text("Nachschlagen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("z.B. Kalium, Elektroden, Hypo …") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Suche leeren") }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        if (query.isNotBlank()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (hits.isEmpty()) item {
                    Text("Keine Treffer für „$query“.", modifier = Modifier.padding(20.dp))
                }
                items(hits, key = { (if (it is SearchHit.ArticleHit) "a:" else "t:") + it.title }) { hit ->
                    when (hit) {
                        is SearchHit.ArticleHit -> ListItem(
                            headlineContent = { Text(hit.article.title) },
                            supportingContent = { Text(hit.article.summary, maxLines = 2) },
                            overlineContent = { Text("Artikel · ${hit.article.category}") },
                            modifier = Modifier.clickable { onOpenArticle(hit.article.id) },
                        )
                        is SearchHit.TermHit -> ListItem(
                            headlineContent = { Text(hit.term.term) },
                            supportingContent = { Text(hit.term.parts.joinToString(" | ") { "${it.part} = ${it.meaning}" }, maxLines = 2) },
                            overlineContent = { Text("Fachbegriff") },
                            modifier = Modifier.clickable { onOpenTerm(hit.term.id) },
                        )
                    }
                    HorizontalDivider()
                }
            }
        } else {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Artikel") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Wortbausteine A–Z") })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (tab == 0) {
                    bundle.articles.groupBy { it.category }.forEach { (category, articles) ->
                        item(key = "c:$category") {
                            Text(category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
                        }
                        items(articles, key = { it.id }) { a ->
                            ListItem(
                                headlineContent = { Text(a.title) },
                                supportingContent = { Text(a.summary, maxLines = 2) },
                                modifier = Modifier.clickable { onOpenArticle(a.id) },
                            )
                        }
                    }
                } else {
                    items(bundle.terms, key = { it.id }) { t ->
                        ListItem(
                            headlineContent = { Text(t.parts.joinToString(" | ") { it.part }, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(t.meaning, maxLines = 2) },
                            modifier = Modifier.clickable { onOpenTerm(t.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, body: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { body() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleScreen(content: LoadedContent, articleId: String, onBack: () -> Unit, onOpenTerm: (String) -> Unit) {
    val bundle = content.bundle
    val a = bundle.article(articleId) ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    DetailScaffold(a.category, onBack) {
        Text(a.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UnreviewedBadge()
            if (a.houseSpecific) HouseSpecificBadge()
        }
        Text(a.summary, style = MaterialTheme.typography.titleMedium)
        a.body.forEach { para ->
            if (para.startsWith("- ")) {
                Row(Modifier.padding(start = 8.dp)) {
                    Text("•  ", color = MaterialTheme.colorScheme.primary)
                    Text(para.removePrefix("- "), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Text(para, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (a.houseSpecific) {
            Text(
                "Grenzwerte, Abläufe und Medikamente können sich je nach Klinik unterscheiden. Es gelten die Standards deines Hauses und die ärztliche Anordnung.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        val terms = a.termIds.mapNotNull { bundle.term(it) }
        if (terms.isNotEmpty()) {
            Text("Fachbegriffe", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                terms.forEach { t -> AssistChip(onClick = { onOpenTerm(t.id) }, label = { Text(t.term) }) }
            }
        }
        Sources(a.sourceIds.mapNotNull { bundle.source(it) }, "Quellen · geprüft am ${a.lastChecked}")
    }
}

@Composable
fun TermScreen(content: LoadedContent, termId: String, onBack: () -> Unit) {
    val bundle = content.bundle
    val t = bundle.term(termId) ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    DetailScaffold("Fachbegriff", onBack) {
        Text(t.term, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Wortbausteine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        WordPartsBreakdown(t.parts)
        Text("Bedeutung", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(t.meaning, style = MaterialTheme.typography.bodyLarge)
        t.example?.let {
            Text("Beispiel", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
        UnreviewedBadge()
        Sources(t.sourceIds.mapNotNull { bundle.source(it) }, "Quellen")
    }
}

@Composable
fun Sources(sources: List<Source>, title: String) {
    val uri = LocalUriHandler.current
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
    sources.forEach { s ->
        val url = s.url
        Text(
            s.citation,
            style = MaterialTheme.typography.bodySmall,
            textDecoration = if (url != null) TextDecoration.Underline else null,
            color = if (url != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = if (url != null) Modifier.clickable { runCatching { uri.openUri(url) } } else Modifier,
        )
    }
}
