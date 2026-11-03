package io.github.mallma.removereels.detection

/**
 * The rules that ship with the app. Each file is tested against screens recorded from the app versions named in the
 * fixture folders.
 */
public object BundledRules {
    // An absolute path, because R8 may move this class to another package in release builds.
    private const val FOLDER = "/io/github/mallma/removereels/detection/rules"
    private val files = listOf("instagram.json", "youtube.json")

    public fun load(): List<AppRules> =
        files.map { file ->
            val resource = checkNotNull(javaClass.getResource("$FOLDER/$file")) { "Missing rules file $file" }
            AppRules.parse(resource.readText())
        }
}
