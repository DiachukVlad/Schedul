package com.vlad.schedule

import android.app.ActionBar
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.note_image.view.*
import java.io.File
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.R.attr.path
import android.app.Activity
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.widget.PopupMenu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_note.*
import android.os.Environment.DIRECTORY_PICTURES
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.R.attr.data
import android.content.DialogInterface
import android.support.v4.app.NotificationCompat.getExtras
import android.os.Bundle








class NoteImageView(context : NoteActivity, i: Int){
    var view : View

    init {
        var ll = LinearLayout(context)
        ll.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        view = context.layoutInflater.inflate(R.layout.note_image, ll)
        val size = 2 //minimize  as much as you want
        val bitmapOriginal = BitmapFactory.decodeFile(context.event.note[i].data)
        val bitmapsimplesize = Bitmap.createScaledBitmap(bitmapOriginal, bitmapOriginal.width / size, bitmapOriginal.height / size, true)
        bitmapOriginal.recycle()
        view.image.setImageBitmap(bitmapsimplesize)

        view.setOnClickListener({
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(Uri.fromFile(File(context.event.note[i].data)), "image/*")
            context.startActivity(intent)
        })

        view.setOnLongClickListener {
            var pm = PopupMenu(context, view.image)

            pm.inflate(R.menu.note_image_menu)
            pm.setOnMenuItemClickListener { item ->
                when(item!!.itemId){
                    R.id.delete_image->{
                        var dialog = android.app.AlertDialog.Builder(context)
                                .setTitle(R.string.delete)
                                .setMessage(R.string.sure_delete_photo)
                                .setPositiveButton(R.string.yes, { _: DialogInterface, _: Int ->
                                    var file = File(context.event.note[i].data)
                                    if(file.name.split('_')[0].equals("SCHEDULE"))
                                        file.delete()

                                    context.event.note.removeAt(i)
                                    context.optimizeNote()
                                    context.initItems()
                                })
                                .setNegativeButton(R.string.cancel, null)
                        dialog.show()
                        true
                    }
                    else -> false
                }
            }
            pm.show()
            true


        }
    }


}