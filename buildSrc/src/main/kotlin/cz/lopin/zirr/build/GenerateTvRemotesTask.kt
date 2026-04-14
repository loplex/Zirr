package cz.lopin.zirr.build

import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.sql.DriverManager
import java.util.zip.GZIPInputStream
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import java.io.Reader
import java.sql.Connection
import java.sql.Statement
import java.util.Scanner
import kotlin.use

abstract class GenerateTvRemotesTask : DefaultTask() {

    @get:InputFile
    abstract val inputDbDump: RegularFileProperty

    @get:OutputDirectory
    abstract val parentDir: DirectoryProperty

    @get:Input
    abstract val jsonDirName: Property<String>

    @TaskAction
    fun generate() {
        val inputDbDumpFile = inputDbDump.get().asFile

        val brands = mutableMapOf<Int, String>()
        val remotesByBrand = mutableMapOf<Int, MutableList<JsonObject>>()
        val keysByRemote = mutableMapOf<Int, MutableMap<String, MutableList<List<Int>>>>()

        doWithTempSqliteDb { conn ->

            println("Creating temporary SQLite DB from dump...")
            conn.importSqlDump(inputDbDumpFile)

            val categoryId = conn.prepareStatement(
                "SELECT id FROM category WHERE name_en = ?"
            ).use { stmt ->
                stmt.setString(1, "TV")
                val rs = stmt.executeQuery()
                if (!rs.next()) error("Category 'TV' not found!")
                rs.getInt("id")
            }

            conn.prepareStatement(
                "SELECT id, name_en, name FROM brand WHERE category_id = ?"
            ).use { stmt ->
                stmt.setInt(1, categoryId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val id = rs.getInt("id")
                    val nameEn = rs.getString("name_en")
                    val name = rs.getString("name")
                    brands[id] = if (!nameEn.isNullOrEmpty() && nameEn.uppercase() != "NULL") nameEn else name
                }
            }

            conn.prepareStatement(
                "SELECT id, brand_id, remote_number, remote_map, remote, protocol FROM remote_index WHERE category_id = ?"
            ).use { stmt ->
                stmt.setInt(1, categoryId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val rId = rs.getInt("id")
                    val brandId = rs.getInt("brand_id")
                    val remoteObj = mapOf(
                        "id" to rId,
                        "remote_number" to rs.getString("remote_number"),
                        "remote_map" to rs.getString("remote_map"),
                        "remote" to rs.getString("remote"),
                        "protocol" to rs.getString("protocol")
                    ).toJsonObject()
                    remotesByBrand.getOrPut(brandId) { mutableListOf() }.add(remoteObj)
                }
            }

            conn.prepareStatement(
                "SELECT remote_index_id, key_name, key_value FROM decode_remote WHERE category_id = ?"
            ).use { stmt ->
                stmt.setInt(1, categoryId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val rId = rs.getInt("remote_index_id")
                    val kName = rs.getString("key_name")
                    val kVal = rs.getString("key_value")
                    if (!kVal.isNullOrEmpty() && kVal.uppercase() != "NULL") {
                        val timings = kVal.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                        val kNameLower = kName.lowercase().trim()
                        keysByRemote.getOrPut(rId) { mutableMapOf() }
                            .getOrPut(kNameLower) { mutableListOf() }.add(timings)
                    }
                }
            }
        }

        val outputDir = parentDir.get().asFile.resolve(jsonDirName.get()).apply {
            if (exists()) {
                for (f in listFiles { it.extension == "json" }) {
                    f.delete() || throw IllegalStateException("Cannot delete $f")
                }
            } else {
                mkdirs() || throw IllegalStateException("Cannot create output directory")
            }
        }

        println("Generating JSON files in ${outputDir.absolutePath}...")
        val gson = GsonBuilder().setPrettyPrinting().create()

        var generatedFiles = 0
        for ((brandId, brandName) in brands) {
            val brandRemotes = remotesByBrand[brandId] ?: emptyList()

            for (remote in brandRemotes) {
                val rId = remote.get("id").asInt
                val keysMap = keysByRemote[rId]
                if (keysMap != null) {
                    val keysObj = keysMap.toJsonObject { keyTimingsList ->
                        keyTimingsList.toJsonArray { timings -> timings.toJsonArray() }
                    }
                    remote.add("keys", keysObj)
                }
            }

            if (brandRemotes.isNotEmpty()) {
                outputDir.writeTvBrandJson(gson, brandId, brandName, brandRemotes)
                generatedFiles++
            }
        }

        if (generatedFiles == 0) {
            throw GradleException("No TV remotes were generated! Check if the source database contains relevant data.")
        }
        println("Done! Generated $generatedFiles JSON files.")
    }

    fun File.writeTvBrandJson(gson: Gson, brandId: Int, brandName: String, remotes: List<JsonObject>) {
        val safeName = brandName.replace(Regex("[^a-zA-Z0-9 _-]"), "")
        val outputFile = resolve("${brandId}_${safeName}.json")

        val data = mapOf(
            "id" to brandId,
            "name_en" to brandName,
            "remotes" to remotes.toJsonArray()
        ).toJsonObject()

        val dumpStr = gson.toJson(data)
            .replace(Regex("\\[\\s+([\\d\\s,-]+?)\\s+]")) { matchResult ->
                val inner = matchResult.groupValues[1]
                inner.split(",")
                    .joinToString(", ", "[", "]") { it.trim() }
        }

        outputFile.writeText(dumpStr)
    }

    fun <V> Map<String, V>.toJsonObject(mapper: (V)->JsonElement): JsonObject {
        return JsonObject().also { result ->
            forEach { (k, v) -> result.add(k, mapper(v)) }
        }
    }

    fun Map<String, Any>.toJsonObject(): JsonObject {
        return JsonObject().also { result ->
            forEach { (k, v) ->
                when (v) {
                    is String -> result.addProperty(k, v)
                    is Number -> result.addProperty(k, v)
                    is JsonElement -> result.add(k, v)
                    else -> throw IllegalArgumentException("Unsupported value type for key '$k': ${v.javaClass}")
                }
            }
        }
    }

    fun <I> List<I>.toJsonArray(mapper: (I)->JsonElement): JsonArray {
        return JsonArray().also { result ->
            forEach { result.add(mapper(it)) }
        }
    }

    fun List<Any>.toJsonArray(): JsonArray {
        return JsonArray().also { result ->
            forEach { e -> when (e) {
                is String -> result.add(e)
                is Number -> result.add(e)
                is JsonElement -> result.add(e)
                else -> throw IllegalArgumentException("Unsupported value type in array: ${e.javaClass}")
            } }
        }
    }

    fun <T> doWithTempSqliteDb(block: (Connection) -> T): T {
        val tempDb = temporaryDir.resolve("temp_remotes.db")
        if (tempDb.exists() && !tempDb.delete()) {
            throw IllegalStateException("Failed to delete existing temporary database! Try killing Gradle daemon.")
        }

        Class.forName("org.sqlite.JDBC")
        return DriverManager.getConnection("jdbc:sqlite:${tempDb.absolutePath}")
            .use(block)
    }

    fun Statement.safeExecuteHugeInsert(hugeSql: String) {
        val regex = Regex("(?is)^\\s*(.*?VALUES\\s*)(.*?)\\s*;?\\s*$", RegexOption.DOT_MATCHES_ALL)
        val (_, prefix, dataPart) = regex.find(hugeSql)?.groupValues
            ?: throw IllegalArgumentException("Cannot parse INSERT statement")

        val records = dataPart.split(Regex("(?<=\\)),\\s*(?=\\()"))
        records.chunked(500).forEach { chunk ->
            execute(prefix + chunk.joinToString())
        }
    }

    fun Connection.importSqlDump(file: File) {
        val reader = file.inputStream().let {
            if (file.extension == "gz") GZIPInputStream(it) else it
        }.bufferedReader()
        importSqlDump(reader)
    }

    fun Connection.importSqlDump(reader: Reader) {
        createStatement().use { stmt ->
            reader.use { reader ->
                val scanner = Scanner(reader).useDelimiter("(?<=;)\\s*\\n")
                while (scanner.hasNext()) {
                    val sql = scanner.next()
                    when {
                        sql.isBlank() -> {}
                        sql.startsWith("INSERT") -> stmt.safeExecuteHugeInsert(sql)
                        else -> stmt.execute(sql)
                    }
                }
            }
        }
    }
}
