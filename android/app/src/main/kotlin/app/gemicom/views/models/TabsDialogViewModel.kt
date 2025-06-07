package app.gemicom.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gemicom.models.SqlTabs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance

class TabsDialogViewModel : ViewModel(), DIGlobalAware {
    private val Tabs: SqlTabs by instance()
    private val Dispatcher: CoroutineDispatcher by instance()
    private val Writer: CoroutineDispatcher by instance(tag = "WRITER")

    private val _tabs = MutableLiveData<List<ScopedTab>>()
    val tabs: LiveData<List<ScopedTab>> = _tabs

    val initialization: Job = viewModelScope.launch(Dispatcher) {
        val tabs = Tabs.all()
        _tabs.postValue(tabs.map { ScopedTab(it) })
    }

    suspend fun new() = withContext(Writer) {
        val tab = Tabs.new()
        val newTabs = _tabs.value.orEmpty().toMutableList()
        newTabs.add(ScopedTab(tab))
        _tabs.postValue(newTabs)
    }

    suspend fun close(tabId: Long) = withContext(Writer) {
        val closingTab = _tabs.value.orEmpty().first { it.id == tabId }
        closingTab.close()
        val updatedTabs = _tabs.value.orEmpty().toMutableList()
        updatedTabs.remove(closingTab)
        Tabs.delete(tabId)
        _tabs.postValue(updatedTabs)
    }

    suspend fun closeAll() = withContext(Writer) {
        _tabs.value.orEmpty().forEach { it.close() }
        Tabs.clear()
        _tabs.postValue(emptyList())
    }
}
