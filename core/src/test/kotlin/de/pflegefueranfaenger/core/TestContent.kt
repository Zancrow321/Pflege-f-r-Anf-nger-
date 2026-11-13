package de.pflegefueranfaenger.core

import de.pflegefueranfaenger.core.model.ContentBundle
import de.pflegefueranfaenger.core.model.ContentLoader
import java.io.File

object TestContent {
    val dir: File = File(System.getProperty("contentDir") ?: "../content")
    val bundle: ContentBundle by lazy { ContentLoader.load { File(dir, it).readText() } }
}
