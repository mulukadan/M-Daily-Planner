package com.example.m_dailyplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Task::class, Project::class, ProjectTask::class, Note::class, NoteCategory::class],
    version = 6,
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectTaskDao(): ProjectTaskDao
    abstract fun noteDao(): NoteDao
    abstract fun noteCategoryDao(): NoteCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // v2 (original) → v3: added position column for manual task ordering
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3 → v4: removed category column (recreate table without it)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        reminderEnabled INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        priority TEXT NOT NULL DEFAULT 'Medium',
                        position INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO tasks_new (id, name, description, date, time, reminderEnabled, status, priority, position, createdAt)
                    SELECT id, name, description, date, time, reminderEnabled, status, priority, position, createdAt FROM tasks
                """.trimIndent())
                database.execSQL("DROP TABLE tasks")
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        // v4 → v5: added projects, project_tasks, and notes tables
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `project_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL DEFAULT '', `priority` TEXT NOT NULL DEFAULT 'Medium', `status` TEXT NOT NULL DEFAULT 'PENDING', `position` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        // v5 → v6: added note_categories table and categoryId column to notes
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL DEFAULT '#6750A4', `createdAt` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "INSERT INTO `note_categories` (`id`, `name`, `color`, `createdAt`) VALUES (1, 'General', '#6750A4', 1716000000000)"
                )
                database.execSQL(
                    "ALTER TABLE `notes` ADD COLUMN `categoryId` INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "mdailyplanner_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
