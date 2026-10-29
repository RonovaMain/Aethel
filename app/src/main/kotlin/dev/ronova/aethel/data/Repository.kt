package dev.ronova.aethel.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.concurrent.Executors

object AethelJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    val pretty: Json = Json(from = json) {
        prettyPrint = true
    }
}

/**
 * Единое локальное хранилище. Вся база — один JSON-файл во внутренней памяти приложения.
 * Запись идёт в отдельном потоке и атомарно (через временный файл).
 */
class Repository(context: Context) {

    private val dir: File = context.filesDir
    private val file = File(dir, "aethel_db.json")
    private val tmpFile = File(dir, "aethel_db.json.tmp")

    private val state = MutableStateFlow(loadInitial())
    val db: StateFlow<AethelDb> = state.asStateFlow()

    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "aethel-db-writer") }

    private fun loadInitial(): AethelDb {
        if (!file.exists()) return AethelDb().normalized()
        return try {
            AethelJson.json.decodeFromString(AethelDb.serializer(), file.readText()).normalized()
        } catch (e: Exception) {
            try {
                file.copyTo(File(dir, "aethel_db.corrupt.json"), overwrite = true)
            } catch (_: Exception) {
            }
            AethelDb().normalized()
        }
    }

    fun update(block: (AethelDb) -> AethelDb) {
        state.update { block(it) }
        persist()
    }

    private fun persist() {
        writer.execute {
            try {
                val snapshot = state.value
                val text = AethelJson.json.encodeToString(AethelDb.serializer(), snapshot)
                tmpFile.writeText(text)
                if (!tmpFile.renameTo(file)) {
                    file.delete()
                    tmpFile.renameTo(file)
                }
            } catch (_: Throwable) {
            }
        }
    }

    /** Полная копия базы в читаемом JSON. */
    fun exportJson(): String =
        AethelJson.pretty.encodeToString(AethelDb.serializer(), state.value)

    /** Пулы в CSV. */
    fun exportPullsCsv(): String {
        val sb = StringBuilder("game,banner,rarity,name,featured,time\n")
        val zone = java.time.ZoneId.systemDefault()
        for (p in state.value.pulls.sortedBy { it.id }) {
            val time = java.time.Instant.ofEpochMilli(p.at).atZone(zone).toLocalDateTime().toString()
            val name = "\"" + p.name.replace("\"", "\"\"") + "\""
            sb.append("${p.game.name},${p.banner.name},${p.rarity},$name,${p.featured},$time\n")
        }
        return sb.toString()
    }

    /** Возвращает null при успехе или текст ошибки. */
    fun importJson(text: String): String? {
        val root = try {
            AethelJson.json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return "Файл не похож на резервную копию Aethel"
        }
        if (root["format"]?.jsonPrimitive?.contentOrNull != DbInfo.FORMAT) {
            return "Файл не похож на резервную копию Aethel"
        }
        val parsed = try {
            AethelJson.json.decodeFromString(AethelDb.serializer(), text)
        } catch (e: Exception) {
            return "Файл повреждён и не может быть прочитан"
        }
        if (parsed.schema > DbInfo.SCHEMA) {
            return "Копия создана более новой версией приложения"
        }
        state.value = parsed.normalized().copy(schema = DbInfo.SCHEMA)
        persist()
        return null
    }

    fun resetAll() {
        state.value = AethelDb().normalized()
        persist()
    }
}
