package uz.reja24.app.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.reja24.app.core.di.IoDispatcher
import uz.reja24.app.data.local.db.RejaDatabase
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zaxira nusxa: barcha rejalar, Muhosaba yozuvlari, sozlamalar bitta .zip faylga yoziladi
 * va o'sha fayldan qayta tiklanadi (telefon almashsa yoki ilova o'chirib qayta o'rnatilsa).
 *
 * Fayl ichida: ma'lumotlar bazasi, sozlamalar fayli va backup.json (qaysi versiyadan olingani).
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: RejaDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    sealed interface RestoreResult {
        data object Ok : RestoreResult
        /** Bu 24/7 zaxira fayli emas yoki buzilgan. */
        data object InvalidFile : RestoreResult
        /** Fayl ilovaning yangiroq versiyasidan — avval ilovani yangilash kerak. */
        data object NewerVersion : RestoreResult
    }

    /** Hamma ma'lumotni uri ga yozadi. */
    suspend fun export(uri: Uri) = withContext(ioDispatcher) {
        // WAL faylidagi so'nggi o'zgarishlar asosiy faylga o'tkaziladi — nusxa to'liq bo'lsin
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile = context.getDatabasePath(RejaDatabase.NAME)
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("no output")
        ZipOutputStream(output.buffered()).use { zip ->
            val meta = JSONObject()
                .put("app", APP_ID)
                .put("dbVersion", RejaDatabase.VERSION)
                .put("createdAt", System.currentTimeMillis())
            zip.putNextEntry(ZipEntry(META))
            zip.write(meta.toString().toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DB_ENTRY))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            prefsFile().takeIf { it.exists() }?.let { prefs ->
                zip.putNextEntry(ZipEntry(PREFS_ENTRY))
                prefs.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Fayldan tiklaydi. Muvaffaqiyatli bo'lsa, ilova qayta ishga tushirilishi kerak ([restartApp]) —
     * ochiq baza va sozlamalar xotirasi eski ma'lumotda qolmasin.
     */
    suspend fun restore(uri: Uri): RestoreResult = withContext(ioDispatcher) {
        val tempDir = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            var meta: JSONObject? = null
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext RestoreResult.InvalidFile
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        META -> meta = runCatching { JSONObject(zip.readBytes().decodeToString()) }.getOrNull()
                        DB_ENTRY, PREFS_ENTRY -> File(tempDir, entry.name).outputStream().use { zip.copyTo(it) }
                        // Boshqa nomdagi fayllar e'tiborga olinmaydi (zip ichidagi yo'l orqali hujum bo'lmasin)
                    }
                    zip.closeEntry()
                }
            }
            val info = meta
            val dbTemp = File(tempDir, DB_ENTRY)
            if (info == null || info.optString("app") != APP_ID || !dbTemp.exists() || !isSqlite(dbTemp)) {
                return@withContext RestoreResult.InvalidFile
            }
            if (info.optInt("dbVersion", Int.MAX_VALUE) > RejaDatabase.VERSION) return@withContext RestoreResult.NewerVersion

            // Bazani yopib, fayllarni almashtiramiz (eski versiyadagi baza ochilganda migratsiya o'zi ishlaydi)
            database.close()
            val dbFile = context.getDatabasePath(RejaDatabase.NAME)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            dbTemp.copyTo(dbFile, overwrite = true)
            File(tempDir, PREFS_ENTRY).takeIf { it.exists() }?.let { it.copyTo(prefsFile(), overwrite = true) }
            RestoreResult.Ok
        } catch (e: IOException) {
            RestoreResult.InvalidFile
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /** Ilovani qaytadan ochadi (tiklangandan keyin). */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private fun prefsFile(): File = File(context.filesDir, "datastore/$PREFS_NAME.preferences_pb")

    private fun isSqlite(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(SQLITE_HEADER.length)
        input.read(header) == header.size && header.decodeToString() == SQLITE_HEADER
    }

    companion object {
        const val APP_ID = "uz.reja24.app"
        const val PREFS_NAME = "reja24_prefs"
        private const val META = "backup.json"
        private const val DB_ENTRY = "reja24.db"
        private const val PREFS_ENTRY = "prefs.preferences_pb"
        private const val SQLITE_HEADER = "SQLite format 3"

        /** Fayl nomi: "24-7-zaxira-2026-11-19.zip". */
        fun fileName(date: java.time.LocalDate): String = "24-7-zaxira-$date.zip"
    }
}
