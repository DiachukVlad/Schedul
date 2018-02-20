package com.vlad.schedule

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.gson.Gson
import kotlinx.android.synthetic.main.dialog_edit.*
import android.view.WindowManager
import android.widget.*
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import java.text.SimpleDateFormat
import java.util.*
import android.telephony.TelephonyManager
import java.io.File


class EditEvent : AppCompatActivity() {

    private lateinit var event: Event
    var calendar = Calendar.getInstance()
    lateinit var days : Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_edit)

        title = resources.getString(R.string.add_event)

        if(intent.hasExtra("edit"))
            title = resources.getString(R.string.edit_event)



        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        days = arrayOf(resources.getString(R.string.monday),
                resources.getString(R.string.tuesday),
                resources.getString(R.string.wednesday),
                resources.getString(R.string.thursday),
                resources.getString(R.string.friday),
                resources.getString(R.string.saturday),
                resources.getString(R.string.sunday))

        if(intent.hasExtra("event")) {
            event = Gson().fromJson(intent.getStringExtra("event"), Event::class.java)
        }
        else{
            event = Event(calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE),calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE))
            if(intent.hasExtra("day"))
                event.days[intent.getIntExtra("day", 0)]=1
            else
                event.days[(calendar.get(Calendar.DAY_OF_WEEK)+5)%7]=1
        }

        float_done.setOnClickListener {
            val intent = Intent(this, EventsActivity::class.java)
            intent .putExtra("event", Gson().toJson(initEvent()))
            setResult(Activity.RESULT_OK ,intent)
            finish()
        }

        edit_name.setText(event.name.toString())

        edit_start_time.text = event.getTime(event.startTime)
        edit_start_time.setOnClickListener {
            val timePicker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, h, m ->
                run {
                    event.startTime = h * 60 + m
                    edit_start_time.text = event.getTime(event.startTime)
                }
            }, event.startTime/60, event.startTime%60, true)

            timePicker.show()
        }
        edit_finish_time.text = event.getTime(event.finishTime)
        edit_finish_time.setOnClickListener {
            val timePicker = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, h, m ->
                run {
                    event.finishTime = h * 60 + m
                    edit_finish_time.text = event.getTime(event.finishTime)
                }
            }, event.finishTime/60, event.finishTime%60, true)

            timePicker.show()
        }

        setRepeatText(edit_repeat)
        repeat_layout.setOnClickListener({
            val pm = PopupMenu(this, edit_repeat)
            pm.inflate(R.menu.repeat_menu)
            pm.setOnMenuItemClickListener {item->
                when(item.itemId){
                    R.id.repeat_everyday->{
                        event.days = intArrayOf(1,1,1,1,1,1,1)
                        edit_repeat.setText(R.string.everyday)
                        true
                    }
                    R.id.repeat_mon_to_fri->{
                        event.days = intArrayOf(1,1,1,1,1,0,0)
                        edit_repeat.setText(R.string.mon_to_fri)
                        true
                    }
                    R.id.repeat_weekends->{
                        event.days = intArrayOf(0,0,0,0,0,1,1)
                        edit_repeat.setText(R.string.weekends)
                        true
                    }
                    R.id.repeat_custom->{
                        val linLay = LinearLayout(this)
                        linLay.orientation = LinearLayout.VERTICAL
                        val checks = arrayOf(CheckBox(this),CheckBox(this),CheckBox(this),CheckBox(this),CheckBox(this),CheckBox(this),CheckBox(this))

                        for(i in 0 until 7){
                            checks[i].text = days[i]
                            linLay.addView(checks[i])
                            if(event.days[i]==1)
                                checks[i].isChecked = true
                        }

                        val alertBuider = AlertDialog.Builder(this)
                        alertBuider.setView(linLay)
                                .setTitle(R.string.select_days)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, {_, _->
                                    run{
                                        for(i in 0 until 7){
                                            if(checks[i].isChecked)
                                                event.days[i]=1
                                            else
                                                event.days[i]=0
                                        }
                                        setRepeatText(edit_repeat)
                                    }
                                })
                        alertBuider.show()
                        true
                    }
                    else->false
                }
            }
            pm.show()
        })

        question_number_repeat.setOnClickListener({
            var dialog = android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.number_repeat)
                    .setMessage(R.string.question_number_repeat)
                    .setPositiveButton(R.string.ok, null)
            dialog.show()
        })

        question_date.setOnClickListener({
            var dialog = android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.date)
                    .setMessage(R.string.question_date)
                    .setPositiveButton(R.string.ok, null)
            dialog.show()
        })

        edit_vibro.checkedTogglePosition = event.isVibro
        edit_wifi.checkedTogglePosition = event.isWifi
        edit_bluetooth.checkedTogglePosition = event.isBluetooth
        //edit_mobile_data.checkedTogglePosition = event.isMobData

        color_edit_view.setBackgroundColor(event.color)
        color_edit.setOnClickListener({
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("Choose color")
                    .initialColor(event.color)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener {  }
                    .setPositiveButton("ok") { _, selectedColor, _ ->
                        run {
                            color_edit_view.setBackgroundColor(selectedColor)
                            event.color = selectedColor
                        }
                    }
                    .setNegativeButton("cancel") { _, _ -> }
                    .build()
                    .show()
        })

        edit_number_repeat.setText(event.numberRepeat.toString())

        if(intent.hasExtra("edit")) {
            other.removeView(edit_date_layout)
        }else {
            edit_date.text = getDateString(event.date!!)
            edit_date.setOnClickListener({
                val year = event.date!!.get(Calendar.YEAR)
                val month = event.date!!.get(Calendar.MONTH)
                val day = event.date!!.get(Calendar.DAY_OF_MONTH)

                var datePicker = DatePickerDialog(this,
                        { picker, y, m, d ->
                            run {
                                event.date!!.set(y, m, d)
                                edit_date.text = getDateString(event.date!!)
                            }
                        }, year, month, day)

                datePicker.show()
            })
        }
    }


    fun getDateString(date: Calendar): String{
        var dateFormat = SimpleDateFormat("dd MMM yyyy")
        return  dateFormat.format(date.time)
    }

    fun setRepeatText(t:TextView){
        var sum = 0
        for(i in event.days){
            if(i==1)
                sum++
        }

        if(sum==7)
            t.setText(R.string.everyday)
        else if(sum==1){
            for(i in 0..6)
                if(event.days[i]==1)
                    t.text = days[i]
        }
        else if(sum==2 && event.days[5]==1 && event.days[6]==1){
            t.setText(R.string.weekends)
        }
        else if(sum==5 && event.days[5]==0 && event.days[6]==0){
            t.setText(R.string.mon_to_fri)
        }
        else{
            t.setText(R.string.custom)
        }
    }

    fun initEvent(): Event? {
        event.name = edit_name.text.toString()

        event.isVibro = edit_vibro.checkedTogglePosition
        event.isWifi = edit_wifi.checkedTogglePosition
        event.isBluetooth = edit_bluetooth.checkedTogglePosition

        println(event.isVibro)
        println(event.isWifi)
        println(event.isBluetooth)
        //event.isMobData = edit_mobile_data.checkedTogglePosition

        event.numberRepeat = edit_number_repeat.text.toString().toInt()

        return event
    }

}
