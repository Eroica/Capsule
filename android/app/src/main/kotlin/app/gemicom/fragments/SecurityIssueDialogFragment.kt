package app.gemicom.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import app.gemicom.R
import app.gemicom.models.ICertificates
import app.gemicom.platform.ViewRefs
import app.gemicom.controllers.CustomDialog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance
import java.time.format.DateTimeFormatter

interface ISecurityDialogListener {
    fun onContinue(host: String, hash: String)
}

class SecurityIssueDialogFragment : AppCompatDialogFragment(), DIGlobalAware {
    companion object {
        private const val ARG_HOST = "HOST"
        private const val ARG_HASH = "HASH"

        operator fun invoke(host: String, hash: String): SecurityIssueDialogFragment {
            val fragment = SecurityIssueDialogFragment()
            fragment.arguments = bundleOf(
                ARG_HOST to host,
                ARG_HASH to hash
            )
            return fragment
        }
    }

    private val Certificates: ICertificates by instance()
    private val Dispatcher: CoroutineDispatcher by instance()

    private var listener: ISecurityDialogListener? = null

    private val viewRefs = ViewRefs()
    private lateinit var button: () -> Button
    private lateinit var message: () -> TextView
    private lateinit var checkBox: () -> CheckBox

    private lateinit var host: String
    private lateinit var hash: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as ISecurityDialogListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().apply {
            host = getString(ARG_HOST) ?: throw IllegalArgumentException("No host in arguments")
            hash = getString(ARG_HASH) ?: throw IllegalArgumentException("No hash in arguments")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = CustomDialog(requireContext())
        dialog.window?.let {
            it.setBackgroundDrawableResource(android.R.color.transparent)
            it.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }

        val layout = layoutInflater.inflate(R.layout.dialog_security_issue, dialog.findViewById(R.id.container), false)
        dialog.setTitle(getString(R.string.dialog_security_issue_title)).addView(layout)

        button = viewRefs.bind(layout, R.id.submitButton)
        message = viewRefs.bind(layout, R.id.message)
        checkBox = viewRefs.bind(layout, R.id.confirmCheckbox)

        lifecycleScope.launch {
            setupMessage()
            setupClickListener()
        }

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

    private suspend fun setupMessage() {
        val createdAt = withContext(Dispatcher) { Certificates[host].second }
        val formatter = DateTimeFormatter.ofPattern(getString(R.string.tab_created_at_format))
        message().movementMethod = LinkMovementMethod.getInstance()
        message().text = HtmlCompat.fromHtml(
            getString(R.string.dialog_security_issue_explanation, host, createdAt.format(formatter)),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).trim()
    }

    private fun setupClickListener() {
        checkBox().setOnCheckedChangeListener { _, isChecked ->
            button().isEnabled = isChecked
        }
        button().setOnClickListener {
            listener?.onContinue(host, hash)
            dismiss()
        }
    }
}
