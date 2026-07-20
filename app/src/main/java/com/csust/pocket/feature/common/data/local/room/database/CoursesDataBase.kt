package com.csust.pocket.feature.common.data.local.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.csust.pocket.feature.common.data.local.entity.TimeTableMySubject
import com.csust.pocket.feature.common.data.local.room.converter.WeeksTypeConverter
import com.csust.pocket.feature.common.data.local.room.dao.CourseDao

@Database(entities = [TimeTableMySubject::class], version = 13, exportSchema = true)
@TypeConverters(WeeksTypeConverter::class)
abstract class CoursesDataBase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile
        private var INSTANCE: CoursesDataBase? = null
        fun getDatabase(context: Context) = INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                CoursesDataBase::class.java,
                "course_database"
            )
                .addMigrations(MIGRATION_10_11)
                .addMigrations(MIGRATION_11_12)
                .addMigrations(MIGRATION_12_13)
                .build()
            INSTANCE = instance
            instance
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DROP INDEX IF EXISTS `index_courses_courseName_classroom_teacher_start_step_weekday_term`"
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_courses_courseName_classroom_teacher_weeks_start_step_weekday_term_studentId_studentPassword`
                    ON `courses` (`courseName`, `classroom`, `teacher`, `weeks`, `start`, `step`, `weekday`, `term`, `studentId`, `studentPassword`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN credit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE courses ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE courses ADD COLUMN customColor INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE courses ADD COLUMN positionOverridden INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE courses ADD COLUMN positionOverrideKey TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
