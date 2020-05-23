package app.spidy.kookaburra.data

import android.graphics.drawable.Drawable

data class OptionMenuItem(
    val title: String,
    val icon: Drawable?,
    val menuId: Int,
    val showAsAction: Int,
    val callback: () -> Unit
)