package com.vlad.schedule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.note_edit_text.view.*
import java.net.URL
import android.provider.MediaStore
import android.graphics.BitmapFactory
import android.R.attr.path
import android.R.attr.data
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.util.Base64
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class NoteActivity : AppCompatActivity() {
    lateinit var event: Event

    lateinit var sdPath : File

    val GALLERY = 0
    val CAMERA = 1

    var photoFile : File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)
        setSupportActionBar(note_toolbar)

        sdPath = File(Environment.getExternalStorageDirectory(), "Android/data/Schedule")

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        if (!intent.hasExtra("event")) {
            finish()
            Toast.makeText(this, "Error 404", Toast.LENGTH_LONG).show()
            return
        }
        event = Gson().fromJson(intent.getStringExtra("event"), Event::class.java)

        title = event.name


        initItems()

        note_add_image.setOnClickListener({
            val pickPhoto = Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickPhoto, GALLERY)
        })

        note_add_camera.setOnClickListener({
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = createImageFile()
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
            startActivityForResult(takePictureIntent, CAMERA)
        })

    }

    override fun onResume() {

        if(photoFile!=null){
            if(photoFile!!.length()>0){
                event.note.add(NoteElement("image", photoFile!!.absolutePath))
                addImage(event.note.size-1)
                event.note.add(NoteElement("text", ""))
                addText(event.note.size-1)
                optimizeNote()
            }
            else
                photoFile!!.delete()

            photoFile = null
        }

        super.onResume()
    }

    fun addText(i:Int){
        var e = layoutInflater.inflate(R.layout.note_edit_text, null)
        e.editText.setText(event.note[i].data)
        e.editText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun afterTextChanged(t: Editable?) {
                event.note[i].data = t.toString()
            }
        })
        e.editText.requestFocus()
        note_content.addView(e)
    }
    fun addImage(i:Int){
        if(File(event.note[i].data).exists()) {
            val imageView = NoteImageView(this, i)
            note_content.addView(imageView.view)
        }
        else{
            Toast.makeText(this, "File \"${event.note[i].data}\" not exist!", Toast.LENGTH_LONG).show()
            event.note.removeAt(i)
            optimizeNote()
        }
    }

    fun initItems() {
        note_content.removeAllViews()
        var i =0
        while(i<event.note.size) {
            when (event.note[i].type) {
                "text" -> addText(i)
                "image" -> addImage(i)

            }
            i++
        }
        if (event.note.size == 0) {
            event.note.add(NoteElement("text", ""))
            addText(event.note.size-1)
        }

    }

    fun optimizeNote(){
        for (i in 1 until event.note.size){
            if(event.note[i-1].type == "text" && event.note[i].type == "text"){
                event.note[i-1].data+="\n${event.note[i].data}"
                event.note.removeAt(i)
                optimizeNote()
                break
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if(intent==null)
            return

        when (requestCode) {
            GALLERY -> if (resultCode == Activity.RESULT_OK) {
                val selectedImage = intent.data
                event.note.add(NoteElement("image", getRealPathFromUri(selectedImage)))
                addImage(event.note.size-1)
                event.note.add(NoteElement("text", ""))
                addText(event.note.size-1)
                optimizeNote()
            }
            CAMERA -> if (resultCode == Activity.RESULT_OK) {

            }

        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "SCHEDULE_" + timeStamp + "_"
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                sdPath      /* directory */
        )

        return image
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.note_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.note_done ->{
                var intent = Intent(this, EventsActivity::class.java)
                intent.putExtra("event", Gson().toJson(event))
                setResult(Activity.RESULT_OK, intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
