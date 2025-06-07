package app.gemicom

import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories

val TMP_APP_DIR: Path = Path.of(
    AppDirsFactory.getInstance().getUserCacheDir("Gemicom", null, null)
)
val TESTS_APP_DIR: Path = TMP_APP_DIR.resolve("Tests")
val TESTS_MEDIA_DIR: Path = TESTS_APP_DIR.resolve("Media").also {
    it.createDirectories()
}
