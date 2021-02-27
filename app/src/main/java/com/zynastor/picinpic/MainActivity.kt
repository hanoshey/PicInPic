package com.zynastor.picinpic

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Secure
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.google.android.gms.ads.*
import com.google.android.vending.licensing.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.zynastor.picinpic.db.MainList
import com.zynastor.picinpic.db.RoomAppDb
import com.zynastor.picinpic.ui.ListAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

private const val BASE64_PUBLIC_KEY = ""
private val SALT = byteArrayOf(-46, -54, -62, -6, -5, -76, -25, -54, -51, 1,
    -32, -95, -117, -18, -124, -34, 36, 12, -12, -31)
private const val TAG = "MainActivity"
internal var isGenuine = false
private const val adIdRelease=""
private const val adIdTest=""

class MainActivity : AppCompatActivity(), ListAdapter.ClickingListener {
    private lateinit var licenseCheckerCallback: LicenseCheckerCallback
    private lateinit var checker: LicenseChecker
    private lateinit var mInterstitialAd: InterstitialAd
    lateinit var listAdapter: ListAdapter
    private var aboutDialog: AlertDialog? = null
    private var recyclerMainList: RecyclerView? = null
    private var allMainList: List<MainList> = ArrayList()
    private val mainAndListdao by lazy { RoomAppDb.getAppDatabase(this).getMainAndUserDao() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        MobileAds.initialize(this@MainActivity)
//        licenseCheckerCallback = MyLicenseCheckerCallback()
//        checker = LicenseChecker(
//            this,
//            ServerManagedPolicy(this,
//                AESObfuscator(SALT,
//                    packageName,
//                    Secure.getString(getContentResolver(), Secure.ANDROID_ID))),
//            BASE64_PUBLIC_KEY // Your public licensing key.
//        )
//        doCheck()
//        mInterstitialAd = InterstitialAd(this)
//        mInterstitialAd.adUnitId = adIdTest
//        mInterstitialAd.loadAd(AdRequest.Builder().build())
        Log.d(TAG, "onCreate: called")
        recyclerMainList = findViewById(R.id.recycler_view_mainlist)
        recyclerMainList?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            listAdapter = ListAdapter(this@MainActivity)
            listAdapter.setListData(allMainList)
            adapter = listAdapter
            addItemDecoration(DividerItemDecoration(applicationContext, VERTICAL))
        }
        getAllMainList()
        listAdapter.notifyDataSetChanged()
        deleteUnusedPhoto()
        tedPermission()
        fab.setOnClickListener {
//            if (mInterstitialAd.isLoaded && (BuildConfig.IS_FREE || !isGenuine)) {
//                Log.d(TAG, "onCreate: $mInterstitialAd.isLoaded")
//                mInterstitialAd.show()
//            } else {
//                Log.d(TAG, "onCreate: $isGenuine")
//                Log.d(TAG, "onCreate: hasLoaded : ${mInterstitialAd.isLoaded}")
//                Log.d(TAG, "The interstitail wasnt loaded yet.")
//            }
            val intent = Intent(this, DrawActivity::class.java)
            val dialogView =
                LayoutInflater.from(applicationContext).inflate(R.layout.add_dialog, null)
            val etName = dialogView.findViewById<EditText>(R.id.inputName)
            var dlg = AlertDialog.Builder(this).apply {
                setTitle("Add PicInPic")
                setView(dialogView)
                setPositiveButton("OK") { _, _ ->
                    if (etName.text.toString() != "") {
                        intent.putExtra("wherefrom", "MainActivity")
                        intent.putExtra("itemname", etName.text.toString())
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity,
                            "Please input name",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                setOnCancelListener {
                    Toast.makeText(this@MainActivity, "Cancelled...", Toast.LENGTH_SHORT)
                        .show()
                }
                setNegativeButton("Cancel", null)
                show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checker.onDestroy()
    }

    private fun doCheck() {
        checker.checkAccess(licenseCheckerCallback)
    }

    private fun deleteUnusedPhoto() = runBlocking {
        val cw = ContextWrapper(applicationContext)
        val directory = cw.getDir("imageDir", Context.MODE_PRIVATE)
        val allFileList = ArrayList<String>()
        val allFiles = ArrayList<String>()
        for (i in directory.listFiles().indices) {
            allFileList.add((directory.listFiles()[i].toString()))
        }
        val mainsArray: List<MainList>
        val filesArray = mainAndListdao.getAllUserInfo()
        for (i in filesArray.indices) {
            allFiles.add(filesArray[i].destPhotoUri)
        }
        Log.d(TAG, "deleteUnusedPhoto2: $allFiles")
        val list = mainAndListdao.getAllMainListInfo()
        mainsArray = list
        for (i in mainsArray.indices) {
            allFiles.add(mainsArray[i].photoData)
        }
        Log.d(TAG, "deleteUnusedPhoto2: $allFiles")
        for (i in allFiles.indices) {
            allFileList.remove(allFiles[i])
        }
        Log.d(TAG, "deleteUnusedPhoto: $allFileList")
        var count = 0
        for (i in allFileList.indices) {
            val file = File(allFileList[i])
            file.delete()
            count++
        }
        Toast.makeText(this@MainActivity, "$count unused image deleted.", Toast.LENGTH_SHORT)
            .show()
    }

    private fun showAboutDialog() {
        val messageView = layoutInflater.inflate(R.layout.activity_info, null, false)
        val builder = AlertDialog.Builder(this, R.style.MyBackground).run {
            setTitle(R.string.app_name)//this wont work
            setIcon(R.mipmap.ic_launcher)
            setPositiveButton("OK") { _, _ ->
                Log.d(TAG, "onClick: Entering messageView.onClick")
                if (aboutDialog != null && aboutDialog?.isShowing == true) {
                    aboutDialog?.dismiss()
                }
            }
            if (BuildConfig.IS_FREE || !isGenuine) {
                setNeutralButton("Get PicInPic") { _, _ ->
                    Log.d(TAG, "showAboutDialog: paid clicked")
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.zynastor.picinpic"))
                    startActivity(intent)
                }
            }
            this
        }
        aboutDialog = builder.setView(messageView).create()
        aboutDialog?.setCanceledOnTouchOutside(true)
        messageView.setOnClickListener {
            Log.d(TAG, "entering messageView.onclick")
            if (aboutDialog != null && aboutDialog?.isShowing == true) {
                aboutDialog?.dismiss()
            }
        }
        val aboutVersion = messageView.findViewById(R.id.version) as TextView
        aboutVersion.text = BuildConfig.VERSION_NAME
        aboutDialog?.show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: called")
        getAllMainList()
    }

    private fun getAllMainList() {
        runBlocking {
            val insertt = mainAndListdao.getAllMainListInfo()
            Log.d(TAG, "getAllMainList: $insertt")
            listAdapter.setListData(insertt)
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun deleteMainlistInfo(list: MainList) {
        runBlocking{ mainAndListdao.deleteMainList(list) }
        getAllMainList()
        Log.d(TAG, "deleteMainlistInfo: notify called")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.info_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.info -> {
                showAboutDialog()
            }
            else -> onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDeleteUserClickListener(user: MainList) {
        deleteMainlistInfo(user); listAdapter.notifyDataSetChanged()
    }

    override fun onItemClickListener(main: MainList) {
//        if (mInterstitialAd.isLoaded && (BuildConfig.IS_FREE || !isGenuine)) {
//            Log.d(TAG, "onItemClickListener: $mInterstitialAd.isLoaded ")
//            mInterstitialAd.show()
//        } else {
//            Log.d(TAG, "onItemClickListener: ${mInterstitialAd.isLoaded}")
//            Log.d(TAG, "The interstitail wasnt loaded yet.")
//        }
        val intent = Intent(this@MainActivity, ShowActivity::class.java).run {
            putExtra("mainlist", main); putExtra("wherefrom", "MainActivity"); this
        }
        startActivity(intent)
    }

    private fun tedPermission() {
        val permissionlistener = object : PermissionListener {
            override fun onPermissionGranted() {}
            override fun onPermissionDenied(deniedPermissions: List<String>) {
                Toast.makeText(this@MainActivity,
                    "Denied. : \n$deniedPermissions", Toast.LENGTH_SHORT).show()
            }
        }
        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setRationaleMessage("Permission required to bring photo.")
            .setDeniedMessage("You can edit permission on Setting -> Permission.")
            .setPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .setPermissions(android.Manifest.permission.INTERNET)
            .check()
    }

    private inner class MyLicenseCheckerCallback : LicenseCheckerCallback {

        override fun allow(reason: Int) {
            if (isFinishing) {
                return
            }
            isGenuine = true
            Log.d(TAG, "allow: $isGenuine")
        }

        override fun dontAllow(reason: Int) {
            if (isFinishing) {
                return
            }
            isGenuine = false
            Log.d(TAG, "dontAllow: $isGenuine")
            if (reason == Policy.RETRY) {
                Log.d(TAG, "dontAllow: $reason")
            } else {
                Log.d(TAG, "dontAllow: $reason")
            }
        }

        override fun applicationError(errorCode: Int) {
            Log.e(TAG, "applicationError: ErrorCode: $errorCode")
        }
    }
}