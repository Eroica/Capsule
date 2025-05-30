package app.gemicom.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import app.gemicom.BuildConfig
import app.gemicom.R
import com.google.android.material.transition.MaterialFadeThrough

class AboutFragment : Fragment(R.layout.fragment_about) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        enterTransition = MaterialFadeThrough().apply {
            targets += listOf(view.findViewById(android.R.id.list_container))
        }
        view.findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        }
        view.findViewById<TextView>(R.id.appVersion).text = BuildConfig.VERSION_NAME
        view.findViewById<TextView>(R.id.authorLink).movementMethod = LinkMovementMethod.getInstance()
        view.findViewById<TextView>(R.id.licenseReportText).text = readResource(R.raw.report)
        view.findViewById<TextView>(R.id.licenseApache).text = readResource(R.raw.apache)
        view.findViewById<TextView>(R.id.licenseKodein).text = readResource(R.raw.license_kodein)
        view.findViewById<TextView>(R.id.licenseSlf4j).text = readResource(R.raw.license_slf4j)
        startPostponedEnterTransition()
    }

    private fun readResource(resourceId: Int): String {
        return resources.openRawResource(resourceId)
            .bufferedReader()
            .use { it.readText() }
    }
}
