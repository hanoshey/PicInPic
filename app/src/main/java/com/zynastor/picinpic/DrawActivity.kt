package com.zynastor.picinpic

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.zynastor.picinpic.BuildConfig.IS_FREE
import com.zynastor.picinpic.db.MainList
import com.zynastor.picinpic.db.RoomAppDb
import com.zynastor.picinpic.db.UserEntry
import com.zynastor.picinpic.util.DrawFunctions.projectXY
import kotlinx.android.synthetic.main.activity_draw.*
import kotlinx.android.synthetic.main.activity_show.*
import kotlinx.android.synthetic.main.item_list.*
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private const val TAG = "DrawActivity"

class DrawActivity : AppCompatActivity() {
    var source: Uri? = null
    var realUri: String = ""
    private var allUserEntry: List<UserEntry> = ArrayList()
    private var thisUserEntry: List<UserEntry> = ArrayList()
    private lateinit var canvasMaster: Canvas
    private lateinit var bitmapMaster: Bitmap
    private lateinit var bitmapDrawingPane: Bitmap
    private lateinit var canvasDrawingPane: Canvas
    private lateinit var startPt: ProjectPt
    private var fromId: Long = 0
    private val IMAGE1 = 1
    private val IMAGE2 = 2
    private var layerLevel: Int = 0
    private var mainId: Long = 0
    private var canIDraw = false
    private var firstUri: String = ""
    private var secondUri: String = ""
    private var resultUri: String = ""
    private lateinit var rectanglefunc: Rectanglefunc
    private lateinit var itemName: String
    private var finalHeight: Int = 0
    private var finalWidth: Int = 0
    private lateinit var resultView: ImageView
    private val mainAndListDao by lazy { RoomAppDb.getAppDatabase(this).getMainAndUserDao() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw)
        resultView = findViewById(R.id.result)
        val vto = resultView.viewTreeObserver
        vto.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                resultView.viewTreeObserver.removeOnPreDrawListener(this)
                finalHeight = resultView.measuredHeight
                finalWidth = resultView.measuredWidth
                Log.d(TAG, "onPreDraw: Height: $finalHeight Width: $finalWidth")
                return true
            }
        })
        val intent = this.intent
        itemName = intent.getStringExtra("itemname").toString()
        Log.d(TAG, "onCreate: $itemName")
        layerLevel = intent.getIntExtra("layerlevel", 0)
        layerLevel += 1
        fromId = intent.getLongExtra("fromid", 0)
        mainId = intent.getLongExtra("mainid", 0)
        Log.d(TAG, "onCreate: fromId: $fromId")
        Log.d(TAG, "onCreate: layerlevel : $layerLevel")
        val intentt = CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .setMultiTouchEnabled(true)
            .getIntent(this)
        when (intent.extras!!.getString("wherefrom")) {
            "MainActivity" -> {
                vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        resultView.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                        startActivityForResult(intentt, IMAGE1)
                    }
                })
            }
            "DrawActivity" -> {
                resultUri = intent.getStringExtra("currentPhotoUri")!!
                Log.d(TAG, "onCreate: resultUri")
                rectanglefunc = intent.extras!!.getParcelable("rect")!!
                vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        resultView.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                        startActivityForResult(intentt, IMAGE2)
                    }
                })
            }
            "Buttons" -> {
                resultUri = intent.getStringExtra("photodata")!!
                Log.d(TAG, "onCreate: $resultUri")
                val tempBitmap = BitmapFactory.decodeFile(resultUri)
                val config: Bitmap.Config = if (tempBitmap.config != null) {
                    tempBitmap.config
                } else {
                    Bitmap.Config.ARGB_8888
                }
                val vtol = draw_layout.viewTreeObserver
                vtol.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        draw_layout.viewTreeObserver
                            .removeOnPreDrawListener(this)
                        bitmapMaster = Bitmap.createBitmap(
                            tempBitmap.width, tempBitmap.height, config
                        )
                        canvasMaster = Canvas(bitmapMaster)
                        canvasMaster.drawBitmap(tempBitmap, 0f, 0f, null)
                        result.setImageBitmap(bitmapMaster)
                        bitmapDrawingPane = Bitmap.createBitmap(
                            tempBitmap.width, tempBitmap.height, config
                        )
                        canvasDrawingPane = Canvas(bitmapDrawingPane)
                        drawingpane.setImageBitmap(bitmapDrawingPane)
                        draw_layout.removeAllViews()
                        val layoutFinalHeight = draw_layout.measuredHeight
                        val layoutFinalWidth = draw_layout.measuredWidth
                        Log.d(TAG, "onPreDraw: Height: $layoutFinalHeight Width: $layoutFinalWidth")
                        drawButtons(thisUserEntry,layoutFinalWidth,layoutFinalHeight)
                        return true
                    }
                })
            }
        }
        if ((IS_FREE || !isGenuine) && layerLevel >= 3) {
            drawingpane.setOnTouchListener { _: View, event: MotionEvent ->
                val action = event.action
                if (action == MotionEvent.ACTION_DOWN) {
                    Toast.makeText(this,
                        "Demo version layer level limit reached.",
                        Toast.LENGTH_LONG).show()
                }
                true
            }
        } else {
            drawingpane.setOnTouchListener(drawing)
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

    private fun performClick(): Boolean {
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: called")
        runBlocking {
            allUserEntry = mainAndListDao.getUserByMainId(mainId)
        }
        thisUserEntry = allUserEntry.filterByUser(fromId)
        Log.d(TAG, "onCreate: thisUserEntry: $thisUserEntry")
        val vtol = draw_layout.viewTreeObserver
        if (this::bitmapMaster.isInitialized) {
            vtol.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    draw_layout.removeAllViews()
                    draw_layout.viewTreeObserver.removeOnPreDrawListener(this)
                    val layoutFinalHeight = draw_layout.measuredHeight
                    val layoutFinalWidth = draw_layout.measuredWidth
                    Log.d(TAG, "onPreDraw: Height: $layoutFinalHeight Width: $layoutFinalWidth")
                    drawButtons(thisUserEntry,layoutFinalWidth,layoutFinalHeight)
                    return true
                }
            })
        }
    }
    private fun drawButtons(thisUserEntry: List<UserEntry>,layoutFinalWidth:Int,layoutFinalHeight:Int){
        for (i in thisUserEntry.indices) {
            val buttons = Button(this@DrawActivity)
            val params =
                RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT).run {
                    val returnedBtn =
                        convertToRealButton(thisUserEntry[i].rect.startX,
                            thisUserEntry[i].rect.startY,
                            thisUserEntry[i].rect.endX,
                            thisUserEntry[i].rect.endY)
                    leftMargin =
                        (returnedBtn.x + ((layoutFinalWidth - bitmapMaster.width) / 2))
                    topMargin =
                        (returnedBtn.y + ((layoutFinalHeight - bitmapMaster.height) / 2))
                    Log.d(TAG,
                        "onResume: leftMargin:$leftMargin, topMargin:$topMargin")
                    Log.d(TAG, "onResume: $layoutFinalWidth,${layoutFinalWidth}")
                    Log.d(TAG,
                        "onResume: ${bitmapMaster.width},${bitmapMaster.height}")
                    width =
                        abs(thisUserEntry[i].rect.endX - thisUserEntry[i].rect.startX)
                    height =
                        abs(thisUserEntry[i].rect.endY - thisUserEntry[i].rect.startY)
                    Log.i(TAG, "onCreate: $leftMargin, $topMargin, $width, $height")
                    this
                }
            buttons.run {
                text =
                    (i + 1).toString(); setBackgroundResource(R.drawable.button_color)
                layoutParams = params
                setOnClickListener { view ->
                    val intentt =
                        Intent(this@DrawActivity, DrawActivity::class.java).run {
                            flags = FLAG_ACTIVITY_MULTIPLE_TASK
                            putExtra("fromid", thisUserEntry[i].userId)
                            putExtra("mainid", thisUserEntry[i].mainListId)
                            putExtra("wherefrom", "Buttons")
                            putExtra("photodata", thisUserEntry[i].destPhotoUri)
                            putExtra("layerlevel", layerLevel)
                        }
                    startActivity(intentt)
                }
                this
            }
            draw_layout.addView(buttons)
        }
    }

    private val drawing = View.OnTouchListener { v: View, event ->
        val action = event.action
        val x = event.x.toInt()
        val y = event.y.toInt()
        if (action == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onCreate: \"ACTION_DOWN- $x : $y\"")
            if (projectXY(v, bitmapMaster, x, y) == null) {
                canIDraw = false
            } else {
                startPt = projectXY(v, bitmapMaster, x, y)!!
                canIDraw = true
            }
        }
        if (canIDraw) {
            when (action) {
                MotionEvent.ACTION_MOVE -> {
                    drawOnRectProjectedBitMap(v, bitmapMaster, x, y, true)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    performClick()
                    Log.d(TAG, "onCreate: \"ACTION_UP- $x : $y\"")
                    if (abs(startPt.x - x) >= 60 && abs(startPt.y - y) >= 60) {
                        val tosendrect = drawOnRectProjectedBitMap(v, bitmapMaster, x, y, false)
                        v.invalidate()
                        finalizeDrawing()
                        val toSend = tosendrect
                        val intent = Intent(this@DrawActivity, DrawActivity::class.java).run {
                            flags = FLAG_ACTIVITY_MULTIPLE_TASK
                            putExtra("currentPhotoUri", firstUri)
                            Log.d(TAG, "currentphotouri: $firstUri")
                            putExtra("wherefrom", "DrawActivity")
                            putExtra("rect", toSend)
                            putExtra("layerlevel", layerLevel)
                            putExtra("fromid", fromId)
                            putExtra("mainid", mainId)
                            putExtra("wherefrom", "DrawActivity")
                            this
                        }
                        startActivity(intent)
                    } else {
                        drawOnRectProjectedBitMap(v, bitmapMaster, x, y, false)
                        Toast.makeText(this, "The area is too small.", Toast.LENGTH_LONG).show()
                        v.invalidate()
                        finalizeDrawing()
                    }
                }
                else -> {
                }
            }
        }
        true
        /** Return 'true' to indicate that the event have been consumed.
         * If auto-generated 'false', your code can detect ACTION_DOWN only,
         * cannot detect ACTION_MOVE and ACTION_UP.*/
    }

    private fun drawOnRectProjectedBitMap(
        iv: View, bm: Bitmap, x: Int, y: Int, notSmall: Boolean,
    )
            : Rectanglefunc {
        val projectedX =
            (x.toDouble() - ((iv.width.toDouble() - bm.width.toDouble()) / 2)).toInt()
        val projectedY =
            (y.toDouble() - ((iv.height.toDouble() - bm.height.toDouble()) / 2)).toInt()
        if (x >= (iv.width - bm.width) / 2 && y >= (iv.height - bm.height) / 2 && x <= ((iv.width - bm.width) / 2) + bm.width && y <= (iv.height - bm.height) / 2 + bm.height) {
            //clear canvasDrawingPane
            canvasDrawingPane.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val paint = Paint().run {
                style = Paint.Style.STROKE
                color = Color.WHITE
                strokeWidth = 5f
                setShadowLayer(10f, 0f, 0f, Color.BLACK)
                this
            }
            if (notSmall) {
                canvasDrawingPane.drawRect(
                    startPt.x.toFloat(), startPt.y.toFloat(),
                    projectedX.toFloat(), projectedY.toFloat(), paint)
            }
        }
        return Rectanglefunc(startPt.x, startPt.y, projectedX, projectedY)
    }

    private fun finalizeDrawing() {
        canvasMaster.drawBitmap(bitmapDrawingPane, 0f, 0f, null)
    }

    private fun resizeBitmapImageFun(tempBitmap: Bitmap): Bitmap {
        var newWidth = tempBitmap.width
        var newHeight = tempBitmap.height
        val rate: Float
        val rate2: Float
        if (tempBitmap.width > tempBitmap.height) {
            Log.d(TAG, "resizeBitmapImageFun: CASE 5")
            rate = finalWidth.toFloat() / tempBitmap.width.toFloat()
            newHeight = (tempBitmap.height * rate).toInt()
            newWidth = finalWidth
            if (finalHeight < newHeight) {
                Log.d(TAG, "resizeBitmapImageFun: CASE 3")
                rate2 = finalWidth.toFloat() / newHeight.toFloat()
                newHeight = finalHeight
                newWidth = (newWidth * rate2).toInt()
            }
        } else if (tempBitmap.width <= tempBitmap.height) {
            Log.d(TAG, "resizeBitmapImageFun: CASE 2")
            rate = finalHeight.toFloat() / tempBitmap.height.toFloat()
            newWidth = (tempBitmap.width * rate).toInt()
            newHeight = finalHeight
            if (finalWidth < newWidth) {
                Log.d(TAG, "resizeBitmapImageFun: CASE 4")
                rate2 = finalWidth.toFloat() / newWidth.toFloat()
                newWidth = finalWidth
                newHeight = (newHeight * rate2).toInt()
            }
        }
        Log.d(TAG,
            "resizeBitmapImageFun222: ${tempBitmap.width},${tempBitmap.height} to $newWidth,$newHeight")
        return Bitmap.createScaledBitmap(tempBitmap, newWidth, newHeight, true)
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap, name: String): String {
        val cw = ContextWrapper(applicationContext)//path to /data/data/yourapp/appdata/imagedir
        val directory = cw.getDir("imageDir", Context.MODE_PRIVATE)
        val mypath = File(directory, name)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(mypath)
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return mypath.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: $requestCode")
        var tempBitmap: Bitmap?
        val vto = resultView.viewTreeObserver
        vto.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                resultView.viewTreeObserver.removeOnPreDrawListener(this)
                finalHeight = resultView.measuredHeight
                finalWidth = resultView.measuredWidth
                Log.d(TAG, "onPreDraw111: Height: $finalHeight Width: $finalWidth")
                return true
            }
        })
        if (resultCode == RESULT_OK) {
            try {
                val fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val resultdata = CropImage.getActivityResult(data)
                source = resultdata.uri
                tempBitmap = BitmapFactory.decodeStream(
                    contentResolver.openInputStream(source!!)
                )
                tempBitmap = resizeBitmapImageFun(tempBitmap!!)
                val config: Bitmap.Config = if (tempBitmap.config != null) {
                    tempBitmap.config
                } else {
                    Bitmap.Config.ARGB_8888
                }
                bitmapMaster = Bitmap.createBitmap(
                    tempBitmap.width, tempBitmap.height, config
                )
                canvasMaster = Canvas(bitmapMaster)
                canvasMaster.drawBitmap(tempBitmap, 0f, 0f, null)
                result.setImageBitmap(bitmapMaster)
                bitmapDrawingPane = Bitmap.createBitmap(
                    tempBitmap.width, tempBitmap.height, config
                )
                canvasDrawingPane = Canvas(bitmapDrawingPane)
                drawingpane.setImageBitmap(bitmapDrawingPane)
                realUri = saveToInternalStorage(tempBitmap, fileName)
                Log.d(TAG, "realUri: $realUri")
                if (requestCode == IMAGE1) {
                    firstUri = realUri
                    Log.d(TAG, "onActivityResult: $firstUri")
                    val mainList = MainList(0, itemName, firstUri)
                    Log.d(TAG, "onActivityResult: $mainList")
                    lifecycleScope.launch(Dispatchers.IO) {
                        mainId = mainAndListDao.insertMainList(mainList)!!
                    }
                } else if (requestCode == IMAGE2) {
                    secondUri = realUri
                    Log.d(TAG, "onActivityResult: $firstUri, $secondUri")
                    val toSend =
                        UserEntry(0, layerLevel, rectanglefunc, fromId, secondUri, mainId)
                    Log.d(TAG, "tosend:$toSend ")
                    lifecycleScope.launch(Dispatchers.IO) {
                        fromId = mainAndListDao.insertUser(toSend)
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else if (resultCode == RESULT_CANCELED) {
            val cancel = Intent()
            cancel.putExtra("cancelled", true)
            setResult(RESULT_OK, cancel)
            finish()
        }
    }

    private fun List<UserEntry>.filterByUser(fromId: Long) = this.filter { it.fromId == fromId }
}
//ToDo: Bugs happens randomically. What is the problem? Related by MainId and FromId.