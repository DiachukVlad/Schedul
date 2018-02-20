package com.vlad.schedule

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.event.view.*

class EventView(var event:Event, context: EventsActivity) {
    var view:LinearLayout

    init {
        val ltInflater = LayoutInflater.from(context)
        view = ltInflater.inflate(R.layout.event, LinearLayout(context)) as LinearLayout

        view.event_name.text = event.name
        view.start_time.text = event.getTime(event.startTime)
        view.finish_time.text = event.getTime(event.finishTime)
        view.color.setBackgroundColor(event.color)

        view.event_edit_image.setOnClickListener({
            var pm = PopupMenu(context, view.event_edit_image)
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

            pm.inflate(R.menu.event_edit_menu)
            pm.setOnMenuItemClickListener { item ->
                when(item!!.itemId){
                    R.id.event_menu_edit->{
                        var intent = Intent(context, EditEvent().javaClass)
                        intent .putExtra("event", Gson().toJson(event))
                        intent .putExtra("edit", true)
                        (context as Activity).startActivityForResult(intent, 2)
                        true
                    }
                    R.id.event_menu_delete->{
                        var dialog = AlertDialog.Builder(context)
                                .setTitle(R.string.delete)
                                .setMessage(R.string.sure_delete_event)
                                .setPositiveButton(R.string.yes, { _: DialogInterface, _: Int ->
                                    context.events.remove(event)
                                    context.setID()
                                    context.initIvents(context.day, true)
                                    context.save()
                                    context.restartService()})
                                .setNegativeButton(R.string.cancel, null)
                        dialog.show()

                        true
                    }
                    else -> false
                }
            }

            pm.show()
        })

        view.main_event_layout.setOnClickListener({
            var intent = Intent(context, NoteActivity::class.java)
            intent.putExtra("event", Gson().toJson(event))
            context.startActivityForResult(intent, context.EVENT_EDIT)
        })

        if(Build.VERSION.SDK_INT >= 21) {
            view.main_event_layout.outlineProvider = CLipProvider()
            view.main_event_layout.clipToOutline = true
        }
    }

}