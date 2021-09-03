// Copyright (c) 2014-2021 OPEN CASCADE SAS
//
// This file is part of the examples of the Open CASCADE Technology software library.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE

package com.opencascade.jnisample

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.text.Html.ImageGetter
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.jvm.Throws

//! Main activity
class OcctJniActivity : Activity(), View.OnClickListener {
    //! Auxiliary method to load native libraries
    fun loadNatives(): Boolean {
        if (wasNativesLoadCalled) {
            return areNativeLoaded
        }
        wasNativesLoadCalled = true
        val aLoaded = StringBuilder()
        val aFailed = StringBuilder()

        // copy OCCT resources
        val aResFolder = filesDir.absolutePath
        copyAssetFolder(assets, "src/SHMessage", "$aResFolder/SHMessage")
        copyAssetFolder(assets, "src/XSMessage", "$aResFolder/XSMessage")

        // C++ runtime
        loadLibVerbose("gnustl_shared", aLoaded, aFailed)

        // 3rd-parties
        loadLibVerbose("freetype", aLoaded, aFailed)
        loadLibVerbose("freeimage", aLoaded, aFailed)
        if ( // OCCT modeling
                !loadLibVerbose("TKernel", aLoaded, aFailed)
                || !loadLibVerbose("TKMath", aLoaded, aFailed)
                || !loadLibVerbose("TKG2d", aLoaded, aFailed)
                || !loadLibVerbose("TKG3d", aLoaded, aFailed)
                || !loadLibVerbose("TKGeomBase", aLoaded, aFailed)
                || !loadLibVerbose("TKBRep", aLoaded, aFailed)
                || !loadLibVerbose("TKGeomAlgo", aLoaded, aFailed)
                || !loadLibVerbose("TKTopAlgo", aLoaded, aFailed)
                || !loadLibVerbose("TKShHealing", aLoaded, aFailed)
                || !loadLibVerbose("TKMesh", aLoaded, aFailed) // exchange
                || !loadLibVerbose("TKPrim", aLoaded, aFailed)
                || !loadLibVerbose("TKBO", aLoaded, aFailed)
                || !loadLibVerbose("TKBool", aLoaded, aFailed)
                || !loadLibVerbose("TKFillet", aLoaded, aFailed)
                || !loadLibVerbose("TKOffset", aLoaded, aFailed)
                || !loadLibVerbose("TKXSBase", aLoaded, aFailed)
                || !loadLibVerbose("TKSTL",  aLoaded, aFailed)
                || !loadLibVerbose("TKIGES", aLoaded, aFailed)
                || !loadLibVerbose("TKSTEPBase", aLoaded, aFailed)
                || !loadLibVerbose("TKSTEPAttr", aLoaded, aFailed)
                || !loadLibVerbose("TKSTEP209", aLoaded, aFailed)
                || !loadLibVerbose("TKSTEP", aLoaded, aFailed) // OCCT Visualization
                || !loadLibVerbose("TKService", aLoaded, aFailed)
                || !loadLibVerbose("TKHLR", aLoaded, aFailed)
                || !loadLibVerbose("TKV3d", aLoaded, aFailed)
                || !loadLibVerbose("TKOpenGles", aLoaded, aFailed) // application code
                || !loadLibVerbose("TKJniSample", aLoaded, aFailed)) {
            nativeLoaded = aLoaded.toString()
            nativeFailed = aFailed.toString()
            areNativeLoaded = false
            //exitWithError (theActivity, "Broken apk?\n" + theFailedInfo);
            return false
        }
        nativeLoaded = aLoaded.toString()
        areNativeLoaded = true
        return true
    }

    //! Create activity
    override fun onCreate(theBundle: Bundle?) {
        super.onCreate(theBundle)
        val isLoaded = loadNatives()
        if (!isLoaded) {
            printShortInfo(this, nativeFailed)
            OcctJniLogger.postMessage("""
    $nativeLoaded
    $nativeFailed
    """.trimIndent())
        }
        setContentView(R.layout.activity_main)
        myOcctView = findViewById(R.id.custom_view) as OcctJniView
        myMessageTextView = findViewById(R.id.message_view) as TextView
        OcctJniLogger.setTextView(myMessageTextView)
        createViewAndButtons(Configuration.ORIENTATION_LANDSCAPE)
        myButtonPreferSize = defineButtonSize(findViewById(R.id.panel_menu) as LinearLayout)
        val aScrollBtn = findViewById(R.id.scroll_btn) as ImageButton
        aScrollBtn.y = myButtonPreferSize.toFloat()
        aScrollBtn.setOnTouchListener { theView, theEvent -> onScrollBtnTouch(theView, theEvent) }
        onConfigurationChanged(resources.configuration)
        val anIntent = intent
        val aDataUrl = anIntent?.data
        val aDataPath = if (aDataUrl != null) aDataUrl.path else ""
        myOcctView!!.open(aDataPath)
        myLastPath = aDataPath
        myContext = ContextWrapper(this)
        myContext!!.getExternalFilesDir(null)
    }

    //! Handle scroll events
    private fun onScrollBtnTouch(theView: View,
                                 theEvent: MotionEvent): Boolean {
        when (theEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                val aPanelMenu = findViewById(R.id.panel_menu) as LinearLayout
                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (aPanelMenu.visibility == View.VISIBLE) {
                    aPanelMenu.visibility = View.GONE
                    if (!isLandscape) {
                        (theView as ImageButton).setImageResource(R.drawable.open_p)
                        theView.setY(0f)
                    } else {
                        (theView as ImageButton).setImageResource(R.drawable.open_l)
                        theView.setX(0f)
                    }
                } else {
                    aPanelMenu.visibility = View.VISIBLE
                    if (!isLandscape) {
                        (theView as ImageButton).setImageResource(R.drawable.close_p)
                        theView.setY(myButtonPreferSize.toFloat())
                    } else {
                        (theView as ImageButton).setImageResource(R.drawable.close_l)
                        theView.setX(myButtonPreferSize.toFloat())
                    }
                }
            }
        }
        return false
    }

    //! Initialize views and buttons
    @Suppress("UNUSED_PARAMETER")
    private fun createViewAndButtons(theOrientation: Int) {
        // open button
        val anOpenButton = findViewById(R.id.open) as ImageButton
        anOpenButton.setOnClickListener(this)

        // fit all
        val aFitAllButton = findViewById(R.id.fit) as ImageButton
        aFitAllButton.setOnClickListener(this)
        aFitAllButton.setOnTouchListener { theView, theEvent -> onTouchButton(theView, theEvent) }

        // message
        val aMessageButton = findViewById(R.id.message) as ImageButton
        aMessageButton.setOnClickListener(this)

        // info
        val anInfoButton = findViewById(R.id.info) as ImageButton
        anInfoButton.setOnClickListener(this)

        // font for text view
        val anInfoView = findViewById(R.id.info_view) as TextView
        anInfoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

        // add submenu buttons
        createSubmenuBtn(R.id.view, R.id.view_group,
                Arrays.asList(R.id.proj_front, R.id.proj_top, R.id.proj_left,
                        R.id.proj_back, R.id.proj_bottom, R.id.proj_right),
                Arrays.asList(R.drawable.proj_front, R.drawable.proj_top, R.drawable.proj_left,
                        R.drawable.proj_back, R.drawable.proj_bottom, R.drawable.proj_right),
                4)
    }

    override fun onNewIntent(theIntent: Intent) {
        super.onNewIntent(theIntent)
        intent = theIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        OcctJniLogger.setTextView(null)
    }

    override fun onPause() {
        super.onPause()
        myOcctView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        myOcctView!!.onResume()
        val anIntent = intent
        val aDataUrl = anIntent?.data
        val aDataPath = if (aDataUrl != null) aDataUrl.path else ""
        if (aDataPath != myLastPath) {
            myOcctView!!.open(aDataPath)
            myLastPath = aDataPath
        }
    }

    //! Copy folder from assets
    private fun copyAssetFolder(theAssetMgr: AssetManager,
                                theAssetFolder: String,
                                theFolderPathTo: String): Boolean {
        return try {
            val aFiles = theAssetMgr.list(theAssetFolder)
            val aFolder = File(theFolderPathTo)
            aFolder.mkdirs()
            var isOk = true
            for (aFileIter in aFiles) {
                isOk = if (aFileIter.contains(".")) {
                    isOk and copyAsset(theAssetMgr,
                            "$theAssetFolder/$aFileIter",
                            "$theFolderPathTo/$aFileIter")
                } else {
                    isOk and copyAssetFolder(theAssetMgr,
                            "$theAssetFolder/$aFileIter",
                            "$theFolderPathTo/$aFileIter")
                }
            }
            isOk
        } catch (theError: Exception) {
            theError.printStackTrace()
            false
        }
    }

    //! Copy single file from assets
    private fun copyAsset(theAssetMgr: AssetManager,
                          thePathFrom: String,
                          thePathTo: String): Boolean {
        return try {
            val aStreamIn = theAssetMgr.open(thePathFrom)
            val aFileTo = File(thePathTo)
            aFileTo.createNewFile()
            val aStreamOut: OutputStream? = FileOutputStream(thePathTo)
            copyStreamContent(aStreamIn, aStreamOut)
            aStreamIn.close()
            aStreamOut!!.flush()
            aStreamOut.close()
            true
        } catch (theError: Exception) {
            theError.printStackTrace()
            false
        }
    }

    //! Show/hide text view
    private fun switchTextView(theTextView: TextView?,
                               theClickedBtn: ImageButton,
                               theToSwitchOn: Boolean) {
        if (theTextView != null && theTextView.visibility == View.GONE && theToSwitchOn) {
            theTextView.visibility = View.VISIBLE
            theClickedBtn.setBackgroundColor(resources.getColor(R.color.pressedBtnColor))
            setTextViewPosition(theTextView)
        } else {
            theTextView!!.visibility = View.GONE
            theClickedBtn.setBackgroundColor(resources.getColor(R.color.btnColor))
        }
    }

    //! Setup text view position
    private fun setTextViewPosition(theTextView: TextView?) {
        if (theTextView!!.visibility != View.VISIBLE) {
            return
        }
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            theTextView.x = myButtonPreferSize.toFloat()
            theTextView.y = 0f
        } else {
            theTextView.x = 0f
            theTextView.y = myButtonPreferSize.toFloat()
        }
    }

    override fun onClick(theButton: View) {
        val aClickedBtn = theButton as ImageButton
        when (aClickedBtn.id) {
            R.id.message -> {
                switchTextView(findViewById(R.id.info_view) as TextView,
                        findViewById(R.id.info) as ImageButton, false)
                switchTextView(myMessageTextView, aClickedBtn, true)
                return
            }
            R.id.info -> {
                var aText = getString(R.string.info_html)
                aText = String.format(aText, cppOcctMajorVersion(), cppOcctMinorVersion(), cppOcctMicroVersion())
                val aSpanned = Html.fromHtml(aText, ImageGetter { theSource ->
                    val aResources = resources
                    val anId = aResources.getIdentifier(theSource, "drawable", packageName)
                    val aRes = aResources.getDrawable(anId)
                    aRes.setBounds(0, 0, aRes.intrinsicWidth, aRes.intrinsicHeight)
                    aRes
                }, null)
                val anInfoView = findViewById(R.id.info_view) as TextView
                anInfoView.text = aSpanned
                switchTextView(myMessageTextView, findViewById(R.id.message) as ImageButton, false)
                switchTextView(anInfoView, aClickedBtn, true)
                return
            }
            R.id.fit -> {
                myOcctView!!.fitAll()
                return
            }
            R.id.proj_front -> {
                myOcctView!!.setProj(OcctJniRenderer.TypeOfOrientation.Xpos)
                return
            }
            R.id.proj_left -> {
                myOcctView!!.setProj(OcctJniRenderer.TypeOfOrientation.Yneg)
                return
            }
            R.id.proj_top -> {
                myOcctView!!.setProj(OcctJniRenderer.TypeOfOrientation.Zpos)
                return
            }
            R.id.proj_back -> {
                myOcctView!!.setProj(OcctJniRenderer.TypeOfOrientation.Xneg)
                return
            }
            R.id.proj_right -> {
                myOcctView!!.setProj(OcctJniRenderer.TypeOfOrientation.Ypos)
                return
            }
            R.id.proj_bottom -> {
                myOcctView!!.setProj(OcctJniRenderer.TypeOfOrientation.Zneg)
                return
            }
            R.id.open -> {
                val aPath = Environment.getExternalStorageDirectory()
                aClickedBtn.setBackgroundColor(resources.getColor(R.color.pressedBtnColor))
                if (myFileOpenDialog == null) {
                    // should be requested on runtime since API level 26 (Android 8)
                    askUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, null) // for accessing SD card
                    myFileOpenDialog = OcctJniFileDialog(this, aPath)
                    myFileOpenDialog!!.setFileEndsWith(".brep")
                    myFileOpenDialog!!.setFileEndsWith(".rle")
                    myFileOpenDialog!!.setFileEndsWith(".iges")
                    myFileOpenDialog!!.setFileEndsWith(".igs")
                    myFileOpenDialog!!.setFileEndsWith(".step")
                    myFileOpenDialog!!.setFileEndsWith(".stp")
                    myFileOpenDialog!!.setFileEndsWith(".stl")

                    myFileOpenDialog!!.addFileListener (object : OcctJniFileDialog.FileSelectedListener {
                        override fun fileSelected(theFile: File?) {
                            if (theFile != null && myOcctView != null) {
                                myOcctView!!.open(theFile.getPath())
                            }
                        }
                    })

                    myFileOpenDialog!!.addDialogDismissedListener (object : OcctJniFileDialog.DialogDismissedListener {
                        override fun dialogDismissed() {
                            val openButton = findViewById(R.id.open) as ImageButton
                            openButton.setBackgroundColor(resources.getColor(R.color.btnColor))
                        }
                    })
                }
                myFileOpenDialog!!.showDialog()
                return
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun createSubmenuBtn(theParentBtnId: Int,
                                 theParentLayoutId: Int,
                                 theNewButtonIds: List<Int>,
                                 theNewButtonImageIds: List<Int>,
                                 thePosition: Int) {
        var aPosInList = 0
        val aParentBtn = findViewById(theParentBtnId) as? ImageButton
        val aParams: ViewGroup.LayoutParams? = null
        val parentLayout = findViewById(theParentLayoutId) as LinearLayout
        for (newButtonId in theNewButtonIds) {
            var aNewButton = findViewById(newButtonId) as? ImageButton
            if (aNewButton == null) {
                aNewButton = ImageButton(this)
                aNewButton.id = newButtonId
                aNewButton.setImageResource(theNewButtonImageIds[aPosInList])
                aNewButton.layoutParams = aParams
                parentLayout.addView(aNewButton)
            }
            aNewButton.setOnClickListener(this)
            aNewButton.visibility = View.GONE
            aNewButton.setOnTouchListener { theView, theEvent -> onTouchButton(theView, theEvent) }
            ++aPosInList
        }
        if (aParentBtn != null) {
            aParentBtn.setOnTouchListener(null)
            aParentBtn.setOnTouchListener { theView, theEvent ->
                if (theView == null) {} // dummy
                if (theEvent.action == MotionEvent.ACTION_DOWN) {
                    var isVisible = false
                    for (aNewButtonId in theNewButtonIds) {
                        val anBtn = findViewById(aNewButtonId) as? ImageButton
                        if (anBtn != null) {
                            if (anBtn.visibility == View.GONE) {
                                anBtn.visibility = View.VISIBLE
                                isVisible = true
                            } else {
                                anBtn.visibility = View.GONE
                            }
                        }
                    }
                    aParentBtn.setBackgroundColor(if (!isVisible) resources.getColor(R.color.btnColor) else resources.getColor(R.color.pressedBtnColor))
                }
                false
            }
        }
    }

    //! Implements onTouch functionality
    private fun onTouchButton(theView: View,
                              theEvent: MotionEvent): Boolean {
        when (theEvent.action) {
            MotionEvent.ACTION_DOWN -> (theView as ImageButton).setBackgroundColor(resources.getColor(R.color.pressedBtnColor))
            MotionEvent.ACTION_UP -> (theView as ImageButton).setBackgroundColor(resources.getColor(R.color.btnColor))
        }
        return false
    }

    //! Handle configuration change event
    override fun onConfigurationChanged(theNewConfig: Configuration) {
        super.onConfigurationChanged(theNewConfig)
        val aLayoutPanelMenu = findViewById(R.id.panel_menu) as LinearLayout
        val aPanelMenuLayoutParams = aLayoutPanelMenu.layoutParams
        val aLayoutViewGroup = findViewById(R.id.view_group) as LinearLayout
        val aViewGroupLayoutParams = aLayoutViewGroup.layoutParams
        val aScrollBtn = findViewById(R.id.scroll_btn) as ImageButton
        val aScrollBtnLayoutParams = aScrollBtn.layoutParams
        myButtonPreferSize = defineButtonSize(findViewById(R.id.panel_menu) as LinearLayout)
        defineButtonSize(findViewById(R.id.view_group) as LinearLayout)
        when (theNewConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                setHorizontal(aLayoutPanelMenu, aPanelMenuLayoutParams)
                setHorizontal(aLayoutViewGroup, aViewGroupLayoutParams)
                aLayoutViewGroup.setGravity(Gravity.BOTTOM)
                aScrollBtnLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                aScrollBtnLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                aScrollBtn.layoutParams = aScrollBtnLayoutParams
                if (aLayoutPanelMenu.visibility == View.VISIBLE) {
                    aScrollBtn.setImageResource(R.drawable.close_p)
                    aScrollBtn.y = myButtonPreferSize.toFloat()
                    aScrollBtn.x = 0f
                } else {
                    aScrollBtn.setImageResource(R.drawable.open_p)
                    aScrollBtn.y = 0f
                    aScrollBtn.x = 0f
                }
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                setVertical(aLayoutPanelMenu, aPanelMenuLayoutParams)
                setVertical(aLayoutViewGroup, aViewGroupLayoutParams)
                aLayoutViewGroup.setGravity(Gravity.RIGHT)
                aScrollBtnLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                aScrollBtnLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                aScrollBtn.layoutParams = aScrollBtnLayoutParams
                if (aLayoutPanelMenu.visibility == View.VISIBLE) {
                    aScrollBtn.setImageResource(R.drawable.close_l)
                    aScrollBtn.x = myButtonPreferSize.toFloat()
                    aScrollBtn.y = 0f
                } else {
                    aScrollBtn.setImageResource(R.drawable.open_l)
                    aScrollBtn.y = 0f
                    aScrollBtn.x = 0f
                }
            }
        }
        setTextViewPosition(myMessageTextView)
        setTextViewPosition(findViewById(R.id.info_view) as TextView)
    }

    private fun setHorizontal(theLayout: LinearLayout,
                              theLayoutParams: ViewGroup.LayoutParams) {
        theLayout.orientation = LinearLayout.HORIZONTAL
        theLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        theLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        theLayout.layoutParams = theLayoutParams
    }

    private fun setVertical(theLayout: LinearLayout,
                            theLayoutParams: ViewGroup.LayoutParams) {
        theLayout.orientation = LinearLayout.VERTICAL
        theLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        theLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        theLayout.layoutParams = theLayoutParams
    }

    //! Define button size
    private fun defineButtonSize(theLayout: LinearLayout): Int {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val aDisplay = windowManager.defaultDisplay
        val aDispPnt = Point()
        aDisplay.getSize(aDispPnt)
        val aNbChildren = theLayout.childCount
        val aHeight = aDispPnt.y / aNbChildren
        val aWidth = aDispPnt.x / aNbChildren

        for (aChildIter in 0 until aNbChildren) {
            val aView = theLayout.getChildAt(aChildIter)
            if (aView is ImageButton) {
                val aButton = aView
                if (isLandscape) {
                    aButton.minimumWidth = aHeight
                } else {
                    aButton.minimumHeight = aWidth
                }
            }
        }
        return if (isLandscape) { aHeight } else { aWidth }
    }

    //! Request user permission.
    private fun askUserPermission(thePermission: String, theRationale: String?) {
        // Dynamically load methods introduced by API level 23.
        // On older system this permission is granted by user during application installation.
        val aMetPtrCheckSelfPermission: Method
        val aMetPtrRequestPermissions: Method
        val aMetPtrShouldShowRequestPermissionRationale: Method
        try {
            aMetPtrCheckSelfPermission = myContext!!.javaClass.getMethod("checkSelfPermission", String::class.java)
            aMetPtrRequestPermissions = javaClass.getMethod("requestPermissions", Array<String>::class.java, Int::class.javaPrimitiveType)
            aMetPtrShouldShowRequestPermissionRationale = javaClass.getMethod("shouldShowRequestPermissionRationale", String::class.java)
        } catch (theError: SecurityException) {
            postMessage("""
    Unable to find permission methods:
    ${theError.message}
    """.trimIndent(), Message_Trace)
            return
        } catch (theError: NoSuchMethodException) {
            postMessage("""
    Unable to find permission methods:
    ${theError.message}
    """.trimIndent(), Message_Trace)
            return
        }
        try {
            val isAlreadyGranted = aMetPtrCheckSelfPermission.invoke(myContext, thePermission) as Int
            if (isAlreadyGranted == PackageManager.PERMISSION_GRANTED) {
                return
            }
            val toShowInfo = theRationale != null && aMetPtrShouldShowRequestPermissionRationale.invoke(this, thePermission) as Boolean
            if (toShowInfo) {
                postMessage(theRationale, Message_Info)
            }

            // show dialog to user
            aMetPtrRequestPermissions.invoke(this, arrayOf(thePermission), 0)
        } catch (theError: IllegalArgumentException) {
            postMessage("""
    Internal error: Unable to call permission method:
    ${theError.message}
    """.trimIndent(), Message_Fail)
            return
        } catch (theError: IllegalAccessException) {
            postMessage("""
    Internal error: Unable to call permission method:
    ${theError.message}
    """.trimIndent(), Message_Fail)
            return
        } catch (theError: InvocationTargetException) {
            postMessage("""
    Internal error: Unable to call permission method:
    ${theError.message}
    """.trimIndent(), Message_Fail)
            return
        }
    }

    //! Auxiliary method to show info message.
    fun postMessage(theMessage: String?, theGravity: Int) {
        if (theGravity == Message_Trace) {
            return
        }
        val aCtx: Context = this
        runOnUiThread {
            val aBuilder = AlertDialog.Builder(aCtx)
            aBuilder.setMessage(theMessage).setNegativeButton("OK", null)
            val aDialog = aBuilder.create()
            aDialog.show()
        }
    }

    //! OCCT major version
    private external fun cppOcctMajorVersion(): Long

    //! OCCT minor version
    private external fun cppOcctMinorVersion(): Long

    //! OCCT micro version
    private external fun cppOcctMicroVersion(): Long
    private var myOcctView: OcctJniView? = null
    private var myMessageTextView: TextView? = null
    private var myLastPath: String? = null
    private var myContext: ContextWrapper? = null
    private var myFileOpenDialog: OcctJniFileDialog? = null
    private var myButtonPreferSize = 65

    companion object {
        //! Auxiliary method to print temporary info messages
        fun printShortInfo(theActivity: Activity,
                           theInfo: CharSequence?) {
            val aCtx = theActivity.applicationContext
            val aToast = Toast.makeText(aCtx, theInfo, Toast.LENGTH_LONG)
            aToast.show()
        }

        //! Load single native library
        private fun loadLibVerbose(theLibName: String,
                                   theLoadedInfo: StringBuilder,
                                   theFailedInfo: StringBuilder): Boolean {
            return try {
                System.loadLibrary(theLibName)
                theLoadedInfo.append("Info:  native library \"")
                theLoadedInfo.append(theLibName)
                theLoadedInfo.append("\" has been loaded\n")
                true
            } catch (theError: UnsatisfiedLinkError) {
                theFailedInfo.append("Error: native library \"")
                theFailedInfo.append(theLibName)
                theFailedInfo.append("""" is unavailable:
  ${theError.message}""")
                false
            } catch (theError: SecurityException) {
                theFailedInfo.append("Error: native library \"")
                theFailedInfo.append(theLibName)
                theFailedInfo.append("""" can not be loaded for security reasons:
  ${theError.message}""")
                false
            }
        }

        var wasNativesLoadCalled = false
        @JvmField
        var areNativeLoaded = false
        var nativeLoaded = ""
        var nativeFailed = ""

        //! Copy single file
        @Throws(IOException::class)
        private fun copyStreamContent(theIn: InputStream?,
                                      theOut: OutputStream?) {
            val aBuffer = ByteArray(1024)
            var aNbReadBytes: Int
            while (theIn!!.read(aBuffer).also { aNbReadBytes = it } != -1) {
                theOut!!.write(aBuffer, 0, aNbReadBytes)
            }
        }

        //! Message gravity.
        private const val Message_Trace = 0
        private const val Message_Info = 1
        private const val Message_Warning = 2
        private const val Message_Alarm = 3
        private const val Message_Fail = 4
    }
}
