package app.gemicom.models

import app.gemicom.IDb
import app.gemicom.Sql
import java.sql.Types

interface IPreferences {
    operator fun get(name: String): String?
    operator fun set(name: String, value: String?)
    operator fun set(name: String, value: Boolean)
}

class SqlPreferences(
    private val prefs: String,
    private val db: IDb
) : IPreferences {
    override fun get(name: String): String? {
        return db.query(Sql.Env_Settings_Get, {
            it.setString(1, name)
            it.setString(2, prefs)
        }) {
            if (it.next()) {
                it.getString(1)
            } else {
                null
            }
        }
    }

    override fun set(name: String, value: String?) {
        db.transaction {
            db.update(Sql.Env_Settings_Set_1) {
                it.setString(1, prefs)
                it.setString(2, name)
                if (value == null) {
                    it.setNull(3, Types.VARCHAR)
                } else {
                    it.setString(3, value)
                }
            }
            db.update(Sql.Env_Settings_Set_2) {
                it.setString(1, name)
                if (value == null) {
                    it.setNull(2, Types.VARCHAR)
                } else {
                    it.setString(2, value)
                }
                it.setString(3, prefs)
            }
        }
    }

    override fun set(name: String, value: Boolean) {
        db.transaction {
            db.update(Sql.Env_Settings_Set_1) {
                it.setString(1, prefs)
                it.setString(2, name)
                it.setBoolean(3, value)
            }
            db.update(Sql.Env_Settings_Set_2) {
                it.setString(1, name)
                it.setBoolean(2, value)
                it.setString(3, prefs)
            }
        }
    }
}
