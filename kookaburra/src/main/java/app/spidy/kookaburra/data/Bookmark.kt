package app.spidy.kookaburra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark")
data class Bookmark(
    val title: String,
    val url: String,
    val createdAt: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}