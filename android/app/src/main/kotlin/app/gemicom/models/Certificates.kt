package app.gemicom.models

import app.gemicom.DATE_FORMAT
import app.gemicom.IDb
import app.gemicom.Sql
import java.time.LocalDateTime

class NoCertificateError : Exception()

interface ICertificates {
    operator fun get(host: String): Pair<String, LocalDateTime>

    fun add(host: String, hash: String)
    fun replace(host: String, hash: String)
    fun clear()
}

class SqlCertificates(private val db: IDb) : ICertificates {
    override fun get(host: String): Pair<String, LocalDateTime> {
        return db.query(Sql.Certificate_Get, { it.setString(1, host) }) {
            if (it.next()) {
                it.getString(1) to LocalDateTime.parse(it.getString(2), DATE_FORMAT)
            } else {
                throw NoCertificateError()
            }
        }
    }

    override fun add(host: String, hash: String) {
        db.update(Sql.Certificate_Create) {
            it.setString(1, host)
            it.setString(2, hash)
        }
    }

    override fun replace(host: String, hash: String) {
        db.update(Sql.Certificate_Replace) {
            it.setString(1, hash)
            it.setString(2, host)
        }
    }

    override fun clear() {
        db.update(Sql.Certificate_DeleteAll)
    }
}
