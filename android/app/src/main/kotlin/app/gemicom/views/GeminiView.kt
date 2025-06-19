package app.gemicom.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.gemicom.R
import app.gemicom.models.Anchor
import app.gemicom.models.AppSettings
import app.gemicom.models.IGeminiDocument
import app.gemicom.models.Image
import app.gemicom.ui.toSp
import app.gemicom.views.lists.GeminiAdapter
import app.gemicom.views.lists.IGemtextClickListener
import app.gemicom.views.lists.OuterMarginDecoration
import app.gemicom.views.lists.VerticalRhythmDecoration
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance

fun interface IViewInteraction {
    fun onInteracted()
}

class GeminiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle),
    IGemtextClickListener,
    DIGlobalAware {
    private val AppSettings: AppSettings by instance()

    private val list: RecyclerView
    private val adapter = GeminiAdapter(this, AppSettings)

    var listener: IGemtextClickListener? = null
    var scrollListener: IViewInteraction? = null

    init {
        inflate(context, R.layout.view_gemini, this)
        list = findViewById(android.R.id.list)
        list.addItemDecoration(VerticalRhythmDecoration(16f.toSp(context)))
        list.addItemDecoration(OuterMarginDecoration(16f.toSp(context)))
        list.setHasFixedSize(false)
        list.adapter = adapter
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        scrollListener?.onInteracted()
        return super.onInterceptTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        listener = null
        super.onDetachedFromWindow()
    }

    override fun onAnchorClicked(anchor: Anchor) {
        listener?.onAnchorClicked(anchor)
    }

    override fun onImageClicked(image: Image, imageView: ImageView) {
        listener?.onImageClicked(image, imageView)
    }

    fun show(document: IGeminiDocument) {
        adapter.submitList(document.blocks)
    }
}
