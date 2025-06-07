package app.gemicom.models

import app.gemicom.IDb
import app.gemicom.Sql
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile

class SqliteCache(
    private val cacheId: Long,
    val cacheDir: Path,
    private val db: IDb
) : AutoCloseable {
    companion object {
        const val DEFAULT_CACHE_ID = 1000L
    }

    private val logger = KotlinLogging.logger {}

    override fun close() {
        logger.info { "Cache $cacheId: Closing" }
        db.transaction {
            val managedFiles = db.query(Sql.Cache_GetFilename, {
                it.setLong(1, cacheId)
            }) {
                buildList {
                    while (it.next()) {
                        add(cacheDir.resolve(it.getString(1)))
                    }
                }
            }
            db.update(Sql.Cache_Delete, {
                it.setLong(1, cacheId)
            })

            logger.info { "Cache $cacheId: Found ${managedFiles.size} files:" }
            logger.info { """Cache $cacheId:
${managedFiles.joinToString("\n") { it.toString() }}""" }
            managedFiles.forEach { it.deleteIfExists() }
        }
    }

    fun add(name: String, originalName: String) {
        db.update(Sql.Cache_Create) {
            it.setLong(1, cacheId)
            it.setString(2, name)
            it.setString(3, originalName)
        }
        logger.info { "Cache $cacheId: Added $name ($originalName)" }
    }

    fun purge() {
        logger.info { "Cache $cacheId: Begin cache purge" }
        val knownFiles = db.query(Sql.Cache_All, {}) {
            buildList {
                while (it.next()) {
                    add(cacheDir.resolve(it.getString(1)))
                }
            }
        }

        Files.list(cacheDir).filter { it !in knownFiles && it.isRegularFile() }.forEach {
            it.deleteIfExists()
            logger.info { "Deleted from cache: $it" }
        }
    }
}
