package app.gemicom

import app.gemicom.models.SqlCertificates
import org.junit.AfterClass
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

internal class GeminiClientTest {
    companion object {
        private lateinit var db: IDb
        private lateinit var client: GeminiClient

        @BeforeClass
        @JvmStatic
        fun setUp() {
            db = Db.memory()
            client = GeminiClient(SqlCertificates(db))
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            db.close()
        }
    }

    @Before
    fun reset() {
        db.update("""DELETE FROM certificate""")
    }

    @Test
    fun `Get page`() {
        val r = client.get("gemini://gemicom.app")
        assertNotEquals("", r)
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Get invalid url`() {
        val r = client.get("gemini://             ")
    }

    @Test(expected = CertificateMismatchError::class)
    fun `Certificate mismatch throws`() {
        val r = client.get("gemini://gemicom.app")
        db.update("""UPDATE certificate set hash='invalid' WHERE host='gemicom.app'""")
        val r2 = client.get("gemini://gemicom.app")
    }
}
