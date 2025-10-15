package com.demo.core.view.banner.indicator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 一个内置的、开箱即用的圆形指示器实现。
 * 它会显示一排圆点，并高亮显示与当前页面对应的圆点。
 *
 * @param context 上下文。
 * @param attrs 属性集。
 * @param defStyleAttr 默认样式属性。
 *
 * @example
 * ```kotlin
 * // 在你的 Activity/Fragment 中以编程方式创建
 * val banner = findViewById<BannerView>(R.id.banner)
 * val circleIndicator = CircleIndicator(this)
 *
 * // (可选) 自定义颜色
 * circleIndicator.setIndicatorColor(Color.GRAY)
 * circleIndicator.setSelectedIndicatorColor(Color.WHITE)
 *
 * banner.setIndicator(circleIndicator, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
 * ```
 */
class CircleIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Indicator {

    // 默认未选中圆点的颜色 (半透明白色)
    private var indicatorColor = 0x88FFFFFF.toInt()
    // 默认选中圆点的颜色 (不透明白色)
    private var selectedIndicatorColor = 0xFFFFFFFF.toInt()

    // 圆点的半径和间距，已转换为像素值
    private val indicatorRadius = dp2px(4f)
    private val indicatorSpacing = dp2px(8f)

    private var totalCount = 0
    private var currentPosition = 0

    // 用于绘制圆点的画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun getIndicatorView(): View = this

    override fun onPageChanged(totalItemCount: Int, currentPosition: Int) {
        this.totalCount = totalItemCount
        this.currentPosition = currentPosition
        // 数据变化后，请求重新测量自身尺寸并重绘
        requestLayout()
        invalidate()
    }

    /**
     * 设置未选中圆点的颜色。
     * @param color 颜色值 (例如, `Color.GRAY`)。
     */
    fun setIndicatorColor(color: Int) {
        this.indicatorColor = color
    }

    /**
     * 设置选中圆点的颜色。
     * @param color 颜色值 (例如, `Color.WHITE`)。
     */
    fun setSelectedIndicatorColor(color: Int) {
        this.selectedIndicatorColor = color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (totalCount == 0) {
            // 如果没有数据，则指示器尺寸为0
            setMeasuredDimension(0, 0)
            return
        }
        // 计算指示器所需的总宽度和总高度
        val width = (indicatorRadius * 2 * totalCount) + (indicatorSpacing * (totalCount - 1))
        val height = indicatorRadius * 2
        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (totalCount == 0) return

        val cy = height / 2f
        // 遍历并绘制所有圆点
        for (i in 0 until totalCount) {
            val cx = indicatorRadius + i * (indicatorRadius * 2 + indicatorSpacing)
            // 根据是否为当前位置，设置不同的颜色
            paint.color = if (i == currentPosition) selectedIndicatorColor else indicatorColor
            canvas.drawCircle(cx, cy, indicatorRadius, paint)
        }
    }

    private fun dp2px(dp: Float): Float {
        return context.resources.displayMetrics.density * dp
    }
}