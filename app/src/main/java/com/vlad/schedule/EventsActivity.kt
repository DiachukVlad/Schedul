package com.vlad.schedule

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList
import com.google.android.gms.ads.AdView
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import com.google.android.gms.ads.MobileAds;


class EventsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val EVENT_ADD = 1
    val EVENT_EDIT = 2
    var calendar = Calendar.getInstance()
    lateinit var sdPath : File

    var settings = Settings()
    var data = Data()
    var events : ArrayList<Event> = ArrayList()
    var day = 0

    lateinit var days:Array<String>

    lateinit private var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        sdPath = File(Environment.getExternalStorageDirectory(), "Android/data/Schedule")

        read()

        if(intent.getBooleanExtra("restartService", false)){
            restartService()
        }

        floatAdd.setOnClickListener {
            var intent = Intent(this, EditEvent().javaClass)
            intent.putExtra("day", day)
            startActivityForResult(intent, EVENT_ADD)
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        day = (calendar.get(Calendar.DAY_OF_WEEK)+5)%7

        days = arrayOf(resources.getString(R.string.monday),
                resources.getString(R.string.tuesday),
                resources.getString(R.string.wednesday),
                resources.getString(R.string.thursday),
                resources.getString(R.string.friday),
                resources.getString(R.string.saturday),
                resources.getString(R.string.sunday))

        content_main.post({
            toolbar.title = days[day]
            if(data.schedules.size>0)
                initIvents(day)
        })

        MobileAds.initialize(this, "ca-app-pub-8510756949517393~3661301232")

        mAdView = findViewById<View>(R.id.adView) as AdView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        startService(Intent(this, EventsNotificationService::class.java))

    }

    override fun onDestroy() {
        mAdView.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        mAdView.pause()
        super.onPause()
    }

    override fun onResume() {
        read()
        initIvents(day)
        super.onResume()
    }

    fun initIvents(day:Int, remove : Boolean = false){
        this.day = day
        var bbc = true

        if(remove)
            removeEvents()

        for (i in events){
            if(i.days[day]==1) {
                if(bbc){
                    removeEvents()
                    bbc=false
                }
                content_main.addView(EventView(i, this).view)
            }
        }
    }

    fun removeEvents(){
        content_main.removeAllViews()
    }

    fun read(){
        while(!sdPath.exists()) {
            sdPath.mkdirs()
            Thread.sleep(1000)
        }


        var file = File(sdPath, "event.data")

        while (!file.exists()) {
            file.createNewFile()
            Thread.sleep(1000)
        }

        var scn = Scanner(file)
        if(scn.hasNext())
            data = Gson().fromJson(scn.nextLine(), Data::class.java)

        //----settings

        var fileSettings = File(sdPath, "settings.data")
        if(!fileSettings.exists()) {
            fileSettings.createNewFile()

            var fws = FileWriter(fileSettings)
            fws.write(Gson().toJson(Settings()))
            fws.close()
        }
        scn = Scanner(fileSettings)
        settings = Gson().fromJson(scn.nextLine(), Settings::class.java)
        if(Locale.getDefault() != settings.locale)
            setLocale(settings.locale)
        //-----------

        if(data.schedules.size<1) {
            first()
            return
        }else {
            var scheduleFile = File(data.schedules[0].path)
            scn = Scanner(scheduleFile)
            var type = object : TypeToken<ArrayList<Event>>() {}.type
            if(scn.hasNext()) {
                var s = scn.nextLine()
                try {
                    events = Gson().fromJson(s, type)
                }catch (e : Exception){
                    type = object : TypeToken<ArrayList<OldEvent>>() {}.type
                    var ev : ArrayList<OldEvent> = Gson().fromJson(s, type)

                    for(e in ev){
                        var r = Event(e.startTime, e.finishTime)
                        r.name = e.name
                        r.note = e.note
                        r.date = e.date
                        r.days = e.days
                        r.ID = e.ID
                        r.color = e.color
                        r.numberRepeat = e.numberRepeat

                        if(e.isVibro)
                            r.isVibro = 2

                        events.add(r)
                    }

                    save()

                    val i = baseContext.packageManager
                            .getLaunchIntentForPackage(baseContext.packageName)
                    i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                }
            }
        }

    }

    fun save(){
        var fw = FileWriter(File(sdPath, "event.data"))
        var s = Gson().toJson(data)
        fw.write(s)
        fw.close()

        fw = FileWriter(File(data.schedules[0].path))

        s = Gson().toJson(events)
        fw.write(s)
        fw.close()
    }

    fun first(){
        var intent = Intent(this, SchedulesActivity::class.java)
        intent.putExtra("first", true)
        startActivity(intent)
    }

    fun setLocale(l : Locale) {
        Locale.setDefault(l)
        val config = Configuration()
        config.locale = l
        val resources = resources
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (intent == null) {
            return
        }
        if(!intent.hasExtra("event")) {
            return
        }
        var event : Event = Gson().fromJson<Event>(intent.getStringExtra("event"), Event::class.java)
        when(requestCode){
            EVENT_ADD -> {
                addEvent(event)
            }
            EVENT_EDIT -> {
                events.removeAt(event.ID)
                if(event.date==null)
                    event.date = Calendar.getInstance()
                addEvent(event)

                initIvents(day)
                save()
            }
        }

        restartService()
    }

    fun setID(){
        for(i in 0 until events.size){
            events[i].ID = i
        }
    }

    fun addEvent(event: Event){
        if(event.date!!.timeInMillis>Calendar.getInstance().timeInMillis){
            data.forFuture.add(event)
            save()
        }
        else {
            var i = 0
            var es = ArrayList<Event>()
            if (events.size > 0)
                while (events[i].startTime < event.startTime) {
                    es.add(events[i])
                    i++
                    if (i == events.size)
                        break
                }
            es.add(event)
            for (j in i until events.size)
                es.add(events[j])

            events = es
            setID()
            save()
            initIvents(day)
        }
    }

    fun restartService(){
        var intent = Intent(this, EventsNotificationService::class.java)
        stopService(intent)
        intent.putExtra("restart", true)
        startService(intent)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.schedules->{
                startActivity(Intent(this, SchedulesActivity::class.java))
                true
            }
            R.id.settings->{
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_monday -> initIvents(0, true)
            R.id.menu_tuesday -> initIvents(1, true)
            R.id.menu_wednesday -> initIvents(2, true)
            R.id.menu_thursday -> initIvents(3, true)
            R.id.menu_friday -> initIvents(4, true)
            R.id.menu_saturday -> initIvents(5, true)
            R.id.menu_sunday -> initIvents(6, true)
        }

        toolbar.title = item.title
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
