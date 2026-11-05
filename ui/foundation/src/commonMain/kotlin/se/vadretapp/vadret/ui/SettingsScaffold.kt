package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        VadretTopAppBar(
            title = title,
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}