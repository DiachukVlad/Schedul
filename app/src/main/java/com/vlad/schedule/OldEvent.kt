package com.vlad.schedule

import android.graphics.Color
import java.util.*


class OldEvent(
        var startTime: Int,
        var finishTime: Int,
        var days: IntArray = intArrayOf(0,0,0,0,0,0,0),
        var name: String? = "",
        var ID: Int = 0,
        var isVibro: Boolean = false,
        var note: ArrayList<NoteElement> = ArrayList(),
        var color: Int = Color.CYAN,
        var numberRepeat:Int = 0,
        var date : Calendar = Calendar.getInstance()
)