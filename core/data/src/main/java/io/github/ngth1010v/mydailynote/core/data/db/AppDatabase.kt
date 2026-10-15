package io.github.ngth1010v.mydailynote.core.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EntryEntity::class, EntryFts::class, GroupEntity::class, ActionEntity::class],
    version = 2,
    exportSchema = true,
    // 2: group order/default/category columns and the actions table.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun groupDao(): GroupDao

    companion object {
        const val NAME = "my_daily_note.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
