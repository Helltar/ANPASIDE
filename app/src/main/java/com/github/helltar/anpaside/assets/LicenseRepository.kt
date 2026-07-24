package com.github.helltar.anpaside.assets

import android.content.res.AssetManager

data class LicenseDocuments(
    val mit: String,
    val apache: String
)

class LicenseRepository(
    private val assets: AssetManager
) {
    fun load(): LicenseDocuments =
        LicenseDocuments(
            mit = read("licenses/mit.txt"),
            apache = read("licenses/apache-2.0.txt")
        )

    private fun read(path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }
}

