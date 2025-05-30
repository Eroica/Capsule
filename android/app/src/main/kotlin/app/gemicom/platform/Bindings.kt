package app.gemicom.platform

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ViewRefs {
    private val views = mutableMapOf<Int, View>()

    fun <T : View> bind(root: View, @IdRes id: Int): () -> T {
        @Suppress("UNCHECKED_CAST")
        return {
            val view = views[id] ?: root.findViewById<T>(id).also { views[id] = it }
            view as T
        }
    }

    fun clear() {
        views.clear()
    }
}

inline fun View.onClickLaunch(scope: CoroutineScope, crossinline block: suspend () -> Unit) {
    setOnClickListener {
        scope.launch { block() }
    }
}

fun EditText.textChanges(): Flow<CharSequence?> = callbackFlow {
    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            trySend(s)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
    addTextChangedListener(watcher)
    awaitClose { removeTextChangedListener(watcher) }
}
