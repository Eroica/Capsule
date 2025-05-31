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
import app.gemicom.R
import app.gemicom.platform.ViewRefs
import app.gemicom.views.CustomDialog
import com.google.android.material.textfield.TextInputEditText

interface IInputListener {
    fun onInput(input: String)
}

class InputDialogFragment : AppCompatDialogFragment() {
    companion object {
        private const val ARG_MESSAGE = "MESSAGE"
        private const val ARG_IS_SENSITIVE = "IS_SENSITIVE"

        operator fun invoke(message: String, isSensitive: Boolean = false): InputDialogFragment {
            val fragment = InputDialogFragment()
            fragment.arguments = bundleOf(
                ARG_MESSAGE to message,
                ARG_IS_SENSITIVE to isSensitive
            )
            return fragment
        }
    }

    private var listener: IInputListener? = null

    private val viewRefs = ViewRefs()
    private lateinit var button: () -> Button
    private lateinit var message: () -> TextView
    private lateinit var input: () -> TextInputEditText

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as IInputListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = CustomDialog(requireContext())
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        val layout = layoutInflater.inflate(R.layout.dialog_input, dialog.findViewById(R.id.container), false)
        dialog.setTitle(getString(R.string.dialog_title_input))
            .addView(layout)

        button = viewRefs.bind(layout, R.id.submitButton)
        message = viewRefs.bind(layout, R.id.message)
        input = viewRefs.bind(layout, R.id.inputField)

        if (requireArguments().getBoolean(ARG_IS_SENSITIVE)) {
            input().inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
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
        val message = arguments?.getString(ARG_MESSAGE)
        if (message != null) {
            message().text = message
        } else {
            message().visibility = View.GONE
        }
    }
}
