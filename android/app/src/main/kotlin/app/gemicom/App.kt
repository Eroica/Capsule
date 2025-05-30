package app.gemicom

import android.app.Application
import app.gemicom.models.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.*
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.conf.global
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.createDirectories

fun appModule(appDir: Path, mediaRoot: Path, cacheDir: Path) = DI.Module(name = "App") {
    val mediaDir = mediaRoot.resolve(MEDIA_NAME)

    bindSingleton(tag = "APP_DIR") { appDir }
    bindSingleton(tag = "MEDIA_DIR") { mediaDir }
    bindSingleton(tag = "CACHE_DIR") { cacheDir }
    bind<CoroutineDispatcher>() with singleton { Dispatchers.IO }
    bind<CoroutineDispatcher>(tag = "WRITER") with singleton {
        Dispatchers.IO.limitedParallelism(1)
    }
    bindSingleton { DefaultContext() }

    bindSingleton { Db.at(appDir) }
    bindSingleton { SqlDocuments(instance()) }
    bindSingleton { SqlTabs(instance()) }
    bindSingleton { SqlCertificates(instance()) }

    bindSingleton { GeminiClient(instance()) }
    bindSingleton(tag = "DEFAULT_CACHE") {
        SqliteCache(SqliteCache.DEFAULT_CACHE_ID, cacheDir, instance())
    }

    bindSingleton(tag = "AppSettings") { SqlPreferences("AppSettings", instance()) }
}

class App : Application(), DIGlobalAware {
    private val Documents: IDocuments by instance()
    private val DefaultContext: IContext by instance()
    private val Writer: CoroutineDispatcher by instance(tag = "WRITER")

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("gemicom")
        val appDir = applicationContext.getDatabasePath(DB_NAME).toPath().parent
        val cacheDir = applicationContext.cacheDir.toPath()
        val mediaRoot = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
        mediaRoot.resolve(MEDIA_NAME).toPath().createDirectories()
        DI.global.addImport(appModule(appDir, mediaRoot.toPath(), cacheDir))

        /* Clean up old document cache */
        DefaultContext.co.launch(Writer) {
            Documents.clear(LocalDateTime.now().minusMonths(1))
        }
    }
}
