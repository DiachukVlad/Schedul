package com.vlad.schedule

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_schedules.*
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.add_schedule.view.*
import kotlinx.android.synthetic.main.languages.view.*
import kotlinx.android.synthetic.main.note_image.view.*
import java.io.File
import java.io.FileWriter
import java.util.*

class SettingsActivity : AppCompatActivity() {

    val sdPath = File(Environment.getExternalStorageDirectory(), "Android/data/Schedule")
    var settings = Settings()
    var file = File(sdPath, "settings.data")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = resources.getString(R.string.settings)

        var file = File(sdPath, "settings.data")
        var scn = Scanner(file)
        settings = Gson().fromJson(scn.nextLine(), Settings::class.java)

        settings_notification.isChecked = settings.notification
        settings_change_system.isChecked = settings.systemSetings
        settings_vibro_notification.isChecked = settings.vibroNotification
        when(settings.locale.toString()){
            "en_EN"->language_image.setImageResource(R.drawable.ic_flag_of_the_united_kingdom)
            "uk_UA"->language_image.setImageResource(R.drawable.ic_flag_of_ukraine)
            "ru_RU"->language_image.setImageResource(R.drawable.ic_flag_of_russia)
            else -> language_image.setImageResource(R.drawable.ic_flag_of_the_united_kingdom)
        }

        language_image.setOnClickListener({
            var alertBuilder = AlertDialog.Builder(this)
            var languages = layoutInflater.inflate(R.layout.languages, null)
            alertBuilder.setTitle(R.string.add_schedule)
                    .setPositiveButton(R.string.ok, null)
                    .setView(languages)

            var alertDialog = alertBuilder.create()

            languages.ukrainian_language.setOnClickListener({
                alertDialog.dismiss()
                setLocale(Locale("uk_UA"))
            })
            languages.russian_language.setOnClickListener({
                alertDialog.dismiss()
                setLocale(Locale("ru_RU"))
            })
            languages.english_language.setOnClickListener({
                alertDialog.dismiss()
                setLocale(Locale("en_EN"))
            })

            alertDialog.show()
        })

        float_ok_settings.setOnClickListener({
            settings.notification = settings_notification.isChecked
            settings.systemSetings = settings_change_system.isChecked
            settings.vibroNotification = settings_vibro_notification.isChecked

            save()

            var intent = Intent(this, EventsActivity::class.java)
            intent.putExtra("restartService", true)
            startActivity(intent)
        })
    }

    fun save(){
        var fw = FileWriter(file)
        var s = Gson().toJson(settings)
        fw.write(s)
        fw.close()
    }

    fun setLocale(l : Locale) {
        settings.locale = l
        save()

        var intent = Intent(this, EventsActivity::class.java)
        intent.putExtra("restartService", true)
        startActivity(intent)
    }
}
