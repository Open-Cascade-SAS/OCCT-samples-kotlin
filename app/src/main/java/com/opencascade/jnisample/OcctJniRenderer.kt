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

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//! Wrapper for C++ OCCT viewer.
class OcctJniRenderer internal constructor(theView: GLSurfaceView?,
                                           theScreenDensity: Float) : GLSurfaceView.Renderer {
    //! Wrapper for V3d_TypeOfOrientation
    enum class TypeOfOrientation {
        Xpos,  // front
        Ypos,  // left
        Zpos,  // top
        Xneg,  // back
        Yneg,  // right
        Zneg // bottom
    }

    //! Open file.
    fun open(thePath: String) {
        if (myCppViewer != 0L) {
            cppOpen(myCppViewer, thePath)
        }
    }

    //! Update viewer.
    override fun onDrawFrame(theGl: GL10) {
        if (myCppViewer != 0L) {
            if (cppRedraw(myCppViewer)) {
                myView!!.requestRender() // this method is allowed from any thread
            }
        }
    }

    //! (re)initialize viewer.
    override fun onSurfaceChanged(theGl: GL10, theWidth: Int, theHeight: Int) {
        if (myCppViewer != 0L) {
            cppResize(myCppViewer, theWidth, theHeight)
        }
    }

    override fun onSurfaceCreated(theGl: GL10, theEglConfig: EGLConfig) {
        if (myCppViewer != 0L) {
            cppInit(myCppViewer)
        }
    }

    //! Add touch point.
    fun onAddTouchPoint(theId: Int, theX: Float, theY: Float) {
        if (myCppViewer != 0L) {
            cppAddTouchPoint(myCppViewer, theId, theX, theY)
        }
    }

    //! Update touch point.
    fun onUpdateTouchPoint(theId: Int, theX: Float, theY: Float) {
        if (myCppViewer != 0L) {
            cppUpdateTouchPoint(myCppViewer, theId, theX, theY)
        }
    }

    //! Remove touch point.
    fun onRemoveTouchPoint(theId: Int) {
        if (myCppViewer != 0L) {
            cppRemoveTouchPoint(myCppViewer, theId)
        }
    }

    //! Select in 3D Viewer.
    fun onSelectInViewer(theX: Float, theY: Float) {
        if (myCppViewer != 0L) {
            cppSelectInViewer(myCppViewer, theX, theY)
        }
    }

    //! Fit All
    fun fitAll() {
        if (myCppViewer != 0L) {
            cppFitAll(myCppViewer)
        }
    }

    //! Move camera
    fun setProj(theProj: TypeOfOrientation?) {
        if (myCppViewer == 0L) {
            return
        }
        when (theProj) {
            TypeOfOrientation.Xpos -> cppSetXposProj(myCppViewer)
            TypeOfOrientation.Ypos -> cppSetYposProj(myCppViewer)
            TypeOfOrientation.Zpos -> cppSetZposProj(myCppViewer)
            TypeOfOrientation.Xneg -> cppSetXnegProj(myCppViewer)
            TypeOfOrientation.Yneg -> cppSetYnegProj(myCppViewer)
            TypeOfOrientation.Zneg -> cppSetZnegProj(myCppViewer)
        }
    }

    //! Post message to the text view.
    fun postMessage(theText: String?) {
        OcctJniLogger.postMessage(theText)
    }

    //! Create instance of C++ class
    private external fun cppCreate(theDispDensity: Float): Long

    //! Destroy instance of C++ class
    private external fun cppDestroy(theCppPtr: Long)

    //! Initialize OCCT viewer (steal OpenGL ES context bound to this thread)
    private external fun cppInit(theCppPtr: Long)

    //! Resize OCCT viewer
    private external fun cppResize(theCppPtr: Long, theWidth: Int, theHeight: Int)

    //! Open CAD file
    private external fun cppOpen(theCppPtr: Long, thePath: String)

    //! Add touch point
    private external fun cppAddTouchPoint(theCppPtr: Long, theId: Int, theX: Float, theY: Float)

    //! Update touch point
    private external fun cppUpdateTouchPoint(theCppPtr: Long, theId: Int, theX: Float, theY: Float)

    //! Remove touch point
    private external fun cppRemoveTouchPoint(theCppPtr: Long, theId: Int)

    //! Select in 3D Viewer.
    private external fun cppSelectInViewer(theCppPtr: Long, theX: Float, theY: Float)

    //! Redraw OCCT viewer
    //! Returns TRUE if more frames are requested.
    private external fun cppRedraw(theCppPtr: Long): Boolean

    //! Fit All
    private external fun cppFitAll(theCppPtr: Long)

    //! Move camera
    private external fun cppSetXposProj(theCppPtr: Long)

    //! Move camera
    private external fun cppSetYposProj(theCppPtr: Long)

    //! Move camera
    private external fun cppSetZposProj(theCppPtr: Long)

    //! Move camera
    private external fun cppSetXnegProj(theCppPtr: Long)

    //! Move camera
    private external fun cppSetYnegProj(theCppPtr: Long)

    //! Move camera
    private external fun cppSetZnegProj(theCppPtr: Long)
    private var myView: GLSurfaceView? = null //!< back reference to the View
    private var myCppViewer: Long = 0 //!< pointer to c++ class instance

    //! Empty constructor.
    init {
        myView = theView // this makes cyclic dependency, but it is OK for JVM
        if (OcctJniActivity.areNativeLoaded) {
            myCppViewer = cppCreate(theScreenDensity)
        }
    }
}
