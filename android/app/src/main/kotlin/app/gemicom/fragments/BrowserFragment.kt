package app.gemicom.fragments

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import app.gemicom.*
import app.gemicom.models.*
import app.gemicom.platform.*
import app.gemicom.ui.FluentInterpolator
import app.gemicom.views.GeminiView
import app.gemicom.views.IViewInteraction
import app.gemicom.views.TabsButton
import app.gemicom.views.lists.IGemtextClickListener
import app.gemicom.views.models.BrowserViewModel
import coil.ImageLoader
import coil.load
import kotlinx.coroutines.*
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance

class BrowserFragment : Fragment(R.layout.fragment_browser),
    TabsButton.IClickTabs,
    IGemtextClickListener,
    IInputListener,
    ISecurityDialogListener,
    ITabListener,
    IImagePool,
    DIGlobalAware {
    private val AppSettings: IPreferences by instance(tag = "AppSettings")
    private val DefaultCache: SqliteCache by instance(tag = "DEFAULT_CACHE")
    private val Certificates: ICertificates by instance()

    private val geminiClient = GeminiClient(Certificates)
    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(requireContext())
            .components {
                add(GeminiImageFetcher.Factory(geminiClient, this@BrowserFragment))
                add(GeminiImageKeyer())
            }
            .build()
    }

    private val viewModel: BrowserViewModel by viewModels()
    private val viewRefs = ViewRefs()
    private lateinit var tabsButton: () -> TabsButton
    private lateinit var addressBar: () -> EditText
    private lateinit var clearButton: () -> ImageView
    private lateinit var geminiView: () -> GeminiView
    private lateinit var progressBar: () -> ProgressBar
    private lateinit var bottomBarHeader: () -> ViewGroup
    private lateinit var homeButton: () -> Button

    private lateinit var co: CoroutineScope
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            co.launch { viewModel.back() }
        }
    }
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        viewModel.isLoading.value = false
        when (throwable) {
            is InvalidGeminiUri, is InvalidHostError, is InvalidGeminiResponse -> {
                geminiView().show(InvalidGeminiDocument)
                Toast.makeText(
                    context,
                    getString(R.string.browser_error_invalid_url),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is TooManyRedirects -> {
                geminiView().show(InvalidGeminiDocument)
                Toast.makeText(
                    context,
                    getString(R.string.browser_error_invalid_url),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is NoResponseError -> geminiView().show(EmptyGeminiDocument)

            is InvalidDocument -> geminiView().show(InvalidGeminiDocument)

            NoMoreHistory -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    if (backPressedCallback.isEnabled) {
                        backPressedCallback.isEnabled = false
                        Toast.makeText(context, getString(R.string.browser_exit_press), Toast.LENGTH_SHORT).show()
                        delay(1000)
                        backPressedCallback.isEnabled = true
                    }
                }
            }

            is InputRequired -> InputDialogFragment(throwable.currentUri, throwable.meta)
                .show(childFragmentManager, "Input")

            is SensitiveInputRequired -> InputDialogFragment(throwable.currentUri, throwable.meta, true)
                .show(childFragmentManager, "Input")

            is CertificateMismatchError -> {
                geminiView().show(SecurityIssueGeminiDocument)
                SecurityIssueDialogFragment(throwable.host, throwable.newHash)
                    .show(childFragmentManager, "SecurityIssue")
            }

            else -> throwable.printStackTrace()
        }
    }

    private var navigation: INavigation? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigation = context as INavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(view.findViewById(R.id.bottomBar))
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
        setupMenu()

        addressBar = viewRefs.bind(view, R.id.addressBar)
        clearButton = viewRefs.bind(view, R.id.addressBarClearButton)
        geminiView = viewRefs.bind(view, R.id.geminiView)
        tabsButton = viewRefs.bind(view, R.id.tabsButton)
        progressBar = viewRefs.bind(view, R.id.progressBar)
        bottomBarHeader = viewRefs.bind(view, R.id.bottomBarHeader)
        homeButton = viewRefs.bind(view, R.id.bottomHomeButton)

        co = viewLifecycleOwner.lifecycleScope + exceptionHandler
        co.launch {
            viewModel.initialization.join()
            setupBackpress()
            setupListeners()
            setupActionListeners()
            setupObservers()
            setupAddressBarTransition()
            viewModel.start()
        }
    }

    override fun onDestroyView() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(null)
        unfocusAddressBar()
        clearButton().setOnClickListener(null)
        addressBar().setOnEditorActionListener(null)
        addressBar().onFocusChangeListener = null
        tabsButton().listener = null
        geminiView().listener = null
        geminiView().scrollListener = null
        viewRefs.clear()
        super.onDestroyView()
    }

    override fun onDetach() {
        navigation = null
        super.onDetach()
    }

    override fun onTabsClicked() {
        TabsDialogFragment().show(childFragmentManager, "Tabs")
    }

    override fun onAnchorClicked(anchor: Anchor) {
        co.launch { viewModel.navigate(anchor.url, pushToHistory = true, isCheckCache = false) }
    }

    override fun onImageClicked(image: Image, imageView: ImageView) {
        viewModel.currentTab.value?.let {
            try {
                imageView.load(GeminiUri.fromAddress(it.resolve(image.url)), imageLoader) {
                    listener(onError = { _, _ ->
                        Toast.makeText(
                            context, getString(R.string.browser_load_image_error), Toast.LENGTH_SHORT
                        ).show()
                    })
                }
            } catch (_: InvalidGeminiUri) {
                imageView.load(image.url)
            }
        }
    }

    override fun onInput(input: String) {
        co.launch { viewModel.input(input) }
    }

    override fun onContinue(host: String, hash: String) {
        co.launch {
            viewModel.updateCertificate(host, hash)
            viewModel.currentTab.value?.let {
                viewModel.navigate(it.currentLocation, pushToHistory = false, isCheckCache = false)
            }
        }
    }

    override fun onTabSelected(id: Long) {
        co.launch { viewModel.select(id) }
    }

    override fun onTabClosed(id: Long) {
        co.launch { viewModel.selectNext(id) }
    }

    override fun onNewTab() {
        co.launch { viewModel.refresh() }
    }

    override fun onClosedAll() {
        co.launch { viewModel.reset() }
    }

    override fun getCache(): SqliteCache {
        return viewModel.currentTab.value?.cache ?: DefaultCache
    }

    private fun onStartNavigation() {
        addressBar().setText(addressBar().text.trim())
        co.launch { viewModel.navigate(addressBar().text.toString(), isCheckCache = false) }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    MenuCompat.setGroupDividerEnabled(menu, true)
                    menuInflater.inflate(R.menu.main_bottom_app_bar, menu)
                    setupMenuListeners(menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.browser_back -> co.launch { viewModel.back() }
                        R.id.browser_forward -> co.launch { viewModel.forward() }
                        R.id.browser_refresh -> co.launch {
                            viewModel.currentTab.value?.let {
                                viewModel.navigate(
                                    it.currentLocation, pushToHistory = false, isCheckCache = false
                                )
                            }
                        }

                        R.id.about -> navigation?.onAboutClick()
                        R.id.settings -> navigation?.onSettingsClick()
                        else -> return false
                    }

                    return true
                }
            },
            viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    private fun setupMenuListeners(menu: Menu) {
        val backItem = menu.findItem(R.id.browser_back)
        val forwardItem = menu.findItem(R.id.browser_forward)

        viewModel.currentUrl.observe(viewLifecycleOwner) {
            backItem.isEnabled = viewModel.currentTab.value?.canGoBack() ?: false
        }
        viewModel.currentUrl.observe(viewLifecycleOwner) {
            forwardItem.isEnabled = viewModel.currentTab.value?.canGoForward() ?: false
        }
    }

    private fun setupBackpress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun setupListeners() {
        clearButton().setOnClickListener { addressBar().setText("") }
        tabsButton().listener = this@BrowserFragment
        geminiView().listener = this@BrowserFragment
        geminiView().scrollListener = IViewInteraction { unfocusAddressBar() }
        homeButton().setOnClickListener {
            /* TODO Blocking */
            co.launch {
                val homeCapsule = AppSettings["home"] ?: ""
                if (homeCapsule.isBlank()) {
                    Toast.makeText(context, getString(R.string.browser_toast_set_home), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.navigate(homeCapsule)
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            SettingsFragment.RESULT_SETTINGS,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(SettingsFragment.ARG_CLEAR_CACHE)) {
                co.launch { viewModel.reset() }
            }
        }
    }

    private fun setupActionListeners() {
        addressBar().setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                unfocusAddressBar()
                onStartNavigation()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun setupObservers() {
        viewModel.document.observe(viewLifecycleOwner) {
            geminiView().show(it)
        }
        viewModel.tabs.observe(viewLifecycleOwner) {
            tabsButton().setCount(it?.size ?: 0)
        }
        viewModel.currentUrl.observe(viewLifecycleOwner) {
            addressBar().setText(it)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar().visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun setupAddressBarTransition() {
        val transition = ChangeBounds().apply {
            duration = 200
            interpolator = FluentInterpolator
        }
        addressBar().setOnFocusChangeListener { _, hasFocus ->
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
            bottomBarHeader().visibility = if (hasFocus) View.VISIBLE else View.GONE
        }
    }

    private fun unfocusAddressBar() {
        if (addressBar().isFocused) {
            addressBar().clearFocus()
            (requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(addressBar().windowToken, 0)
        }
    }
}
