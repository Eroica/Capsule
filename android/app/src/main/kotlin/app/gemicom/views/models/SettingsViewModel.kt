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

class SettingsViewModel : ViewModel(), DIGlobalAware {
    private val Documents: IDocuments by instance()
    private val Tabs: ITabs by instance()
    private val Certificates: ICertificates by instance()
    private val DefaultCache: SqliteCache by instance()
    private val AppSettings: AppSettings by instance()
    private val Dispatcher: CoroutineDispatcher by instance()
    private val Writer: CoroutineDispatcher by instance(tag = "WRITER")

    private val _isDarkTheme = MutableLiveData<Boolean>()
    val isDarkTheme: LiveData<Boolean> = _isDarkTheme

    private val _home = MutableLiveData<String>()
    val home: LiveData<String> = _home

    private val _isShowInline = MutableLiveData<Boolean>()
    val isShowInline: LiveData<Boolean> = _isShowInline

    val initialization: Job = viewModelScope.launch(Dispatcher) {
        _isDarkTheme.postValue(AppSettings.isDarkTheme)
        _home.postValue(AppSettings.home)
        _isShowInline.postValue(AppSettings.isShowImagesInline)
    }

    suspend fun setDarkTheme(isDark: Boolean) = withContext(Writer) {
        AppSettings.isDarkTheme = isDark
    }

    suspend fun setHome(home: String) = withContext(Writer) {
        AppSettings.home = home
    }

    suspend fun setShowImagesInline(isShowInline: Boolean) = withContext(Writer) {
        AppSettings.isShowImagesInline = isShowInline
    }

    suspend fun clearCertificates() = withContext(Writer) {
        Certificates.clear()
    }

    suspend fun clearCache() = withContext(Writer) {
        Documents.clear()
        Tabs.all().forEach { ScopedTab(it).close() }
        Tabs.clear()
        DefaultCache.purge()
    }

    suspend fun resetPreferences() = withContext(Writer) {
        AppSettings.clear()
        _home.postValue("")
        _isDarkTheme.postValue(false)
        _isShowInline.postValue(false)
    }
}
