package com.vlad.schedule

import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

class CLipProvider : ViewOutlineProvider() {
    override fun getOutline(view: View?, outline: Outline?) {
        if(Build.VERSION.SDK_INT >= 21) {
            var r = Rect(0, 0, view!!.width, view.height)
            outline!!.setRoundRect(r, 15f)
        }
    }

}