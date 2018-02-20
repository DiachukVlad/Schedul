package com.vlad.schedule

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupMenu
import kotlinx.android.synthetic.main.add_schedule.view.*
import kotlinx.android.synthetic.main.schedule.view.*
import java.io.File
import java.io.FileWriter
import java.util.*


class ScheduleView(schedule:Schedule, context: SchedulesActivity) {
    var view: LinearLayout

    init {
        val ltInflater = LayoutInflater.from(context)
        view = ltInflater.inflate(R.layout.schedule, LinearLayout(context)) as LinearLayout

        view.schedule_name.text = schedule.name

        view.schedule_edit.setOnClickListener {
            var pm = PopupMenu(context, view.schedule_edit)
            /*try {
                val fields = pm::class.java.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(pm)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {e.printStackTrace()}*/

            pm.inflate(R.menu.schedule_edit_menu)
            pm.setOnMenuItemClickListener { item ->
                when(item!!.itemId){
                    R.id.schedule_menu_edit->{
                        var alertBuilder = AlertDialog.Builder(context)
                        var dialogView = context.layoutInflater.inflate(R.layout.add_schedule, null)
                        dialogView.name.setText(schedule.name)
                        alertBuilder.setTitle(R.string.add_schedule)
                                .setPositiveButton(R.string.ok) { dialog, id ->
                                    run {
                                        var file = File(schedule.path)
                                        var scn = Scanner(file)
                                        var s = ""
                                        if(scn.hasNext())
                                            s = scn.nextLine()
                                        file.delete()
                                        file = context.createFile(dialogView.name.text.toString())
                                        file.createNewFile()
                                        var fw = FileWriter(file)
                                        fw.write(s)
                                        fw.close()

                                        schedule.name = dialogView.name.text.toString()
                                        schedule.path = file.absolutePath
                                        context.save()
                                        context.initScedules()
                                    }
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .setView(dialogView)

                        alertBuilder.show()
                        true
                    }
                    R.id.schedule_menu_delete->{
                        var dialog = android.app.AlertDialog.Builder(context)
                                .setTitle(R.string.delete)
                                .setMessage(R.string.sure_delete_schedule)
                                .setPositiveButton(R.string.yes, { _: DialogInterface, _: Int ->
                                    File(schedule.path).delete()
                                    context.data.schedules.remove(schedule)
                                    context.save()
                                    context.initScedules(true)
                                })
                                .setNegativeButton(R.string.cancel, null)
                        dialog.show()
                        true
                    }

                    R.id.schedule_menu_share->{
                        var uri = Uri.fromFile(File(schedule.path))
                        var intent = Intent(Intent.ACTION_SEND)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.type = "file/*"
                        context.startActivity(intent)
                        true
                    }
                    else -> false
                }
            }

            pm.show()

        }
        view.setOnClickListener({
            context.setMain(schedule)
            var intent = Intent(context, EventsActivity::class.java)
            if(context.first)
                intent.putExtra("first", true)
            context.startActivity(intent)
        })

        if (Build.VERSION.SDK_INT >= 21) {
            view.main_schedule_layout.outlineProvider = CLipProvider()
            view.main_schedule_layout.clipToOutline = true
        }
    }
}

