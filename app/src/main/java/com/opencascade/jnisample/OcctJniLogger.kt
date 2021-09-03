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

import android.util.Log
import android.widget.TextView
import java.util.concurrent.locks.ReentrantLock

//! Auxiliary class for logging messages
object OcctJniLogger {
    //! Setup text view
    fun setTextView(theTextView: TextView?) {
        if (myTextView != null) {
            myLog = myTextView!!.text.toString()
        }
        myTextView = theTextView
        if (myTextView != null) {
            myTextView!!.text = myLog
            myLog = ""
        }
    }

    //! Interface implementation
    @JvmStatic
    fun postMessage(theText: String?) {
        var aCopy = String()
        aCopy += theText
        Log.e(myTag, theText)
        myMutex.lock()
        val aView = myTextView
        if (aView == null) {
            myLog += aCopy
            myMutex.unlock()
            return
        }
        aView.post(Runnable {
            aView.text = """
     ${aView.text}$aCopy

     """.trimIndent()
        })
        myMutex.unlock()
    }

    private const val myTag = "occtJniViewer"
    private val myMutex = ReentrantLock(true)
    private var myTextView: TextView? = null
    private var myLog = ""
}
