package app.gemicom.models

import app.gemicom.DATE_FORMAT
import app.gemicom.IDb
import app.gemicom.Sql
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

data object NoMoreHistory : Exception() {
    private fun readResolve(): Any = NoMoreHistory
}

data object NoNextEntry : Exception() {
    private fun readResolve(): Any = NoNextEntry
}

enum class TabStatus(val code: Int) {
    BLANK(0), VALID(1), INVALID(2);

    companion object {
        private val map = entries.associateBy(TabStatus::code)

        fun fromInt(id: Int) = map[id]
    }
}

sealed interface ITab {
    val id: Long
    val currentLocation: String
    val history: List<String>
    val createdAt: LocalDateTime
    var status: TabStatus
}

interface ITabs {
    fun all(): List<ITab>
    fun new(address: String? = null): ITab
    fun delete(tabId: Long)
    fun clear()
}

class UninitializedTab(
    override val id: Long,
    override val createdAt: LocalDateTime,
    private val db: IDb
) : ITab {
    override val currentLocation = ""
    override val history = listOf<String>()
    override var status = TabStatus.BLANK

    fun start(address: String): SqlTab {
        val host = GeminiHost.fromAddress(address)
        val startHistory = Json.encodeToString(listOf(host.location))
        db.update(Sql.Tab_SetHistory) {
            it.setString(1, startHistory)
            it.setLong(2, id)
        }

        return SqlTab(id, createdAt, db, host)
    }
}

class SqlTab(
    override val id: Long,
    override val createdAt: LocalDateTime,
    private val db: IDb,
    private var geminiHost: GeminiHost
) : ITab {
    private var currentIndex = 0

    override val currentLocation: String
        get() = geminiHost.location

    override val history: List<String>
        get() = db.query(Sql.Tab_GetHistory, { it.setLong(1, id) }) {
            buildList {
                while (it.next()) {
                    add(it.getString(1))
                }
            }
        }

    override var status = TabStatus.VALID
        set(value) {
            db.update(Sql.Tab_SetStatus) {
                it.setInt(1, value.code)
                it.setLong(2, id)
            }
            field = value
        }

    init {
        /* Tab always starts with index on latest entry. This means that recreating a tab (from DB)
           actually "forwards" tabs to their latest entry. */
        currentIndex = history.size - 1
    }

    fun peekPrevious(): String {
        try {
            return history[currentIndex - 1]
        } catch (_: IndexOutOfBoundsException) {
            throw NoMoreHistory
        }
    }

    fun peekNext(): String {
        try {
            return history[currentIndex + 1]
        } catch (_: IndexOutOfBoundsException) {
            throw NoNextEntry
        }
    }

    fun back() {
        if (canGoBack()) {
            navigate(history[--currentIndex], false)
        } else {
            throw NoMoreHistory
        }
    }

    fun forward() {
        if (canGoForward()) {
            navigate(history[++currentIndex], false)
        } else {
            throw NoNextEntry
        }
    }

    fun navigate(reference: String, pushToHistory: Boolean = true): String {
        val locationBeforeNavigate = currentLocation
        geminiHost.navigate(reference)

        if (pushToHistory && locationBeforeNavigate != currentLocation) {
            /* If history is not at last location, drop everything behind it */
            var updatedHistory = history.toMutableList()
            if (currentIndex != updatedHistory.size - 1) {
                updatedHistory = updatedHistory.dropLast(updatedHistory.size - currentIndex - 1)
                    .toMutableList()
            }

            updatedHistory.add(currentLocation)
            db.update(Sql.Tab_SetHistory) {
                val entries = Json.encodeToString(updatedHistory)
                it.setString(1, entries)
                it.setLong(2, id)
            }
            currentIndex++
        }

        return geminiHost.location
    }

    fun resolve(reference: String): String {
        return geminiHost.resolve(reference)
    }

    fun canGoBack() = currentIndex > 0

    fun canGoForward() = currentIndex < history.size - 1
}

class SqlTabs(private val db: IDb) : ITabs {
    override fun all(): List<ITab> {
        return db.query(Sql.Tab_All, {}) {
            buildList {
                while (it.next()) {
                    val id = it.getLong(1)
                    val status = TabStatus.fromInt(it.getInt(2))
                    val location = it.getString(3) ?: ""
                    val createdAt = LocalDateTime.parse(it.getString(4), DATE_FORMAT)

                    val tab = when (status) {
                        TabStatus.VALID -> {
                            val geminiHost = GeminiHost.fromAddress(location)
                            SqlTab(id, createdAt, db, geminiHost)
                        }

                        TabStatus.INVALID -> {
                            val geminiHost = GeminiHost.fromAddress(location)
                            SqlTab(id, createdAt, db, geminiHost).also { it.status = TabStatus.INVALID }
                        }

                        else -> UninitializedTab(id, createdAt, db)
                    }

                    add(tab)
                }
            }
        }
    }

    override fun new(address: String?): ITab {
        val (tabId, createdAt) = db.update(Sql.Tab_Create, {}) {
            it.getLong(1) to LocalDateTime.parse(it.getString(2), DATE_FORMAT)
        }

        val tab = UninitializedTab(tabId, createdAt, db)
        if (address != null) {
            return tab.start(address)
        }

        return tab
    }

    override fun delete(tabId: Long) {
        db.update(Sql.Tab_Delete) { it.setLong(1, tabId) }
    }

    override fun clear() {
        db.update(Sql.Tab_Purge)
    }
}
