package com.shortstack.hackertracker.ui.activities


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.github.stkent.amplify.tracking.Amplify
import com.orhanobut.logger.Logger
import com.shortstack.hackertracker.App
import com.shortstack.hackertracker.BuildConfig
import com.shortstack.hackertracker.R
import com.shortstack.hackertracker.analytics.AnalyticsController
import com.shortstack.hackertracker.database.DatabaseManager
import com.shortstack.hackertracker.ui.MainActivityViewModel
import com.shortstack.hackertracker.ui.ReviewBottomSheet
import com.shortstack.hackertracker.ui.schedule.EventBottomSheet
import com.shortstack.hackertracker.utils.SharedPreferencesUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.android.synthetic.main.row_nav_view.*
import java.util.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    @Inject
    lateinit var storage: SharedPreferencesUtil

    @Inject
    lateinit var database: DatabaseManager

    @Inject
    lateinit var analytics: AnalyticsController

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        App.application.myComponent.inject(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        setupNavigation()

        val mainActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        mainActivityViewModel.conference.observe(this, Observer {
            if (it != null) {
                nav_view.getHeaderView(0).nav_title.text = it.title
            }
        })

        filter.setOnClickListener { onFilterClick() }
//        close.setOnClickListener { onFilterClick() }

        if (savedInstanceState == null) {
            if (Amplify.getSharedInstance().shouldPrompt() && !BuildConfig.DEBUG) {
                val review = ReviewBottomSheet.newInstance()
                review.show(this.supportFragmentManager, review.tag)
            }
        }

        // TODO: Remove, this is only for debugging.
        Logger.d("Created MainActivity " + (System.currentTimeMillis() - App.application.timeToLaunch))

    }

    private fun setupNavigation() {
        navController = findNavController(R.id.mainNavigationFragment)
        setupActionBarWithNavController(navController, drawerLayout = drawer_layout)

        navController.addOnNavigatedListener { _, destination ->
            //            val visibility = if (destination.id == R.id.nav_schedule) View.VISIBLE else View.INVISIBLE
//            setFABVisibility(visibility)
        }

        initNavDrawer()
    }

    private fun initNavDrawer() {
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onSupportNavigateUp() = findNavController(R.id.mainNavigationFragment).navigateUp()

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        theme.applyStyle(R.style.AppTheme, true)
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
                    val fragment = EventBottomSheet.newInstance(item)
                    fragment.show(supportFragmentManager, fragment.tag)
                }, {})

    }

    private fun onFilterClick() {
//        toggleFAB(onClick = true)

        database.getCons().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    val cons = it.filter { !it.isSelected }
                    val con = cons[Random().nextInt(cons.size)]

                    Toast.makeText(this@MainActivity, "Changed to ${con.title}", Toast.LENGTH_SHORT).show()

                    database.changeConference(con)

                }, {

                })


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

    private fun setFABVisibility(visibility: Int, showFilters: Boolean = false) {
        if (!filter.isAttachedToWindow) {
            return
        }

        val cx = filter.width / 2
        val cy = filter.height / 2


        val radius = Math.hypot(cx.toDouble(), cy.toDouble())

        if (visibility == View.VISIBLE) {

            filter.visibility = View.VISIBLE

            val anim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewAnimationUtils.createCircularReveal(filter, cx, cy, 0f, radius.toFloat())
            } else {
                null
            }


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
                    if (showFilters) toggleFilters()
                }
            })

            anim?.start()
        }
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
        if (drawer_layout.isDrawerOpen(Gravity.START)) {
            drawer_layout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                navController.navigate(R.id.nav_search)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val current = navController.currentDestination.id
        if (item.itemId != current) {
            navController.navigate(item.itemId)
        }

        drawer_layout.closeDrawers()
        return true
    }
}
