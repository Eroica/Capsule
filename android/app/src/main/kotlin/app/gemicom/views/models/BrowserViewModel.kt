package app.gemicom.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gemicom.models.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance

class BrowserViewModel : ViewModel(), DIGlobalAware {
    private val Tabs: ITabs by instance()
    private val Certificates: ICertificates by instance()
    private val Dispatcher: CoroutineDispatcher by instance()
    private val Writer: CoroutineDispatcher by instance(tag = "WRITER")

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentUrl = MutableLiveData("")
    val currentUrl: LiveData<String> = _currentUrl

    val _document = MutableLiveData<IGeminiDocument>()
    val document: LiveData<IGeminiDocument> = _document

    private val _tabs = MutableLiveData<List<ScopedTab>>()
    val tabs: LiveData<List<ScopedTab>> = _tabs

    private val _currentTab: MutableLiveData<ScopedTab> = MutableLiveData()
    val currentTab: LiveData<ScopedTab> = _currentTab

    val initialization: Job = viewModelScope.launch(Dispatcher) {
        val tabs = Tabs.all().map { ScopedTab(it) }

        if (tabs.isNotEmpty()) {
            val lastTab = tabs.last()
            _tabs.postValue(tabs)
            _currentTab.postValue(lastTab)
            _currentUrl.postValue(lastTab.currentLocation)
        } else {
            restart()
        }
    }

    suspend fun load() = withContext(Writer) {
        currentTab.value?.let {
            show(it)
        }
    }

    suspend fun start(address: String) {
        /* Whatever comes from here, act as if gemini:// was prepended to it */
        _currentTab.value?.let {
            if (address.startsWith("gemini://")) {
                navigate(address, pushToHistory = true, isCheckCache = false)
            } else {
                navigate("gemini://$address", pushToHistory = true, isCheckCache = false)
            }
        }
    }

    suspend fun restart() = withContext(Writer) {
        val tab = ScopedTab(Tabs.new())
        _tabs.postValue(listOf(tab))
        _currentTab.postValue(tab)
        _currentUrl.postValue("")
    }

    suspend fun back() = withContext(Writer) {
        _currentTab.value?.let {
            try {
                _document.postValue(it.back())
            } finally {
                _currentUrl.postValue(it.currentLocation)
            }
        }
    }

    suspend fun forward() = withContext(Writer) {
        _currentTab.value?.let {
            try {
                _document.postValue(it.forward())
            } finally {
                _currentUrl.postValue(it.currentLocation)
            }
        }
    }

    suspend fun input(query: String) {
        _currentTab.value?.let {
            val uri = GeminiHost.appendArgs(it.currentLocation, query)
            navigate(uri, pushToHistory = true, isCheckCache = true)
        }
    }

    suspend fun navigate(
        address: String, pushToHistory: Boolean = true, isCheckCache: Boolean = true
    ) = withContext(Writer) {
        _currentTab.value?.let {
            try {
                _isLoading.postValue(true)
                _document.postValue(it.navigate(address, pushToHistory, isCheckCache))
            } finally {
                _currentUrl.postValue(it.currentLocation)
                _isLoading.postValue(false)
            }
        }
    }

    suspend fun select(id: Long) = withContext(Writer) {
        /** @since 2025-06-06 If current tab is selected, no need to do anything */
        if (id == _currentTab.value?.id) {
            return@withContext
        }

        _tabs.value.orEmpty().firstOrNull { it.tab.id == id }?.let { scopedTab ->
            _currentTab.postValue(scopedTab)
            _currentUrl.postValue(scopedTab.currentLocation)
            show(scopedTab)
        }
    }

    /* "id" is the tab that was swiped, try to find the "next", or create a new one */
    suspend fun selectNext(id: Long) = withContext(Writer) {
        val index = _tabs.value.orEmpty().indexOfFirst { it.tab.id == id }
        val currentTabId = _currentTab.value?.id

        if (index != -1) {
            val updatedTabs = _tabs.value.orEmpty().toMutableList()
            updatedTabs.removeAt(index)
            _tabs.postValue(updatedTabs)

            if (updatedTabs.isEmpty()) {
                reset()
            } else if (id == currentTabId) {
                val nextTab = if (index == 0) {
                    updatedTabs[0]
                } else {
                    updatedTabs[index - 1]
                }

                select(nextTab.id)
            }

        } else {
            reset()
        }
    }

    /* Basically, just updates count of current tabs */
    suspend fun refresh() = withContext(Dispatcher) {
        val tabs = Tabs.all().map { ScopedTab(it) }
        _tabs.postValue(tabs)
    }

    suspend fun reset() = withContext(Writer) {
        val tab = Tabs.new()
        _tabs.postValue(listOf(ScopedTab(tab)))
        _currentTab.postValue(ScopedTab(tab))
        _currentUrl.postValue("")
        _document.postValue(EmptyGeminiDocument)
    }

    suspend fun updateCertificate(host: String, hash: String) = withContext(Writer) {
        Certificates.replace(host, hash)
    }

    private suspend fun show(tab: ScopedTab) {
        when (tab.status) {
            TabStatus.BLANK -> _document.postValue(EmptyGeminiDocument)
            TabStatus.VALID -> _document.postValue(tab.load(tab.currentLocation, false))
            TabStatus.INVALID -> _document.postValue(InvalidGeminiDocument)
        }
    }
}
