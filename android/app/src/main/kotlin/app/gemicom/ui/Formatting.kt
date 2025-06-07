package app.gemicom.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.LeadingMarginSpan
import android.view.View
import androidx.core.graphics.withTranslation
import app.gemicom.models.Anchor
import app.gemicom.views.lists.IGemtextClickListener

class ClickableAnchor(
    private val anchor: Anchor,
    private val color: Int,
    private var listener: IGemtextClickListener?
) : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.color = color
        ds.isUnderlineText = true
    }

    override fun onClick(widget: View) {
        listener?.onAnchorClicked(anchor)
    }
}

/**
 * Copy of [android.text.style.BulletSpan] from android SDK 28 with removed internal code
 */
class ImprovedBulletSpan(
    val bulletRadius: Int = STANDARD_BULLET_RADIUS,
    val gapWidth: Int = STANDARD_GAP_WIDTH,
    val color: Int = STANDARD_COLOR
) : LeadingMarginSpan {
    companion object {
        private const val STANDARD_BULLET_RADIUS = 4
        private const val STANDARD_GAP_WIDTH = 2
        private const val STANDARD_COLOR = 0
    }

    private var mBulletPath: Path? = null

    override fun getLeadingMargin(first: Boolean): Int {
        return 2 * bulletRadius + gapWidth
    }

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean,
        layout: Layout?
    ) {
        val bottom = bottom
        if ((text as Spanned).getSpanStart(this) == start) {
            val style = paint.style
            val oldColor = paint.color

            paint.style = Paint.Style.FILL
            if (color != STANDARD_COLOR) {
                paint.color = color
            }

            val yPosition = if (layout != null) {
                val line = layout.getLineForOffset(start)
                layout.getLineBaseline(line).toFloat() - bulletRadius * 2f
            } else {
                (top + bottom) / 2f
            }

            val xPosition = (x + dir * bulletRadius).toFloat()

            if (canvas.isHardwareAccelerated) {
                if (mBulletPath == null) {
                    mBulletPath = Path()
                    mBulletPath!!.addCircle(0.0f, 0.0f, bulletRadius.toFloat(), Direction.CW)
                }

                canvas.withTranslation(xPosition, yPosition) {
                    drawPath(mBulletPath!!, paint)
                }
            } else {
                canvas.drawCircle(xPosition, yPosition, bulletRadius.toFloat(), paint)
            }

            paint.style = style
            paint.color = oldColor
        }
    }
}

fun bulletedText(html: CharSequence, bulletRadius: Int, gap: Int): CharSequence {
    val spannableBuilder = SpannableStringBuilder(html)
    val bulletSpans = spannableBuilder.getSpans(0, spannableBuilder.length, BulletSpan::class.java)
    bulletSpans.forEach {
        val start = spannableBuilder.getSpanStart(it)
        val end = spannableBuilder.getSpanEnd(it)
        spannableBuilder.removeSpan(it)
        spannableBuilder.setSpan(
            ImprovedBulletSpan(bulletRadius, gap),
            start,
            end,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }

    return spannableBuilder
}
