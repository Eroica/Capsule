package app.gemicom.views

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import app.gemicom.R

interface ICancelListener {
    fun onCancel()
}

class CustomDialog(context: Context) : AlertDialog(context) {
    private var title: TextView? = null
    private var cancelButton: Button? = null
    private var container: FrameLayout? = null

    private var mTitle: String? = null
    private var mCancel: String? = null
    private var mView: View? = null

    private var listener: ICancelListener? = null

    /* Remember that show() can be called multiple times (e.g. when switching to and from apps),
       so view setup must only be done once. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_view)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

        title = findViewById(R.id.title)
        cancelButton = findViewById(R.id.cancelButton)
        container = findViewById(R.id.container)

        mTitle?.let { title?.text = it }
        mCancel?.let {
            cancelButton?.text = it
            cancelButton?.visibility = View.VISIBLE
            cancelButton?.setOnClickListener { listener?.onCancel() }
        }
        mView?.let { container?.addView(it) }
    }

    override fun dismiss() {
        listener = null
        super.dismiss()
    }

    fun setTitle(title: String): CustomDialog {
        mTitle = title
        return this
    }

    fun setCancel(cancel: String, listener: ICancelListener): CustomDialog {
        mCancel = cancel
        this.listener = listener
        return this
    }

    fun addView(view: View): CustomDialog {
        mView = view
        return this
    }
}
