package app.gemicom

import app.gemicom.models.SqlCertificates
import org.junit.AfterClass
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class GeminiClientTest {
    companion object {
        private lateinit var db: IDb
        private lateinit var client: GeminiClient

        @BeforeClass
        @JvmStatic
        fun setUp() {
            db = Db.memory(TESTS_APP_DIR)
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
        val r = client.get("gemini://groundctrl.earth")
        assertNotEquals("", r)
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Get invalid url`() {
        val r = client.get("gemini://             ")
    }

    @Test(expected = CertificateMismatchError::class)
    fun `Certificate mismatch throws`() {
        val r = client.get("gemini://groundctrl.earth")
        db.update("""UPDATE certificate set hash='invalid' WHERE host='groundctrl.earth'""")
        val r2 = client.get("gemini://groundctrl.earth")
    }
}
