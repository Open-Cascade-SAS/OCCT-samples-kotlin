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

#include <AIS_InteractiveContext.hxx>
#include <AIS_ViewController.hxx>
#include <TopoDS_Shape.hxx>
#include <V3d_Viewer.hxx>
#include <V3d_View.hxx>

class AIS_ViewCube;

//! Main C++ back-end for activity.
class OcctJni_Viewer : public AIS_ViewController
{

public:

  //! Empty constructor
  OcctJni_Viewer (float theDispDensity);

  //! Initialize the viewer
  bool init();

  //! Release the viewer
  void release();

  //! Resize the viewer
  void resize (int theWidth,
               int theHeight);

  //! Open CAD file
  bool open (const TCollection_AsciiString& thePath);

  //! Take snapshot
  bool saveSnapshot (const TCollection_AsciiString& thePath,
                     int theWidth  = 0,
                     int theHeight = 0);

  //! Viewer update.
  //! Returns TRUE if more frames should be requested.
  bool redraw();

  //! Move camera
  void setProj (V3d_TypeOfOrientation theProj)
  {
    if (myView.IsNull())
    {
      return;
    }

    myView->SetProj (theProj);
    myView->Invalidate();
  }

  //! Fit All.
  void fitAll();

protected:

  //! Reset viewer content.
  void initContent();

  //! Print information about OpenGL ES context.
  void dumpGlInfo (bool theIsBasic);

  //! Handle redraw.
  virtual void handleViewRedraw (const Handle(AIS_InteractiveContext)& theCtx,
                                 const Handle(V3d_View)& theView) override;

protected:

  Handle(V3d_Viewer)             myViewer;
  Handle(V3d_View)               myView;
  Handle(AIS_InteractiveContext) myContext;
  Handle(Prs3d_TextAspect)       myTextStyle; //!< text style for OSD elements
  Handle(AIS_ViewCube)           myViewCube;  //!< view cube object
  TopoDS_Shape                   myShape;
  float                          myDevicePixelRatio; //!< device pixel ratio for handling high DPI displays
  bool                           myIsJniMoreFrames;  //!< need more frame flag

};
