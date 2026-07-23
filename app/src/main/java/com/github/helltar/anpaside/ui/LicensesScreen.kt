package com.github.helltar.anpaside.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R

// attribution required by the components' licences; the full licence texts are read from assets
private data class OssComponent(
    val name: String,
    val copyright: String,
    val licence: String,
    val url: String? = null
)

private val COMPONENTS = listOf(
    OssComponent(
        name = "J2ME Loader",
        copyright = "Copyright 2017-2024 Nikita Shakarun",
        licence = "Apache License 2.0",
        url = "https://github.com/nikita36078/J2ME-Loader"
    ),
    OssComponent(
        name = "MicroEmulator",
        copyright = "Copyright 2001-2007 Bartek Teodorczyk, Vlad Skarzhevskyy, Markus Heberling and others",
        licence = "Apache License 2.0 (also available under LGPL 2.1)"
    ),
    OssComponent(
        name = "Android dx",
        copyright = "Copyright The Android Open Source Project",
        licence = "Apache License 2.0"
    ),
    OssComponent(
        name = "MidiDriver 1.29",
        copyright = "Copyright 2013 Bill Farmer",
        licence = "Apache License 2.0",
        url = "https://github.com/billthefarmer/mididriver"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val mitText = remember { readAsset(context, "licenses/mit.txt") }
    val apacheText = remember { readAsset(context, "licenses/apache-2.0.txt") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_licenses)) },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { innerPadding ->
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

            for (component in COMPONENTS) {
                Section(component.name) {
                    Line(component.copyright)
                    Line(component.licence)

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

            LicenceText("MIT License", mitText)
            LicenceText("Apache License 2.0", apacheText)
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
private fun LicenceText(title: String, body: String) {
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

private fun readAsset(context: Context, path: String): String =
    context.assets.open(path).bufferedReader().use { it.readText() }
