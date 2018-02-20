package com.vlad.schedule

import android.graphics.Color
import com.google.gson.annotations.Expose
import java.util.*
import kotlin.collections.ArrayList

class Event(
        var startTime: Int,
        var finishTime: Int,
        var days: IntArray = intArrayOf(0,0,0,0,0,0,0),
        var name: String? = "",
        var ID: Int = 0,
        var isVibro: Int = 1,
        var isWifi: Int = 1,
        var isBluetooth: Int = 1,
        var isMobData: Int = 1,
        var note: ArrayList<NoteElement> = ArrayList(),
        var color: Int = Color.CYAN,
        var numberRepeat:Int = 0,
        var date : Calendar? = Calendar.getInstance()
) {
    @Expose(serialize = false)
    private val START_EVENT = 1

    @Expose(serialize = false)
    private val FINISH_EVENT = 2

    @Expose(serialize = false)
    private var running = false

    fun getTime(t:Int):String{
        var s = ""
        if(t/60<10) s+="0"
        s+="${t/60}:"
        if(t%60<10) s+="0"
        s+="${t%60}"

        return s
    }

    fun tick(currentTime:Int) : Int{
        if(!running && currentTime>=startTime && currentTime<finishTime) {
            running = true
            return START_EVENT
        }
        else if(running && (currentTime<startTime || currentTime>=finishTime)) {
            running = false
            return FINISH_EVENT
        }
        else
            return 0
    }

}
