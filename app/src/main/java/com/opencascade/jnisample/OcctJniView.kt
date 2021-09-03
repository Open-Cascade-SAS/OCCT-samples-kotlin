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

import android.app.ActionBar
import android.content.Context
import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.opencascade.jnisample.OcctJniLogger.postMessage
import com.opencascade.jnisample.OcctJniRenderer.TypeOfOrientation
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

//! OpenGL ES 2.0+ view.
//! Performs rendering in parallel thread.
internal class OcctJniView(theContext: Context,
                           theAttrs: AttributeSet?) : GLSurfaceView(theContext, theAttrs) {
    //! Open file.
    fun open(thePath: String) {
        queueEvent { myRenderer!!.open(thePath) }
        requestRender()
    }

    //! Create OpenGL ES 2.0+ context
    private class ContextFactory : EGLContextFactory {
        override fun createContext(theEgl: EGL10,
                                   theEglDisplay: EGLDisplay,
                                   theEglConfig: EGLConfig): EGLContext {
            // reset EGL errors stack
            while (theEgl.eglGetError() != EGL10.EGL_SUCCESS) {}
            val anAttribs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
            val aEglContext = theEgl.eglCreateContext(theEglDisplay, theEglConfig, EGL10.EGL_NO_CONTEXT, anAttribs)
            var anError = theEgl.eglGetError()
            while (anError != EGL10.EGL_SUCCESS) {
                postMessage("Error: eglCreateContext() " + String.format("0x%x", anError))
                anError = theEgl.eglGetError()
            }
            return aEglContext
        }

        override fun destroyContext(theEgl: EGL10,
                                    theEglDisplay: EGLDisplay,
                                    theEglContext: EGLContext) {
            theEgl.eglDestroyContext(theEglDisplay, theEglContext)
        }

        companion object {
            private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        }
    }

    //! Search for RGB24 config with depth and stencil buffers
    private class ConfigChooser : EGLConfigChooser {
        //! Reset EGL errors stack
        private fun popEglErrors(theEgl: EGL10) {
            var anError = theEgl.eglGetError()
            while (anError != EGL10.EGL_SUCCESS) {
                postMessage("EGL Error: " + String.format("0x%x", anError))
                anError = theEgl.eglGetError()
            }
        }

        //! Auxiliary method to dump EGL configuration - for debugging purposes
        private fun printConfig(theEgl: EGL10,
                                theEglDisplay: EGLDisplay,
                                theEglConfig: EGLConfig) {
            val THE_ATTRIBS = intArrayOf(
                    EGL10.EGL_BUFFER_SIZE, EGL10.EGL_ALPHA_SIZE, EGL10.EGL_BLUE_SIZE, EGL10.EGL_GREEN_SIZE, EGL10.EGL_RED_SIZE, EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE,
                    EGL10.EGL_CONFIG_CAVEAT,
                    EGL10.EGL_CONFIG_ID,
                    EGL10.EGL_LEVEL,
                    EGL10.EGL_MAX_PBUFFER_HEIGHT, EGL10.EGL_MAX_PBUFFER_PIXELS, EGL10.EGL_MAX_PBUFFER_WIDTH,
                    EGL10.EGL_NATIVE_RENDERABLE, EGL10.EGL_NATIVE_VISUAL_ID, EGL10.EGL_NATIVE_VISUAL_TYPE,
                    0x3030,  // EGL10.EGL_PRESERVED_RESOURCES,
                    EGL10.EGL_SAMPLES, EGL10.EGL_SAMPLE_BUFFERS,
                    EGL10.EGL_SURFACE_TYPE,
                    EGL10.EGL_TRANSPARENT_TYPE, EGL10.EGL_TRANSPARENT_RED_VALUE, EGL10.EGL_TRANSPARENT_GREEN_VALUE, EGL10.EGL_TRANSPARENT_BLUE_VALUE,
                    0x3039, 0x303A,  // EGL10.EGL_BIND_TO_TEXTURE_RGB, EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                    0x303B, 0x303C,  // EGL10.EGL_MIN_SWAP_INTERVAL, EGL10.EGL_MAX_SWAP_INTERVAL
                    EGL10.EGL_LUMINANCE_SIZE, EGL10.EGL_ALPHA_MASK_SIZE,
                    EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RENDERABLE_TYPE,
                    0x3042 // EGL10.EGL_CONFORMANT
            )
            val THE_NAMES = arrayOf(
                    "EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE", "EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE", "EGL_DEPTH_SIZE", "EGL_STENCIL_SIZE",
                    "EGL_CONFIG_CAVEAT",
                    "EGL_CONFIG_ID",
                    "EGL_LEVEL",
                    "EGL_MAX_PBUFFER_HEIGHT", "EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH",
                    "EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID", "EGL_NATIVE_VISUAL_TYPE",
                    "EGL_PRESERVED_RESOURCES",
                    "EGL_SAMPLES", "EGL_SAMPLE_BUFFERS",
                    "EGL_SURFACE_TYPE",
                    "EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE", "EGL_TRANSPARENT_GREEN_VALUE", "EGL_TRANSPARENT_BLUE_VALUE",
                    "EGL_BIND_TO_TEXTURE_RGB", "EGL_BIND_TO_TEXTURE_RGBA",
                    "EGL_MIN_SWAP_INTERVAL", "EGL_MAX_SWAP_INTERVAL",
                    "EGL_LUMINANCE_SIZE", "EGL_ALPHA_MASK_SIZE",
                    "EGL_COLOR_BUFFER_TYPE", "EGL_RENDERABLE_TYPE",
                    "EGL_CONFORMANT"
            )
            val aValue = IntArray(1)
            for (anAttrIter in THE_ATTRIBS.indices) {
                val anAttr = THE_ATTRIBS[anAttrIter]
                val aName = THE_NAMES[anAttrIter]
                if (theEgl.eglGetConfigAttrib(theEglDisplay, theEglConfig, anAttr, aValue)) {
                    postMessage(String.format("  %s: %d\n", aName, aValue[0]))
                } else {
                    popEglErrors(theEgl)
                }
            }
        }

        //! Interface implementation
        override fun chooseConfig(theEgl: EGL10,
                                  theEglDisplay: EGLDisplay): EGLConfig {
            val EGL_OPENGL_ES2_BIT = 4
            val aCfgAttribs = intArrayOf(
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 0,
                    EGL10.EGL_DEPTH_SIZE, 24,
                    EGL10.EGL_STENCIL_SIZE, 8,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_NONE
            )
            val aConfigs = arrayOfNulls<EGLConfig>(1)
            val aNbConfigs = IntArray(1)
            if (!theEgl.eglChooseConfig(theEglDisplay, aCfgAttribs, aConfigs, 1, aNbConfigs)
                    || aConfigs[0] == null) {
                aCfgAttribs[4 * 2 + 1] = 16 // try config with smaller depth buffer
                popEglErrors(theEgl)
                if (!theEgl.eglChooseConfig(theEglDisplay, aCfgAttribs, aConfigs, 1, aNbConfigs)
                        || aConfigs[0] == null) {
                    postMessage("Error: eglChooseConfig() has failed!")
                    ///return null
                }
            }

            //printConfig (theEgl, theEglDisplay, aConfigs[0]);
            return aConfigs[0]!!
        }
    }

    //! Callback to handle touch events
    override fun onTouchEvent(theEvent: MotionEvent): Boolean {
        val aMaskedAction = theEvent.actionMasked
        when (aMaskedAction) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val aPointerIndex = theEvent.actionIndex
                val aPointerId = theEvent.getPointerId(aPointerIndex)
                val aPnt = PointF(theEvent.getX(aPointerIndex), theEvent.getY(aPointerIndex))
                mySelectPoint = if (theEvent.pointerCount == 1) {
                    aPnt
                } else {
                    null
                }
                queueEvent { myRenderer!!.onAddTouchPoint(aPointerId, aPnt.x, aPnt.y) }
            }
            MotionEvent.ACTION_MOVE -> {
                val aNbPointers = theEvent.pointerCount
                var aPntIter = 0
                while (aPntIter < aNbPointers) {
                    val aPointerId = theEvent.getPointerId(aPntIter)
                    val aPnt = PointF(theEvent.getX(aPntIter), theEvent.getY(aPntIter))
                    queueEvent { myRenderer!!.onUpdateTouchPoint(aPointerId, aPnt.x, aPnt.y) }
                    ++aPntIter
                }
                if (mySelectPoint != null) {
                    val aTouchThreshold = 5.0f * myScreenDensity
                    val aPointerIndex = theEvent.actionIndex
                    val aDelta = PointF(theEvent.getX(aPointerIndex) - mySelectPoint!!.x, theEvent.getY(aPointerIndex) - mySelectPoint!!.y)
                    if (Math.abs(aDelta.x) > aTouchThreshold || Math.abs(aDelta.y) > aTouchThreshold) {
                        mySelectPoint = null
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (mySelectPoint != null) {
                    val aSelX = mySelectPoint!!.x
                    val aSelY = mySelectPoint!!.y
                    queueEvent { myRenderer!!.onSelectInViewer(aSelX, aSelY) }
                    mySelectPoint = null
                }
                val aPointerIndex = theEvent.actionIndex
                val aPointerId = theEvent.getPointerId(aPointerIndex)
                //val aPnt = PointF(theEvent.getX(aPointerIndex), theEvent.getY(aPointerIndex))
                queueEvent { myRenderer!!.onRemoveTouchPoint(aPointerId) }
            }
        }
        requestRender()
        return true
    }

    //! Fit All
    fun fitAll() {
        queueEvent { myRenderer!!.fitAll() }
        requestRender()
    }

    //! Move camera
    fun setProj(theProj: TypeOfOrientation?) {
        queueEvent { myRenderer!!.setProj(theProj) }
        requestRender()
    }

    //! OCCT viewer
    private var myRenderer: OcctJniRenderer? = null
    private val mySelectId = -1
    private var mySelectPoint: PointF? = null
    private var myScreenDensity = 1.0f

    // ! Default constructor.
    init {
        val aDispInfo = theContext.resources.displayMetrics
        myScreenDensity = aDispInfo.density
        preserveEGLContextOnPause = true
        setEGLContextFactory(ContextFactory())
        setEGLConfigChooser(ConfigChooser())
        val aLParams = RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        aLParams.addRule(RelativeLayout.ALIGN_TOP)
        myRenderer = OcctJniRenderer(this, myScreenDensity)
        setRenderer(myRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY // render on request to spare battery
    }
}
