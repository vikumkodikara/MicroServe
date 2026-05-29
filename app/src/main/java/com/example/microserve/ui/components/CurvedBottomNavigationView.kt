package com.example.microserve.ui.components

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.microserve.R

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Visual elements using original PNG assets
    private val navBase = ImageView(context)
    private val navSubtract = ImageView(context)
    private val activeEllipse = ImageView(context)
    private val navActiveIcon = ImageView(context)
    private val tabsContainer = LinearLayout(context)

    private val tabs = mutableListOf<TabItem>()
    var currentActiveIndex = -1
        private set

    var onTabSelectedListener: ((Int) -> Unit)? = null

    // Configurations
    private val density = context.resources.displayMetrics.density
    private val navBaseHeight = 70f * density
    private val navSubtractWidth = 120f * density
    private val navSubtractHeight = 80f * density
    private val ellipseSize = 60f * density
    private val activeIconSize = 38f * density

    init {
        // Prevent clipping so the active puck can float above the top boundary
        clipChildren = false
        clipToPadding = false

        // 1. Purple Base Bar Background
        navBase.setImageResource(R.drawable.rectanglenav)
        navBase.scaleType = ImageView.ScaleType.FIT_XY
        addView(navBase, LayoutParams(LayoutParams.MATCH_PARENT, navBaseHeight.toInt(), Gravity.BOTTOM))

        // 2. White Cutout Notch Overlay
        navSubtract.setImageResource(R.drawable.subtractnav)
        navSubtract.scaleType = ImageView.ScaleType.FIT_CENTER
        addView(navSubtract, LayoutParams(navSubtractWidth.toInt(), navSubtractHeight.toInt(), Gravity.TOP or Gravity.LEFT))

        // 3. Dark Active Puck Circle Background
        activeEllipse.setImageResource(R.drawable.ellipsenav)
        activeEllipse.scaleType = ImageView.ScaleType.FIT_CENTER
        activeEllipse.setColorFilter(Color.BLACK)
        addView(activeEllipse, LayoutParams(ellipseSize.toInt(), ellipseSize.toInt(), Gravity.TOP or Gravity.LEFT))

        // 4. Floating Active Tab Icon
        navActiveIcon.scaleType = ImageView.ScaleType.FIT_CENTER
        navActiveIcon.elevation = 10f * density
        addView(navActiveIcon, LayoutParams(activeIconSize.toInt(), activeIconSize.toInt(), Gravity.TOP or Gravity.LEFT))

        // 5. Tabs Layout Container
        tabsContainer.orientation = LinearLayout.HORIZONTAL
        tabsContainer.weightSum = 5f
        addView(tabsContainer, LayoutParams(LayoutParams.MATCH_PARENT, navBaseHeight.toInt(), Gravity.BOTTOM))

        setupTabs()
    }

    private fun setupTabs() {
        // The 5 tabs matching request, service, home, post, profile
        val tabData = listOf(
            Triple("Request", R.drawable.navrequest, R.string.nav_request),
            Triple("Service", R.drawable.navservice, R.string.nav_service),
            Triple("Home", R.drawable.navhome, R.string.nav_home),
            Triple("Post", R.drawable.navpost, R.string.nav_post),
            Triple("Profile", R.drawable.navprofile, R.string.nav_profile)
        )

        for ((index, data) in tabData.withIndex()) {
            val tabLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                setPadding(0, 0, 0, (6 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener {
                    if (currentActiveIndex != index) {
                        animateToTab(index)
                        onTabSelectedListener?.invoke(index)
                    }
                }
            }

            // Static Icon inside tab
            val staticIcon = ImageView(context).apply {
                setImageResource(data.second)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LayoutParams((26 * density).toInt(), (26 * density).toInt()).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }

            // Label text inside tab
            val label = TextView(context).apply {
                text = context.getString(data.third)
                setTextColor(Color.WHITE)
                textSize = 10f
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = (2 * density).toInt()
                }
            }

            tabLayout.addView(staticIcon)
            tabLayout.addView(label)
            tabsContainer.addView(tabLayout)

            tabs.add(TabItem(tabLayout, staticIcon, label, data.second))
        }
    }

    fun setInitialTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        currentActiveIndex = index

        post {
            val targetX = getTabCenterX(index)

            // Instantly position elements at the active tab center
            navSubtract.translationX = targetX - navSubtractWidth / 2f
            activeEllipse.translationX = targetX - ellipseSize / 2f
            navActiveIcon.translationX = targetX - activeIconSize / 2f

            // Set active floating icon resource
            val activeTab = tabs[index]
            navActiveIcon.setImageResource(activeTab.iconRes)

            // Instantly hide active tab static icon, and show inactive ones
            for (i in tabs.indices) {
                tabs[i].staticIcon.alpha = if (i == index) 0f else 1f
            }
        }
    }

    private fun animateToTab(index: Int) {
        val oldIndex = currentActiveIndex
        currentActiveIndex = index

        val oldX = if (oldIndex != -1) getTabCenterX(oldIndex) else getTabCenterX(index)
        val newX = getTabCenterX(index)
        val newTab = tabs[index]

        // Set active icon source before transition starts
        navActiveIcon.setImageResource(newTab.iconRes)

        // Fade in old active static icon, and fade out new active static icon
        if (oldIndex != -1) {
            tabs[oldIndex].staticIcon.animate().alpha(1f).setDuration(200).start()
        }
        newTab.staticIcon.animate().alpha(0f).setDuration(200).start()

        val animators = mutableListOf<android.animation.Animator>()

        // 1. Sliding horizontal animation for cutout, puck, and floating active icon
        val slideAnimator = ValueAnimator.ofFloat(oldX, newX).apply {
            duration = 320
            interpolator = OvershootInterpolator(0.7f) // Elegant premium spring slide
            addUpdateListener {
                val currX = it.animatedValue as Float
                navSubtract.translationX = currX - navSubtractWidth / 2f
                activeEllipse.translationX = currX - ellipseSize / 2f
                navActiveIcon.translationX = currX - activeIconSize / 2f
            }
        }
        animators.add(slideAnimator)

        // 2. Vertical overshoot and scale "pop" bounce animation
        val popAnimator = ValueAnimator.ofFloat(0.8f, 1.15f, 1.0f).apply {
            duration = 320
            addUpdateListener {
                val scale = it.animatedValue as Float
                navActiveIcon.scaleX = scale
                navActiveIcon.scaleY = scale
                activeEllipse.scaleX = scale
                activeEllipse.scaleY = scale
            }
        }
        animators.add(popAnimator)

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    private fun getTabCenterX(index: Int): Float {
        val tabWidth = width / 5f
        return (index + 0.5f) * tabWidth
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Align and position visual elements vertically relative to actual view height
        navSubtract.y = (h - navBaseHeight) - 15f * density
        activeEllipse.y = navSubtract.y - 20f * density
        navActiveIcon.y = activeEllipse.y + (ellipseSize - activeIconSize) / 2f

        // Reposition horizontally once layout sizes are finalized
        if (currentActiveIndex != -1) {
            val targetX = getTabCenterX(currentActiveIndex)
            navSubtract.translationX = targetX - navSubtractWidth / 2f
            activeEllipse.translationX = targetX - ellipseSize / 2f
            navActiveIcon.translationX = targetX - activeIconSize / 2f
        }
    }

    private data class TabItem(
        val container: LinearLayout,
        val staticIcon: ImageView,
        val label: TextView,
        val iconRes: Int
    )
}
