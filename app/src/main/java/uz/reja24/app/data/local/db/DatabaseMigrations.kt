package uz.reja24.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * MIGRATSIYA STRATEGIYASI
 *
 * Foydalanuvchi ilovani yangilaganda uning rejalari, ballari va kuboklari
 * YO'QOLMASLIGI shart. Shuning uchun:
 *
 * 1. Jadvalga o'zgartirish kiritilsa — RejaDatabase.VERSION bittaga oshiriladi.
 * 2. Har bir o'zgarish uchun shu yerga Migration yoziladi va ALL ro'yxatiga qo'shiladi.
 * 3. Har bir versiya sxemasi app/schemas/ papkasida saqlanadi — uni o'chirmang.
 * 4. "fallbackToDestructiveMigration" ISHLATILMAYDI (u ma'lumotni o'chiradi).
 *    Faqat versiya pastga tushganda (downgrade — odatda faqat sinovda) baza tozalanadi.
 */
object DatabaseMigrations {

    /** 1 → 2: rejalarga budilnik (yoqilganmi va qaysi ovoz). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE schedule_entries ADD COLUMN alarmEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE schedule_entries ADD COLUMN alarmSoundUri TEXT")
        }
    }

    /** 2 → 3: muhosaba (kunlik hisob) ro'yxati va kunlik belgilar. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `muhosaba_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `muhosaba_marks` (`itemId` INTEGER NOT NULL, `date` INTEGER NOT NULL, " +
                    "`done` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `date`), " +
                    "FOREIGN KEY(`itemId`) REFERENCES `muhosaba_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_muhosaba_marks_date` ON `muhosaba_marks` (`date`)")
        }
    }

    /** 3 → 4: rejaning "bajarilmadi" (✗) belgisi — muhosabadagi kun tartibi uchun. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `task_misses` (`entryId` INTEGER NOT NULL, `date` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`entryId`, `date`), " +
                    "FOREIGN KEY(`entryId`) REFERENCES `schedule_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_misses_entryId` ON `task_misses` (`entryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_misses_date` ON `task_misses` (`date`)")
        }
    }

    /** 5-versiya: namoz belgilari, kechki yozuv, kunduzgi nazorat (ilova chegarasi va foydalanish vaqti). */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `prayer_marks` (`date` INTEGER NOT NULL, `prayer` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, PRIMARY KEY(`date`, `prayer`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `day_notes` (`date` INTEGER NOT NULL, `good` TEXT NOT NULL, " +
                    "`improve` TEXT NOT NULL, PRIMARY KEY(`date`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_rules` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, " +
                    "`dailyLimitMinutes` INTEGER NOT NULL, `pauseOn` INTEGER NOT NULL, PRIMARY KEY(`packageName`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_usage` (`date` INTEGER NOT NULL, `packageName` TEXT NOT NULL, " +
                    "`seconds` INTEGER NOT NULL, PRIMARY KEY(`date`, `packageName`))"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
