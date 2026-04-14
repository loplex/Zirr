package cz.lopin.zirr

import com.squareup.moshi.Moshi
import cz.lopin.zirr.data.model.BrandRemotesData
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class ParseTest {
    @Test
    fun testParse() {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(BrandRemotesData::class.java)
        val json = File("build/generated/assets/tv_brands_remotes/1506_Thomson.json").readText()
        val data = adapter.fromJson(json)
        assertNotNull(data)
        assertTrue(data!!.remotes.isNotEmpty())
        assertTrue(data.remotes[0].keys!!.isNotEmpty())
    }
}
