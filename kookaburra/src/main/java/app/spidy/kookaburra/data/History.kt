package app.spidy.kookaburra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(
    val title: String,
    val url: String,
    val createdAt: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}