package app.spidy.kookaburra.interfaces

import androidx.room.*
import app.spidy.kookaburra.data.Bookmark
import app.spidy.kookaburra.data.History
import app.spidy.kookaburra.data.Tab

@Dao
interface BrowserDao {

    /* Tab */

    @Query("SELECT * FROM tab")
    fun getTabs(): List<Tab>

    @Insert
    fun putTab(tab: Tab)

    @Update
    fun updateTab(tab: Tab)

    @Delete
    fun removeTab(tab: Tab)

    @Query("DELETE FROM tab")
    fun clearAllTabs()


    /* Bookmark */

    @Query("SELECT * FROM bookmark")
    fun getBookmarks(): List<Bookmark>

    @Insert
    fun putBookmark(bookmark: Bookmark)

    @Update
    fun updateBookmark(bookmark: Bookmark)

    @Delete
    fun removeBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmark")
    fun clearAllBookmarks()


    /* Bookmark */

    @Query("SELECT * FROM history ORDER BY id DESC LIMIT :limit")
    fun getHistories(limit: Int): List<History>

    @Insert
    fun putHistory(history: History)

    @Update
    fun updateHistory(history: History)

    @Delete
    fun removeHistory(history: History)

    @Query("DELETE FROM history")
    fun clearAllHistory()
}