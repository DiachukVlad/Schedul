package com.vlad.schedule

import android.database.Cursor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.PopupWindow
import kotlinx.android.synthetic.main.activity_schedules.*
import android.graphics.Point
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.add_schedule.view.*
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList


class SchedulesActivity : AppCompatActivity() {

    var data = Data()
    var sdPath = File(Environment.getExternalStorageDirectory(), "Android/data/Schedule")
    var first = false

    lateinit private var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedules)

        title = resources.getString(R.string.app_name)

        read()

        float_add_schedule.setOnClickListener({
            var alertBuilder = AlertDialog.Builder(this)
            var dialogView = layoutInflater.inflate(R.layout.add_schedule, null)
            alertBuilder.setTitle(R.string.add_schedule)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        run {
                            read()
                            var file =createFile(dialogView.name.text.toString())

                            file.createNewFile()
                            var s = Schedule(dialogView.name.text.toString(), file.absolutePath)
                            data.schedules.add(s)
                            var sv = ScheduleView(s, this)
                            //schedules_content.addView(sv.view)
                            initScedules()
                            save()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setView(dialogView)

            alertBuilder.show()
        })

        mAdView = findViewById<View>(R.id.ad_schedules) as AdView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        var uri = intent.data

        if(uri!=null){
            var file = File(uri.path)
            var scn = Scanner(file)

            if(file.absolutePath.substring(file.absolutePath.lastIndexOf(".")).equals(".sch")) {
                var name = file.name.split(".")[0]
                var fileSch = File(createFile(name).absolutePath)
                while (!fileSch.exists()) {
                    fileSch.createNewFile()
                    Thread.sleep(200)
                }

                data.schedules.add(Schedule(name, fileSch.absolutePath))
                initScedules()
                save()

                if(scn.hasNext()){
                    var fw = FileWriter(fileSch)
                    fw.write(scn.nextLine())
                    fw.close()
                }

                Toast.makeText(this, "${data.schedules.last().name} ${resources.getString(R.string.was_added)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun createFile(name:String) : File {
        var file = File(sdPath, "$name.sch")
        var i = 1
        while(file.exists()){
            file = File(sdPath, "$name($i).sch")
            i++
        }
        return file
    }

    override fun onDestroy() {
        mAdView.destroy()
        super.onDestroy()
    }


    override fun onPause() {
        mAdView.pause()
        super.onPause()
    }

    fun removeSchedules(){
        schedules_content.removeAllViews()
    }

    fun initScedules(b : Boolean = false){
        if(data.schedules.size>0 || b)
            removeSchedules()
        for(i in data.schedules){
            schedules_content.addView(ScheduleView(i, this).view)
        }
    }

    fun read(){
        var file = File(sdPath, "event.data")
        var scn = Scanner(file)
        if(scn.hasNext()) {
            var s = scn.nextLine()
            data = Gson().fromJson(s, Data::class.java)
            initScedules()
        }
        else
            first = true
    }

    fun save() {
        var fw = FileWriter(File(sdPath, "event.data"))
        var s = Gson().toJson(data)
        fw.write(s)
        fw.close()
    }

    fun setMain(s : Schedule){
        var ss = ArrayList<Schedule>()
        ss.add(s)
        for(i in data.schedules)
            if(i!=s)
                ss.add(i)
        data.schedules = ss
        save()
    }

    fun getRealPathFromUri(contentUri: Uri): String {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor!!.moveToFirst()
            return cursor!!.getString(column_index)
        } finally {
            if (cursor != null) {
                cursor!!.close()
            }
        }
    }
}
