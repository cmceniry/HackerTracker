package com.shortstack.hackertracker.ui.activities


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.AsyncLayoutInflater
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.*
import com.github.stkent.amplify.tracking.Amplify
import com.orhanobut.logger.Logger
import com.shortstack.hackertracker.App
import com.shortstack.hackertracker.BuildConfig
import com.shortstack.hackertracker.Event.ChangeConEvent
import com.shortstack.hackertracker.R
import com.shortstack.hackertracker.analytics.AnalyticsController
import com.shortstack.hackertracker.database.DatabaseManager
import com.shortstack.hackertracker.replaceFragment
import com.shortstack.hackertracker.ui.ReviewBottomSheet
import com.shortstack.hackertracker.ui.SearchFragment
import com.shortstack.hackertracker.ui.SettingsFragment
import com.shortstack.hackertracker.ui.home.HomeFragment
import com.shortstack.hackertracker.ui.information.InformationFragment
import com.shortstack.hackertracker.ui.maps.MapsFragment
import com.shortstack.hackertracker.ui.schedule.ScheduleFragment
import com.shortstack.hackertracker.ui.schedule.ScheduleItemBottomSheet
import com.shortstack.hackertracker.ui.vendors.VendorsFragment
import com.shortstack.hackertracker.utils.SharedPreferencesUtil
import com.squareup.otto.Subscribe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alert_filter.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.android.synthetic.main.row_nav_view.*
import javax.inject.Inject


open class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mFragmentIndex = DEFAULT_FRAGMENT_INDEX

    @Inject
    lateinit var storage: SharedPreferencesUtil

    @Inject
    lateinit var database: DatabaseManager

    @Inject
    lateinit var analytics: AnalyticsController

    override fun onCreate(savedInstanceState: Bundle?) {
        App.application.myComponent.inject(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        AsyncLayoutInflater(this@MainActivity).inflate(R.layout.row_nav_view, drawer_layout) { view, resid, parent ->
            parent?.addView(view)
            initViewPager(view as NavigationView)
        }


        filter.setOnClickListener { onFilterClick() }

//        close.setOnClickListener { onFilterClick() }

        if (savedInstanceState == null) {


            // TODO: Remove, this is only for debugging.
            mFragmentIndex = if (BuildConfig.DEBUG) {
                DEFAULT_FRAGMENT_INDEX
            } else {
                storage.viewPagerPosition
            }
            forceMenuHighlighted()
            loadFragment()

            if (Amplify.getSharedInstance().shouldPrompt() && !BuildConfig.DEBUG) {
                val review = ReviewBottomSheet.newInstance()
                review.show(this.supportFragmentManager, review.tag)
            }
        }

//        handleIntent(intent)


//        setNavHeaderMargin()

//        if (App.application.database.db.initialized)
//            addConferenceMenuItems()

        // TODO: Remove, this is only for debugging.
        Logger.d("Created MainActivity " + (System.currentTimeMillis() - App.application.timeToLaunch))

    }

    override fun onDestroy() {
        Logger.d("onDestroy")
//        App.application.unregisterBusListener(this)

        super.onDestroy()
    }


//    @Subscribe
//    fun onDatabaseSetupEvent(event: SetupDatabaseEvent) {
//        addConferenceMenuItems()
//    }

    @Subscribe
    fun onChangeConEvent(event: ChangeConEvent) {
        database.getCons()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    val current = it.first { it.isSelected }

                    App.application.updateTheme(current)

                    nav_view.getHeaderView(0).nav_title.text = current.title

                    val menu = nav_view.menu


                    it.forEach {
                        val item = menu.findItem(400 + it.index)
                        if (item == null) {
                            val item = menu.add(Menu.NONE, 400 + it.index, 3, it.title)
                            item.icon = ContextCompat.getDrawable(this, R.drawable.ic_chevron_right_white_24dp)
                        }
                    }

                    menu.removeItem(current.index + 400)

                }, {

                })
    }

    private fun setNavHeaderMargin() {
        val params = nav_view.getHeaderView(0).imageView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = getStatusBarHeight()
    }


    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        theme.applyStyle(storage.databaseTheme, true)
        return theme
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.extras == null)
            return

        val target = intent.extras.getInt("target")

        if (target == 0)
            return

        database.findItem(id = target)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ item ->
                    val fragment = ScheduleItemBottomSheet.newInstance(item)
                    fragment.show(supportFragmentManager, fragment.tag)
                }, {})

    }

    private fun forceMenuHighlighted() {
        val menu = nav_view?.menu ?: return

        if (menu.size() > mFragmentIndex)
            menu.getItem(mFragmentIndex).isChecked = true
    }

    private fun loadFragment() {
        val timeToLoad = System.currentTimeMillis()

//        if (supportFragmentManager.contains(fragmentTag)) {
//            drawer_layout!!.closeDrawers()
//            return
//        }


        // TODO: Rewrite, currentFragment will create a new Fragment!
        replaceFragment(currentFragment, fragmentTitle, fragmentTag, R.id.frame)

        storage.viewPagerPosition = mFragmentIndex
        analytics.tagCustomEvent(fragmentEvent)

        //updateFABVisibility()

        //Closing drawer on item click
        drawer_layout.closeDrawers()
    }

    private val fragmentEvent: AnalyticsController.Analytics
        get() {
            return when (mFragmentIndex) {
                NAV_HOME -> AnalyticsController.Analytics.FRAGMENT_HOME

                NAV_SCHEDULE -> AnalyticsController.Analytics.FRAGMENT_SCHEDULE

                NAV_MAP -> AnalyticsController.Analytics.FRAGMENT_MAP

                NAV_INFORMATION -> AnalyticsController.Analytics.FRAGMENT_INFO

                NAV_VENDORS -> AnalyticsController.Analytics.FRAGMENT_COMPANIES

                NAV_SETTINGS -> AnalyticsController.Analytics.FRAGMENT_SETTINGS

                else -> throw IllegalStateException("Could not locale the correct fragment from index $mFragmentIndex.")
            }
        }

    private fun updateFABVisibility() {
        toggleFAB()
    }

    private val currentFragment: Fragment
        get() {
            return when (mFragmentIndex) {
                NAV_HOME -> HomeFragment.newInstance()

                NAV_SCHEDULE -> ScheduleFragment.newInstance()

                NAV_MAP -> MapsFragment.newInstance()

                NAV_INFORMATION -> InformationFragment.newInstance()

                NAV_VENDORS -> VendorsFragment.newInstance()

                NAV_SETTINGS -> SettingsFragment.newInstance()

                else -> throw IllegalStateException("Could not locate the correct fragment for index $mFragmentIndex.")
            }
        }


    private fun onFilterClick() {
        toggleFAB(onClick = true)
    }

    private fun toggleFilters() {


//        val cx = filters.width / 2
//        val cy = filters.height / 2
//
//        val position = IntArray(2)
//
//        filter.getLocationOnScreen(position)
//
//        val (cx, cy) = position
//
//        val radius = Math.hypot(cx.toDouble(), cy.toDouble())
//
//        if (filters.visibility == View.INVISIBLE) {
//
//
//            val anim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                ViewAnimationUtils.createCircularReveal(filters, cx, cy, 0f, radius.toFloat())
//            } else {
//                null
//            }
//
//            filters.visibility = View.VISIBLE
//
//            anim?.start()
//        } else {
//
//            val anim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                ViewAnimationUtils.createCircularReveal(filters, cx, cy, radius.toFloat(), 0f)
//            } else {
//
//                filters.visibility = View.INVISIBLE
//                null
//            }
//
//            anim?.addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator?) {
//                    super.onAnimationEnd(animation)
//                    filters.visibility = View.INVISIBLE
//                    toggleFAB(onClick = false)
//                }
//            })
//
//            anim?.start()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.search_menu, menu)
        return true
    }

    private fun toggleFAB(onClick: Boolean = false) {


        val cx = filter.width / 2
        val cy = filter.height / 2


        val radius = Math.hypot(cx.toDouble(), cy.toDouble())

        if (filter.visibility == View.INVISIBLE) {


            val anim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewAnimationUtils.createCircularReveal(filter, cx, cy, 0f, radius.toFloat())
            } else {
                null
            }

            filter.visibility = View.VISIBLE

            anim?.start()
        } else {

            val anim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewAnimationUtils.createCircularReveal(filter, cx, cy, radius.toFloat(), 0f)
            } else {

                filter.visibility = View.INVISIBLE
                null
            }

            anim?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    filter.visibility = View.INVISIBLE
                    if (onClick) toggleFilters()

                }
            })

            anim?.start()
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawers()
            return
        }

//        if (filters.visibility == View.VISIBLE) {
//            onFilterClick()
//            return
//        }

        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                val fragment = SearchFragment.newInstance()

                val searchView = item.actionView as SearchView
                searchView.setOnQueryTextListener(fragment)

                replaceFragment(fragment, fragmentTitle, fragmentTag, R.id.frame)

                item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                        loadFragment()
                        return true
                    }
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun initViewPager(nav_view: NavigationView) {

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.setDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId in 400..500) {
            database.changeConference(item.itemId - 400)
            drawer_layout.closeDrawers()
        }

        setFragmentIndex(item)
        loadFragment()

        return true
    }

    private fun setFragmentIndex(item: MenuItem) {
        mFragmentIndex = getFragmentIndex(item)
    }

    private fun getFragmentIndex(item: MenuItem): Int {
        when (item.itemId) {
            R.id.nav_home -> return NAV_HOME
            R.id.nav_schedule -> return NAV_SCHEDULE
            R.id.nav_map -> return NAV_MAP
            R.id.nav_information -> return NAV_INFORMATION

            R.id.nav_companies -> return NAV_VENDORS
            R.id.nav_settings -> return NAV_SETTINGS
        }

        throw IllegalStateException("Could not find fragment with id: ${item.itemId}.")
    }

    private val fragmentTag: String
        get() = "home_fragment_" + mFragmentIndex

    private val fragmentTitle: String
        get() {
            return resources.getStringArray(R.array.nav_item_activity_titles)[mFragmentIndex]
        }

    fun loadFragment(fragment: Int) {
        mFragmentIndex = fragment
        forceMenuHighlighted()
        loadFragment()
    }

    protected fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    companion object {

        val DEFAULT_FRAGMENT_INDEX = 1
        val NAV_HOME = 0
        val NAV_SCHEDULE = 1
        val NAV_MAP = 2
        val NAV_INFORMATION = 3
        val NAV_VENDORS = 4
        val NAV_SETTINGS = 5
    }
}
