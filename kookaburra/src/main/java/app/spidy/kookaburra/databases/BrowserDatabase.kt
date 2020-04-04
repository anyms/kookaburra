package app.spidy.kookaburra.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.kookaburra.data.Bookmark
import app.spidy.kookaburra.data.History
import app.spidy.kookaburra.data.Tab
import app.spidy.kookaburra.interfaces.BrowserDao


@Database(entities = [Tab::class, Bookmark::class, History::class], version = 1, exportSchema = false)
abstract class BrowserDatabase: RoomDatabase() {
    abstract fun browserDao(): BrowserDao
}