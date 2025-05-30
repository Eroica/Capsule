package app.gemicom.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gemicom.models.ICertificates
import app.gemicom.models.IDocuments
import app.gemicom.models.IPreferences
import app.gemicom.models.ITabs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance

class SettingsViewModel : ViewModel(), DIGlobalAware {
    private val Documents: IDocuments by instance()
    private val Tabs: ITabs by instance()
    private val Certificates: ICertificates by instance()
    private val AppSettings: IPreferences by instance(tag = "AppSettings")
    private val Dispatcher: CoroutineDispatcher by instance()
    private val Writer: CoroutineDispatcher by instance(tag = "WRITER")

    private val _isDarkTheme = MutableLiveData<Boolean>()
    val isDarkTheme: LiveData<Boolean> = _isDarkTheme

    private val _home = MutableLiveData<String>()
    val home: LiveData<String> = _home

    val initialization: Job = viewModelScope.launch(Dispatcher) {
        _isDarkTheme.postValue(AppSettings["isDarkTheme"] == "1")
        _home.postValue(AppSettings["home"] ?: "")
    }

    suspend fun setDarkTheme(isDark: Boolean) = withContext(Writer) {
        AppSettings["isDarkTheme"] = if (isDark) "1" else "0"
    }

    suspend fun setHome(home: String) = withContext(Writer) {
        AppSettings["home"] = home
    }

    suspend fun clearCertificates() = withContext(Writer) {
        Certificates.clear()
    }

    suspend fun clearCache() = withContext(Writer) {
        Documents.clear()
        Tabs.all().forEach { ScopedTab(it).close() }
        Tabs.clear()
    }

    suspend fun resetPreferences() = withContext(Writer) {
        AppSettings["home"] = ""
        AppSettings["isDarkTheme"] = "0"
        _home.postValue("")
        _isDarkTheme.postValue(false)
    }
}
