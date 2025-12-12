package com.porvida

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.porvida.models.*
import com.porvida.daos.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        User::class,
        ServiceOrder::class,
        Service::class,
        Payment::class,
        Companion::class,
        Sede::class,
        Teacher::class,
        ClassSession::class,
        Enrollment::class,
        WaitlistEntry::class,
        TrainingRecord::class,
        ChatMessage::class,
        AssistantMessage::class,
        Note::class
    ],
    version = 13, // v9: add conversationId; v10: AssistantMessage; v11: Note; v12: Note.timeOfDayMinutes
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UsuarioDao
    abstract fun serviceOrderDao(): ServiceOrderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun companionDao(): CompanionDao
    abstract fun sedeDao(): SedeDao
    abstract fun teacherDao(): TeacherDao
    abstract fun classDao(): ClassDao
    abstract fun trainingDao(): TrainingDao
    abstract fun chatDao(): ChatDao
    abstract fun assistantMessageDao(): AssistantMessageDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {


                val MIGRATION_8_9 = object : Migration(8, 9) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        // Add conversationId with default "default" to ChatMessage
                        try {
                            db.execSQL("ALTER TABLE ChatMessage ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'default'")
                        } catch (_: Exception) { /* if table absent on old installs, ignore */ }
                    }
                }
                val MIGRATION_9_10 = object : Migration(9, 10) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `AssistantMessage` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                        )
                    }
                }
                val MIGRATION_10_11 = object : Migration(10, 11) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `Note` (" +
                                "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `details` TEXT NOT NULL, " +
                                "`dateMillis` INTEGER NOT NULL, `muscleGroup` TEXT NOT NULL, `injuryNotes` TEXT NOT NULL, " +
                                "`reminders` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                                "PRIMARY KEY(`id`))"
                        )
                    }
                }
                val MIGRATION_11_12 = object : Migration(11, 12) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        try {
                            db.execSQL("ALTER TABLE `Note` ADD COLUMN `timeOfDayMinutes` INTEGER")
                        } catch (_: Exception) { }
                    }
                }
                val MIGRATION_12_13 = object : Migration(12, 13) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        try {
                            db.execSQL("ALTER TABLE 'User' ADD COLUMN `planComment` TEXT")
                        } catch (_: Exception) { }
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "porvida_database"
                )
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    // For older installs (<8), perform a destructive reset to avoid crashes
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}