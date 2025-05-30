package app.gemicom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.kodein.di.bindings.ScopeRegistry

interface IContext {
    val co: CoroutineScope
    var scope: ScopeRegistry?
}

class DefaultContext : IContext, AutoCloseable {
    override var scope: ScopeRegistry? = null
    override val co = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun close() {
        co.cancel()
    }
}
