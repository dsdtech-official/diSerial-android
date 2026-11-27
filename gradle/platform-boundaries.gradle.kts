// Compile-time source sets are the primary boundary. This task gives an earlier,
// host-independent error when a platform API leaks into another source set.
val verifyPlatformBoundaries by tasks.registering {
    group = "verification"
    description = "Rejects platform-specific imports outside their owning Kotlin source set."

    val sourceRoots = listOf("commonMain", "androidMain")
    val sources = sourceRoots.associateWith { sourceSet ->
        fileTree("shared/src/$sourceSet") { include("**/*.kt") }
    }
    inputs.files(sources.values)
    val sourceRootDir = rootDir

    doLast {
        val androidApis = listOf("android.", "java.", "javax.", "com.example.data.usb.")
        val androidAdapters = Regex("^com\\.example\\.(?:platform\\.Android|data\\.local\\.Android|ui\\.state\\.Android)")
        val importPattern = Regex("^\\s*import\\s+([^\\s;]+)")
        val violations = mutableListOf<String>()

        sources.forEach { (sourceSet, tree) ->
            tree.files.sortedBy { it.path }.forEach { source ->
                source.readLines().forEachIndexed { index, line ->
                    val imported = importPattern.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
                    val forbidden = sourceSet != "androidMain" &&
                        (androidApis.any(imported::startsWith) || androidAdapters.containsMatchIn(imported))
                    if (forbidden) {
                        violations += "${source.relativeTo(sourceRootDir)}:${index + 1}: $imported"
                    }
                }
            }
        }
        check(violations.isEmpty()) {
            "Platform boundary violations:\n${violations.joinToString("\n")}\n" +
                "Keep contracts in commonMain and inject implementations from the owning platform."
        }
    }
}
