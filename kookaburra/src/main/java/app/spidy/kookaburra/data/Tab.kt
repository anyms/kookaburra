package app.spidy.kookaburra.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import app.spidy.kookaburra.fragments.WebviewFragment

@Entity(tableName = "tab")
data class Tab(
    var url: String,
    var title: String,
    @PrimaryKey var tabId: String,
    @Ignore var fragment: WebviewFragment? = null,
    @Ignore var favIcon: ByteArray? = null
) {

    constructor(url: String, title: String, tabId: String): this(url, title, tabId, null, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tab

        if (url != other.url) return false
        if (title != other.title) return false
        if (tabId != other.tabId) return false
        if (fragment != other.fragment) return false
        if (favIcon != null) {
            if (other.favIcon == null || favIcon == null) return false
            if (!favIcon!!.contentEquals(other.favIcon!!)) return false
        } else if (other.favIcon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + tabId.hashCode()
        result = 31 * result + (fragment?.hashCode() ?: 0)
        result = 31 * result + (favIcon?.contentHashCode() ?: 0)
        return result
    }
}