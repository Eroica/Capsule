package app.gemicom.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import app.gemicom.R
import app.gemicom.controllers.CustomDialog
import app.gemicom.platform.ViewRefs
import com.google.android.material.textfield.TextInputEditText

interface IInputListener {
    fun onInput(input: String)
}

class InputDialogFragment : AppCompatDialogFragment() {
    companion object {
        const val TAG = "InputDialogFragment"
        private const val ARG_URI = "URI"
        private const val ARG_MESSAGE = "MESSAGE"
        private const val ARG_IS_SENSITIVE = "IS_SENSITIVE"

        operator fun invoke(
            uri: String, message: String, isSensitive: Boolean = false
        ): InputDialogFragment {
            val fragment = InputDialogFragment()
            fragment.arguments = bundleOf(
                ARG_URI to uri,
                ARG_MESSAGE to message,
                ARG_IS_SENSITIVE to isSensitive
            )
            return fragment
        }
    }

    private var listener: IInputListener? = null

    private val viewRefs = ViewRefs()
    private lateinit var button: () -> Button
    private lateinit var explanation: () -> TextView
    private lateinit var message: () -> TextView
    private lateinit var input: () -> TextInputEditText

    private lateinit var uri: String
    private lateinit var meta: String
    private var isSensitive: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as IInputListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = CustomDialog(requireContext())
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        val layout = layoutInflater.inflate(R.layout.dialog_input, dialog.findViewById(R.id.container), false)
        dialog.setTitle(getString(R.string.dialog_input_title))
            .addView(layout)

        viewRefs.setRoot(layout)
        button = viewRefs.bind(R.id.submitButton)
        explanation = viewRefs.bind(R.id.explanation)
        message = viewRefs.bind(R.id.message)
        input = viewRefs.bind(R.id.inputField)

        requireArguments().apply {
            uri = getString(ARG_URI) ?: throw IllegalArgumentException("No URI in arguments")
            meta = getString(ARG_MESSAGE) ?: throw IllegalArgumentException("No message in arguments")
            isSensitive = getBoolean(ARG_IS_SENSITIVE)
        }

        setupMessage()
        setupClickListener()

        return dialog
    }

    override fun onDestroyView() {
        viewRefs.clear()
        super.onDestroyView()
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    private fun setupClickListener() {
        button().setOnClickListener {
            listener?.onInput(input().text.toString())
            dismiss()
        }
    }

    private fun setupMessage() {
        if (meta.isBlank()) {
            explanation().text = HtmlCompat.fromHtml(
                getString(R.string.dialog_input_explanation_no_meta, uri),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            message().visibility = View.GONE
        } else {
            explanation().text = HtmlCompat.fromHtml(
                getString(R.string.dialog_input_explanation, uri),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            message().text = meta
        }

        if (isSensitive) {
            input().inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
}
