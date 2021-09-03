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

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Environment
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.opencascade.jnisample.ListenerList.FireHandler
import java.io.File
import java.io.FilenameFilter
import java.util.*

//! Simple open file dialog
class OcctJniFileDialog(theActivity: Activity,
                        thePath: File) {

    private var myFileList: Array<String>? = emptyArray()
    private var myCurrentPath: File? = null
    private val myFileListenerList = ListenerList<FileSelectedListener>()
    private val myDialogDismissedList = ListenerList<DialogDismissedListener>()
    private val myActivity: Activity
    private var myFileEndsWith: MutableList<String>? = null

    //! Main constructor.
    init {
        var aPath = thePath
        myActivity = theActivity
        if (!aPath.exists()) {
            aPath = Environment.getExternalStorageDirectory()
        }
        loadFileList(aPath)
    }

    interface FileSelectedListener {
        fun fileSelected(theFile: File?)
    }

    interface DialogDismissedListener {
        fun dialogDismissed()
    }

    //! Create new dialog
    fun createFileDialog(): Dialog? {
        val anObjWrapper = arrayOfNulls<Any>(1)
        val aBuilder = AlertDialog.Builder(myActivity)
        aBuilder.setTitle(myCurrentPath!!.path)
        val aTitleLayout = LinearLayout(myActivity)
        aTitleLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        aTitleLayout.orientation = LinearLayout.VERTICAL
        val list = ListView(myActivity)
        list.isScrollingCacheEnabled = false
        list.setBackgroundColor(Color.parseColor("#33B5E5"))
        list.adapter = ArrayAdapter(myActivity, android.R.layout.select_dialog_item, myFileList)
        list.onItemClickListener = OnItemClickListener { arg0, view, pos, id ->
            if (arg0 == null || view == null || id == 0L) {} // dummy
            val fileChosen = myFileList!![pos]
            val aChosenFile = getChosenFile(fileChosen)
            if (aChosenFile.isDirectory) {
                loadFileList(aChosenFile)
                (anObjWrapper[0] as Dialog?)!!.cancel()
                (anObjWrapper[0] as Dialog?)!!.dismiss()
                showDialog()
            } else {
                (anObjWrapper[0] as Dialog?)!!.cancel()
                (anObjWrapper[0] as Dialog?)!!.dismiss()
                fireFileSelectedEvent(aChosenFile)
            }
        }
        list.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.6f)
        aTitleLayout.addView(list)
        aBuilder.setNegativeButton("Cancel", null)
        aBuilder.setView(aTitleLayout)

        val aDialog = aBuilder.show()
        aDialog.setOnDismissListener(DialogInterface.OnDismissListener { fireDialogDismissedEvent() })
        anObjWrapper[0] = aDialog
        return aDialog
    }

    fun addFileListener(theListener: FileSelectedListener) {
        myFileListenerList.add(theListener)
    }

    fun addDialogDismissedListener(theListener: DialogDismissedListener) {
        myDialogDismissedList.add(theListener)
    }

    //! Show file dialog
    fun showDialog() {
        createFileDialog()!!.show()
    }

    private fun fireFileSelectedEvent(theFile: File) {
        myFileListenerList.fireEvent(object : FireHandler<FileSelectedListener> {
            override fun fireEvent(theListener: FileSelectedListener) {
                theListener.fileSelected(theFile)
            }
        })
    }

    private fun fireDialogDismissedEvent() {
        myDialogDismissedList.fireEvent(object : FireHandler<DialogDismissedListener> {
            override fun fireEvent(theListener: DialogDismissedListener) {
                theListener.dialogDismissed()
            }
        })
    }

    private fun loadFileList(thePath: File?) {
        myCurrentPath = thePath
        val aList: MutableList<String> = ArrayList()
        if (thePath!!.exists()) {
            if (thePath.parentFile != null) {
                aList.add(PARENT_DIR)
            }
            val aFilter = FilenameFilter { theDir, theFilename ->
                val aSel = File(theDir, theFilename)
                if (!aSel.canRead()) {
                    return@FilenameFilter false
                }
                var isEndWith = false
                if (myFileEndsWith != null) {
                    for (aFileExtIter in myFileEndsWith!!) {
                        if (theFilename.toLowerCase().endsWith(aFileExtIter)) {
                            isEndWith = true
                            break
                        }
                    }
                }
                isEndWith || aSel.isDirectory
            }
            val aFileList1 = thePath.list(aFilter)
            if (aFileList1 != null) {
                for (aFileIter in aFileList1) {
                    aList.add(aFileIter)
                }
            }
        }
        myFileList = aList.toTypedArray()
    }

    private fun getChosenFile(theFileChosen: String): File {
        return if (theFileChosen == PARENT_DIR) myCurrentPath!!.parentFile else File(myCurrentPath, theFileChosen)
    }

    fun setFileEndsWith(fileEndsWith: String) {
        if (myFileEndsWith == null) {
            myFileEndsWith = ArrayList()
        }
        if (myFileEndsWith!!.indexOf(fileEndsWith) == -1) {
            myFileEndsWith!!.add(fileEndsWith)
        }
    }

    fun setFileEndsWith(theFileEndsWith: MutableList<String>?) {
        myFileEndsWith = theFileEndsWith
    }

    companion object {
        private const val PARENT_DIR = ".."
    }
}

internal class ListenerList<L> {
    private val myListenerList: MutableList<L> = ArrayList()

    interface FireHandler<L> {
        fun fireEvent(theListener: L)
    }

    fun add(theListener: L) {
        myListenerList.add(theListener)
    }

    fun fireEvent(theFireHandler: FireHandler<L>) {
        val aCopy: List<L> = ArrayList(myListenerList)
        for (anIter in aCopy) {
            theFireHandler.fireEvent(anIter)
        }
    }

    fun remove(theListener: L) {
        myListenerList.remove(theListener)
    }

    val listenerList: List<L>
        get() = myListenerList
}
