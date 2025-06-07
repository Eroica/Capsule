package app.gemicom.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.gemicom.R
import app.gemicom.platform.ViewRefs
import app.gemicom.platform.onClickLaunch
import app.gemicom.views.models.SettingsViewModel
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    companion object {
        const val RESULT_SETTINGS = "SETTINGS"
        const val ARG_CLEAR_CACHE = "CLEAR_CACHE"
    }

    private val viewModel: SettingsViewModel by viewModels()

    private val viewRefs = ViewRefs()

    private lateinit var darkThemeSwitch: () -> SwitchCompat
    private lateinit var homeField: () -> EditText
    private lateinit var clearCertificatesButton: () -> Button
    private lateinit var clearCacheButton: () -> Button
    private lateinit var resetAllButton: () -> Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = MaterialFadeThrough().apply {
            targets += listOf(view.findViewById(android.R.id.list_container))
        }
        view.findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        }
        startPostponedEnterTransition()

        darkThemeSwitch = viewRefs.bind(view, R.id.darkThemeSwitch)
        homeField = viewRefs.bind(view, R.id.homeCapsule)
        clearCertificatesButton = viewRefs.bind(view, R.id.buttonClearCertificates)
        clearCacheButton = viewRefs.bind(view, R.id.buttonClearCache)
        resetAllButton = viewRefs.bind(view, R.id.buttonResetAll)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.initialization.join()
            setupObservers()
            setupListeners()
        }
    }

    override fun onStop() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setHome(homeField().text.toString().trim())
        }
        super.onStop()
    }

    override fun onDestroyView() {
        viewRefs.clear()
        super.onDestroyView()
    }

    private fun setupObservers() {
        viewModel.isDarkTheme.observe(viewLifecycleOwner) { isDarkTheme ->
            if (darkThemeSwitch().isChecked != isDarkTheme) {
                darkThemeSwitch().isChecked = isDarkTheme
            }
        }
        viewModel.home.observe(viewLifecycleOwner) {
            if (it != homeField().text.toString()) {
                homeField().setText(it)
            }
        }
    }

    private fun setupListeners() {
        darkThemeSwitch().setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { viewModel.setDarkTheme(isChecked) }
        }
        clearCertificatesButton().onClickLaunch(viewLifecycleOwner.lifecycleScope) {
            viewModel.clearCertificates()
            Toast.makeText(context, getString(R.string.settings_toast_certificates_cleared), Toast.LENGTH_SHORT).show()
        }
        clearCacheButton().onClickLaunch(viewLifecycleOwner.lifecycleScope) {
            viewModel.clearCache()
            Toast.makeText(context, getString(R.string.settings_toast_cache_cleared), Toast.LENGTH_SHORT).show()
            parentFragmentManager.setFragmentResult(
                RESULT_SETTINGS, bundleOf(
                    ARG_CLEAR_CACHE to true
                )
            )
        }
        resetAllButton().onClickLaunch(viewLifecycleOwner.lifecycleScope) {
            viewModel.resetPreferences()
        }
    }
}
