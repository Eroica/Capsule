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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_view)
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

    /* Show can be called multiple times, e.g. when switching to and from apps */
    override fun show() {
        super.show()
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
        )
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
