package com.github.helltar.anpaside.core.project

import com.github.helltar.anpaside.core.Paths
import com.github.helltar.anpaside.core.Paths.EXT_PROJ
import java.io.File

// the projects directory on disk: a project is a subdirectory that holds a matching .aproj
object Projects {

    fun names(): List<String> =
        Paths.projectsDir.listFiles().orEmpty()
            .filter { it.resolve(it.name + EXT_PROJ).exists() }
            .map { it.name }
            .sorted()

    fun dir(name: String): File = Paths.projectsDir.resolve(name)

    fun configFile(name: String): File = dir(name).resolve(name + EXT_PROJ)
}
