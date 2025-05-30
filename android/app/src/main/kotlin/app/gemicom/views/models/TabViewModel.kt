package app.gemicom.views.models

import app.gemicom.CachableGeminiClient
import app.gemicom.GeminiClient
import app.gemicom.IDb
import app.gemicom.models.*
import kotlinx.coroutines.withContext
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance
import java.nio.file.Path
import kotlin.coroutines.coroutineContext

class ScopedTab(var tab: ITab) : AutoCloseable, DIGlobalAware {
    private val Db: IDb by instance()
    private val CacheDir: Path by instance(tag = "CACHE_DIR")
    private val Documents: IDocuments by instance()
    private val Certificates: ICertificates by instance()

    val cache: SqliteCache by lazy { SqliteCache(tab.id, CacheDir, Db) }
    val client: CachableGeminiClient by lazy {
        CachableGeminiClient(cache, Documents, GeminiClient(Certificates))
    }

    val id = tab.id

    val currentLocation: String
        get() = tab.currentLocation

    override fun close() {
        client.close()
    }

    fun resolve(address: String): String {
        return when (val t = tab) {
            is SqlTab -> t.resolve(address)
            is UninitializedTab -> ""
        }
    }

    suspend fun navigate(
        address: String, pushToHistory: Boolean = true, isCheckCache: Boolean = true
    ): IGeminiDocument {
        val uri = when (val t = tab) {
            is SqlTab -> t.navigate(address, pushToHistory)
            is UninitializedTab -> {
                t.start(address).also { tab = it }.currentLocation
            }
        }

        return load(uri, isCheckCache)
    }

    suspend fun load(uri: String, isCheckCache: Boolean = true) = withContext(coroutineContext) {
        try {
            val content = client.get(uri, isCheckCache)
            tab.status = TabStatus.VALID
            ChunkedGeminiDocument.fromText(currentLocation, content)
        } catch (e: Exception) {
            tab.status = TabStatus.INVALID
            throw (e)
        }
    }

    suspend fun back(): IGeminiDocument {
        return when (val tab = tab) {
            is SqlTab -> {
                val previous = tab.peekPrevious()
                tab.back()
                val document = load(previous, true)
                document
            }

            else -> throw NoMoreHistory
        }
    }

    suspend fun forward(): IGeminiDocument {
        return when (val tab = tab) {
            is SqlTab -> {
                val next = tab.peekNext()
                tab.forward()
                val document = load(next, true)
                document
            }

            else -> throw NoNextEntry
        }
    }

    fun canGoBack(): Boolean = when (val tab = tab) {
        is SqlTab -> tab.canGoBack()
        is UninitializedTab -> false
    }

    fun canGoForward(): Boolean = when (val tab = tab) {
        is SqlTab -> tab.canGoForward()
        is UninitializedTab -> false
    }
}
