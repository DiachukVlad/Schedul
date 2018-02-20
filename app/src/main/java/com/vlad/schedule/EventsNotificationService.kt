package com.vlad.schedule

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Environment
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList
import android.content.ComponentName
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.net.wifi.WifiManager
import android.support.v4.app.FragmentActivity
import android.telephony.TelephonyManager
import android.util.Log


class EventsNotificationService : Service() {
    override fun onBind(intent: Intent): IBinder? {return null }

    var restart = false
    lateinit var intent : Intent

    var data = Data()
    var settings = Settings()

    private var runnable = true

    var allEvents: ArrayList<Event> = ArrayList()
    var events: ArrayList<Event> = ArrayList()

    var runningEvents : ArrayList<Event> = ArrayList()
    lateinit var calendar: Calendar

    lateinit var sdPath : File
    var day = 0
    var minute = 0

    private lateinit var mNotificationManager: NotificationManager

    var currentTime = 0

    private val START_EVENT = 1
    private val FINISH_EVENT = 2

    private var toEndS = ""
    private var toStartS = ""
    private var hourS = ""
    private var minuteS = ""
    private var nextS = ""

    private var SYSTEM_RINGER_MODE: Int = 0
    private var SYSTEM_WIFI: Boolean = false
    private var SYSTEM_BLUETOOTH: Boolean = false
    private var SYSTEM_MOBILEDATA: Boolean = false

    private lateinit var mAudioManager: AudioManager
    private var mWifiManager: WifiManager? = null
    private var mBluetoothManager: BluetoothAdapter? = null

    private lateinit var vibrator: Vibrator
    private var changingRingMode = false
    private var changingWifiMode = false
    private var changingBluetoothMode = false

    private lateinit var receiver: BroadcastReceiver
    private lateinit var wifireceiver: BroadcastReceiver
    private lateinit var bluetoothreceiver: BroadcastReceiver

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            this.intent = intent!!
            restart = intent.getBooleanExtra("restart", false)
        }
        return super.onStartCommand(intent, flags, startId)
    }


    override fun onCreate() {

        sdPath = File(Environment.getExternalStorageDirectory(), "Android/data/Schedule")
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        calendar = Calendar.getInstance()
        day = (calendar.get(Calendar.DAY_OF_WEEK)+5)%7
        minute = calendar.get(Calendar.MINUTE)

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            mWifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
            SYSTEM_WIFI = mWifiManager!!.isWifiEnabled
        }catch (e : Exception){e.printStackTrace()}

        try {
            mBluetoothManager = BluetoothAdapter.getDefaultAdapter()
            SYSTEM_BLUETOOTH = mBluetoothManager!!.isEnabled
        }catch (e : Exception){e.printStackTrace()}

        SYSTEM_RINGER_MODE = mAudioManager.ringerMode
        //SYSTEM_MOBILEDATA = getMobileDataState()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!changingRingMode) {
                    SYSTEM_RINGER_MODE = mAudioManager.ringerMode
                }
            }
        }
        registerReceiver(receiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

        wifireceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!changingWifiMode && mWifiManager!= null) {
                    SYSTEM_WIFI = mWifiManager!!.isWifiEnabled
                }
            }
        }
        val filters = IntentFilter()
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        //filters.addAction("android.net.wifi.STATE_CHANGE")
        registerReceiver(wifireceiver, filters)

        bluetoothreceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!changingBluetoothMode && mBluetoothManager!= null) {
                    SYSTEM_BLUETOOTH = mBluetoothManager!!.isEnabled
                }
            }
        }
        registerReceiver(bluetoothreceiver, IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED))

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        read()
        forFuture()
        initEvents()
        initAlarm()

        toEndS = resources.getString(R.string.to_end)
        toStartS = resources.getString(R.string.to_start)
        hourS = resources.getString(R.string.hour)
        minuteS = resources.getString(R.string.minute)
        nextS = resources.getString(R.string.next_event)

        Thread({run()}).start()
        super.onCreate()
    }

    override fun onDestroy() {
        mNotificationManager.cancelAll()
        runnable = false

        if(settings.systemSetings) {
            if (mAudioManager.ringerMode != SYSTEM_RINGER_MODE) {
                changingRingMode = true
                mAudioManager.ringerMode = SYSTEM_RINGER_MODE
                Thread.sleep(1000)
                changingRingMode = false
            }

            if (mBluetoothManager!=null) {
                if (mBluetoothManager!!.isEnabled != SYSTEM_BLUETOOTH) {
                    changingBluetoothMode = true

                    if (!SYSTEM_BLUETOOTH)
                        mBluetoothManager!!.disable()

                    if (SYSTEM_BLUETOOTH)
                        mBluetoothManager!!.enable()

                    Thread.sleep(1000)
                    changingBluetoothMode = false
                }
            }

            if (mWifiManager!=null) {
                if (mWifiManager!!.isWifiEnabled != SYSTEM_WIFI) {
                    changingWifiMode = true
                    mWifiManager!!.isWifiEnabled = SYSTEM_WIFI
                    Thread.sleep(1000)
                    changingWifiMode = false
                }
            }
        }

        try {
            unregisterReceiver(receiver)
            unregisterReceiver(wifireceiver)
            unregisterReceiver(bluetoothreceiver)
        }catch (e:Exception){e.printStackTrace()}

        super.onDestroy()
    }


    private fun initEvents(){
        events.clear()
        allEvents
                .filter { it.days[day]==1 }
                .forEach {events.add(it) }
    }

    private fun initAlarm() {
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val p = PendingIntent.getService(this, 0, Intent(this, EventsNotificationService::class.java), 0)

        alarm.cancel(p)

        for (event in events) {
            if (event.startTime > currentTime) {
                val c = Calendar.getInstance()
                c.set(Calendar.HOUR_OF_DAY, event.startTime / 60)
                c.set(Calendar.MINUTE, event.startTime % 60)
                alarm.set(AlarmManager.RTC_WAKEUP, c.timeInMillis, p)
                break
            } else if (event.finishTime > currentTime) {
                val c = Calendar.getInstance()
                c.set(Calendar.HOUR_OF_DAY, event.finishTime / 60)
                c.set(Calendar.MINUTE, event.finishTime % 60)
                alarm.set(AlarmManager.RTC_WAKEUP, c.timeInMillis, p)
                break
            }
        }
    }

    fun onStartEvent(event:Event){
        mNotificationManager.cancel(event.ID)
        runningEvents.add(event)

        if(settings.vibroNotification && !restart) {
            vibrator.vibrate(100)
            Thread.sleep(300)
            vibrator.vibrate(100)
            Thread.sleep(300)
            vibrator.vibrate(100)
        }
        if(restart) {
            restart = false
        }

        if(settings.systemSetings) {

            setVibro(event.isVibro)
            setWifi(event.isWifi)
            setBluetooth(event.isBluetooth)

        }
    }

    fun onFinishEvent(event:Event){
        if(settings.systemSetings) {
            if ((event.isVibro == 2 || event.isVibro == 0) && mAudioManager.ringerMode != SYSTEM_RINGER_MODE) {
                changingRingMode = true
                mAudioManager.ringerMode = SYSTEM_RINGER_MODE
                Thread.sleep(1000)
                changingRingMode = false
            }

            if(mBluetoothManager!=null) {
                if ((event.isBluetooth == 2 || event.isBluetooth == 0) && mBluetoothManager!!.isEnabled != SYSTEM_BLUETOOTH) {
                    changingBluetoothMode = true

                    if (!SYSTEM_BLUETOOTH)
                        mBluetoothManager!!.disable()

                    if (SYSTEM_BLUETOOTH)
                        mBluetoothManager!!.enable()

                    Thread.sleep(1000)
                    changingBluetoothMode = false
                }
            }
            if(mWifiManager!=null) {
                if ((event.isWifi == 2 || event.isWifi == 0) && mWifiManager!!.isWifiEnabled != SYSTEM_WIFI) {
                    changingWifiMode = true
                    mWifiManager!!.isWifiEnabled = SYSTEM_WIFI
                    Thread.sleep(1000)
                    changingWifiMode = false
                }
            }
        }
        runningEvents.remove(event)
        mNotificationManager.cancel(event.ID)
        runningEvents.remove(event)

        if(settings.vibroNotification) {
            vibrator.vibrate(100)
            Thread.sleep(300)
            vibrator.vibrate(100)
            Thread.sleep(300)
            vibrator.vibrate(100)
        }

        if(event.numberRepeat>0){
            event.numberRepeat--
            if(event.numberRepeat<=0) {
                events.clear()
                runningEvents.clear()
                allEvents.remove(event)
                save()
                read()
                initEvents()
                initAlarm()
            }
        }


    }

    fun save(){
        var fw = FileWriter(File(data.schedules[0].path))
        var s = Gson().toJson(allEvents)
        fw.write(s)
        fw.close()

        fw = FileWriter(File(sdPath, "event.data"))
        s = Gson().toJson(data)
        fw.write(s)
        fw.close()

        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTaskInfo = manager.getRunningTasks(1)
        val componentInfo = runningTaskInfo[0].topActivity
        if(componentInfo.getPackageName().equals("com.vlad.schedule")){
            var intent = Intent(this, EventsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    fun notifBuild(){
        val resultIntent = Intent(this, EventsActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(EventsActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        mNotificationManager.cancelAll()

        if(runningEvents.size==0){
            for(event in events){
                var timeToStart = event.startTime - currentTime
                var stoStart = "$timeToStart $minuteS"
                if(timeToStart>60){
                    stoStart = "${timeToStart/60} $hourS ${timeToStart%60} $minuteS"
                }
                if(timeToStart in 1..60){
                    var nb = NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setOngoing(true)
                            .setContentTitle("$toStartS ${event.name} $stoStart")
                            .setContentIntent(resultPendingIntent)
                    mNotificationManager.notify(event.ID, nb.build())
                    break
                }
            }
        }

        for(i in 0 until runningEvents.size){
            var nextEvent : Event? = null
            var event = runningEvents[i]
            if(events.size>events.indexOf(event)+1)
                nextEvent = events[events.indexOf(event)+1]

            var timeToEnd = event.finishTime-currentTime
            var stoEnd = "$timeToEnd $minuteS"
            if(timeToEnd>60){
                stoEnd = "${timeToEnd/60} $hourS ${timeToEnd%60} $minuteS"
            }

            var nb = NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setContentTitle("${event.name} $toEndS $stoEnd")
                    .setContentIntent(resultPendingIntent)

            if (nextEvent != null) {
                val timeToStart = nextEvent.startTime - currentTime
                var stoStart = "$timeToStart $minuteS"
                if(timeToStart>60){
                    stoStart = "${timeToStart/60} $hourS ${timeToStart%60} $minuteS"
                }
                if(timeToStart>0)
                    nb.setContentText("$nextS ${nextEvent.name}. $toStartS $stoStart")
            }

            mNotificationManager.notify(event.ID, nb.build())
        }
    }

    private fun run(){
        while (runnable){
            minute = calendar.get(Calendar.MINUTE)
            if(day != (calendar.get(Calendar.DAY_OF_WEEK)+5)%7) {
                day = (calendar.get(Calendar.DAY_OF_WEEK)+5)%7
                initEvents()
                forFuture()
            }

            currentTime = calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE)
            for (e in events) when(e.tick(currentTime)){
                START_EVENT -> onStartEvent(e)
                FINISH_EVENT -> onFinishEvent(e)
            }

            if(settings.notification)
                notifBuild()
            while(minute == calendar.get(Calendar.MINUTE)) {
                calendar = Calendar.getInstance()
                Thread.sleep(400)
            }
        }
    }

    fun forFuture(b : Boolean = false){
        for(e in data.forFuture){
            if(calendar.timeInMillis>=e.date!!.timeInMillis){
                allEvents.add(e)
                data.forFuture.remove(e)

                forFuture(true)
                return
            }
        }
        if(b) {
            events.clear()
            runningEvents.clear()

            initEvents()
            initAlarm()
            save()
        }
    }

    fun setLocale(l : Locale) {
        Locale.setDefault(l)
        val config = Configuration()
        config.locale = l
        val resources = resources
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun read(){
        var file = File(sdPath, "event.data")
        var scn = Scanner(file)
        if(!scn.hasNext()) {
            onDestroy()
            return
        }

        data = Gson().fromJson(scn.nextLine(), Data::class.java)
        if(data.schedules.size==0){
            onDestroy()
            return
        }

        var scheduleFile = File(data.schedules[0].path)
        scn = Scanner(scheduleFile)
        val type = object : TypeToken<ArrayList<Event>>() {}.type
        if(scn.hasNext())
            allEvents = Gson().fromJson(scn.nextLine(), type)

        //---settings
        var fileSettings = File(sdPath, "settings.data")
        scn = Scanner(fileSettings)
        settings = Gson().fromJson(scn.nextLine(), Settings::class.java)

        setLocale(settings.locale)
    }


    fun setVibro(state : Int){
        if (state == 0) {
            changingRingMode = true
            mAudioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Thread.sleep(1000)
            changingRingMode = false
        }
        if (state == 2) {
            changingRingMode = true
            mAudioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            mAudioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            Thread.sleep(1000)
            changingRingMode = false
        }
    }

    fun setWifi(state : Int){
        if(mWifiManager==null)
            return
        if (state == 0) {
            changingWifiMode = true
            mWifiManager!!.isWifiEnabled = false
            Thread.sleep(1000)
            changingWifiMode = false
        }
        if (state == 2) {
            changingWifiMode = true
            mWifiManager!!.isWifiEnabled = true
            Thread.sleep(1000)
            changingWifiMode = false
        }
    }

    fun setBluetooth(state : Int){
        if(mBluetoothManager==null)
            return

        if (state == 0) {
            changingBluetoothMode = true
            mBluetoothManager!!.disable()
            Thread.sleep(1000)
            changingBluetoothMode = false
        }
        if (state == 2) {
            changingRingMode = true
            mBluetoothManager!!.enable()
            Thread.sleep(1000)
            changingRingMode = false
        }
    }

    fun setMobileDataState(mobileDataEnabled: Boolean) {
        try {
            val telephonyService = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val setMobileDataEnabledMethod = telephonyService.javaClass.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)

            setMobileDataEnabledMethod?.invoke(telephonyService, mobileDataEnabled)
        } catch (ex: Exception) {
            Log.e("Log", "Error setting mobile data state", ex)
        }

    }

    fun getMobileDataState(): Boolean {
        try {
            val telephonyService = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val getMobileDataEnabledMethod = telephonyService.javaClass.getDeclaredMethod("getDataEnabled")

            if (null != getMobileDataEnabledMethod) {

                return getMobileDataEnabledMethod.invoke(telephonyService) as Boolean
            }
        } catch (ex: Exception) {
            Log.e("Log", "Error getting mobile data state", ex)
        }

        return false
    }
}
