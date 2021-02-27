package com.zynastor.picinpic

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.zynastor.picinpic.db.MainList
import com.zynastor.picinpic.db.RoomAppDb
import com.zynastor.picinpic.db.UserEntry
import kotlinx.android.synthetic.main.activity_draw.*
import kotlinx.android.synthetic.main.activity_show.*
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

private const val TAG = "ShowActivity"

class ShowActivity : AppCompatActivity() {
    private val mainAndListdao by lazy { RoomAppDb.getAppDatabase(this).getMainAndUserDao() }
    private var allUserEntry: List<UserEntry> = ArrayList()
    private var thisUserEntry: List<UserEntry> = ArrayList()
    private lateinit var bitmapDrawingPane: Bitmap
    private var fromId = 0L
    private var mainId = 0L
    private var layoutWidth = 0
    private var layoutHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)
        val intent = this.intent
        val tempBitmap: Bitmap?
        Log.d(TAG, "alluserentry: $allUserEntry")
        if (intent.extras!!.getString("wherefrom") == "MainActivity") {
            val mainList = intent.getParcelableExtra<MainList>("mainlist")!!
            fromId = intent.extras!!.getLong("mainid")
            Log.d(TAG, "onCreate: $mainList")
            runBlocking {
                allUserEntry = mainAndListdao.getUserByMainId(mainList.mainId)
            }
            mainId = mainList.mainId
            Log.i(TAG, "onCreate: $allUserEntry")
            thisUserEntry = allUserEntry.filterByUser(fromId)
            Log.i(TAG, "onCreatethisuserentry: $thisUserEntry")
            tempBitmap = BitmapFactory.decodeFile(mainList.photoData)
        } else {
            mainId = intent.extras!!.getLong("mainid")
            fromId = intent.extras!!.getLong("fromid")
            runBlocking {
                allUserEntry = mainAndListdao.getUserByMainId(mainId)
            }
            thisUserEntry = allUserEntry.filterByUser(fromId)
            Log.i(TAG, "onCreate: $thisUserEntry")
            tempBitmap = BitmapFactory.decodeFile(intent.extras!!.getString("photodata"))
        }
        val config: Bitmap.Config = if (tempBitmap.config != null) {
            tempBitmap.config
        } else {
            Bitmap.Config.ARGB_8888
        }
        result_show.setImageBitmap(tempBitmap)
        val params=RelativeLayout.LayoutParams(tempBitmap.width,tempBitmap.height)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        show_inside_layout.layoutParams=params
        bitmapDrawingPane = Bitmap.createBitmap(tempBitmap.width, tempBitmap.height, config)
        show_inside_layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        layoutWidth = show_inside_layout.measuredWidth
        layoutHeight = show_inside_layout.measuredHeight
        Log.i(TAG, "onCreate:layout is   $layoutWidth, $layoutHeight")
        for (i in thisUserEntry.indices) {
            val buttons = Button(this)
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT).run {
                val returnedBtn = convertToRealButton(thisUserEntry[i].rect.startX,
                    thisUserEntry[i].rect.startY, thisUserEntry[i].rect.endX,
                    thisUserEntry[i].rect.endY)
                leftMargin = returnedBtn.x
                topMargin = returnedBtn.y
                width = abs(thisUserEntry[i].rect.endX - thisUserEntry[i].rect.startX)
                height = abs(thisUserEntry[i].rect.endY - thisUserEntry[i].rect.startY)
                Log.i(TAG, "onCreate: $leftMargin, $topMargin, $width, $height")
                this
            }
            buttons.run {
                text = (i + 1).toString(); setBackgroundResource(R.drawable.button_color)
                layoutParams = params
                setOnClickListener { view ->
                    val intent = Intent(this@ShowActivity, ShowActivity::class.java).run {
                        flags = Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        putExtra("fromid", thisUserEntry[i].userId)
                        putExtra("mainid", thisUserEntry[i].mainListId)
                        putExtra("wherefrom", "ShowActivity")
                        putExtra("photodata", thisUserEntry[i].destPhotoUri)
                    }
                    startActivity(intent)
                }
                this
            }
            show_inside_layout.addView(buttons)
        }
        result_show.setOnTouchListener { v: View, event: MotionEvent ->
            result_show.onTouch(v,event)
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) {
                when(result_show.mode){
                    ZoomClass.ZOOM,ZoomClass.DRAG -> show_inside_layout.visibility=View.INVISIBLE
                    ZoomClass.NONE -> {
                        show_inside_layout.visibility=View.VISIBLE
                    }
                }
            }else if(action==MotionEvent.ACTION_POINTER_UP){
                when(result_show.mode){
                    ZoomClass.ZOOM,ZoomClass.DRAG -> show_inside_layout.visibility=View.INVISIBLE
                    ZoomClass.NONE -> {
                        show_inside_layout.visibility=View.VISIBLE
                    }
                }
            }
            false
        }

    }


    data class returnedButton(val x: Int = 0, val y: Int = 0)

    private fun convertToRealButton(
        inputX: Int, inputY: Int, endX: Int, endY: Int,
    ): returnedButton {
        return when {
            (inputX <= endX && inputY <= endY) -> return returnedButton(inputX, inputY)
            (inputX <= endX && inputY > endY) -> return returnedButton(inputX, endY)
            (inputX > endX && inputY <= endY) -> return returnedButton(endX, inputY)
            (inputX > endX && inputY > endY) -> return returnedButton(endX, endY)
            else -> returnedButton()
        }
    }

    private fun List<UserEntry>.filterByUser(fromId: Long) = this.filter { it.fromId == fromId }
}
