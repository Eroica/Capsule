package app.gemicom.ui

import android.content.Context
import android.util.TypedValue

fun Float.toDp(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
    ).toInt()
}

fun Float.toSp(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics
    ).toInt()
}
