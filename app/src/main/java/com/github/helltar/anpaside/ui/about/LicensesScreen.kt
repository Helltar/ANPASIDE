package com.github.helltar.anpaside.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.ui.components.BackButton

// attribution required by the components' licenses; the full texts are read from assets
private data class OpenSourceComponent(
    val name: String,
    val copyright: String,
    val license: String,
    val url: String? = null
)

private val openSourceComponents = listOf(
    OpenSourceComponent(
        name = "J2ME Loader",
        copyright = "Copyright 2017-2024 Nikita Shakarun",
        license = "Apache License 2.0",
        url = "https://github.com/nikita36078/J2ME-Loader"
    ),
    OpenSourceComponent(
        name = "MicroEmulator",
        copyright = "Copyright 2001-2007 Bartek Teodorczyk, Vlad Skarzhevskyy, Markus Heberling and others",
        license = "Apache License 2.0 (also available under LGPL 2.1)"
    ),
    OpenSourceComponent(
        name = "Android dx",
        copyright = "Copyright The Android Open Source Project",
        license = "Apache License 2.0"
    ),
    OpenSourceComponent(
        name = "MidiDriver 1.29",
        copyright = "Copyright 2013 Bill Farmer",
        license = "Apache License 2.0",
        url = "https://github.com/billthefarmer/mididriver"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    viewModel: LicensesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val state = viewModel.state

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_licenses)) },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { innerPadding ->
        val documents = state.documents

        if (state.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                CircularProgressIndicator()
            }
        } else if (documents != null) {
            Column(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Section("ANPASIDE") {
                    Line("MIT License")
                }

                for (component in openSourceComponents) {
                    Section(component.name) {
                        Line(component.copyright)
                        Line(component.license)

                        if (component.url != null) {
                            Text(
                                text = component.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { uriHandler.openUri(component.url) }
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }

                LicenseText("MIT License", documents.mit)
                LicenseText("Apache License 2.0", documents.apache)
            }
        } else if (state.failed) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.err_failed_load_licenses),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    HorizontalDivider(Modifier.padding(top = 16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )

    content()
}

@Composable
private fun Line(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun LicenseText(title: String, body: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )

    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}
