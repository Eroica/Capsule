package app.gemicom.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import app.gemicom.R

class TabsButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    interface IClickTabs {
        fun onTabsClicked()
    }

    var listener: IClickTabs? = null

    private val textView: TextView

    init {
        inflate(context, R.layout.view_tabs_button, this)
        textView = findViewById(R.id.tabsText)
        textView.text = "0"
        setOnClickListener { listener?.onTabsClicked() }
    }

    fun setCount(count: Int) {
        if (count > 99) {
            textView.text = context.getString(R.string.browser_100_tabs)
        } else {
            textView.text = count.toString()
        }
    }
}
