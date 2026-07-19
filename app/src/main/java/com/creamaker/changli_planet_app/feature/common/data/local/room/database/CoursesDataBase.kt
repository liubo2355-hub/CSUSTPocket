package com.creamaker.changli_planet_app.feature.common.data.local.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.creamaker.changli_planet_app.feature.common.data.local.entity.TimeTableMySubject
import com.creamaker.changli_planet_app.feature.common.data.local.room.converter.WeeksTypeConverter
import com.creamaker.changli_planet_app.feature.common.data.local.room.dao.CourseDao

@Database(entities = [TimeTableMySubject::class], version = 11, exportSchema = true)
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
    }
}
