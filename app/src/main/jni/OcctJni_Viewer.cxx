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

#include "OcctJni_Viewer.hxx"
#include "OcctJni_MsgPrinter.hxx"

#include <AIS_ViewCube.hxx>
#include <AIS_Shape.hxx>
#include <Aspect_NeutralWindow.hxx>
#include <Image_AlienPixMap.hxx>
#include <BRepTools.hxx>
#include <Message_Messenger.hxx>
#include <Message_PrinterSystemLog.hxx>
#include <OpenGl_GraphicDriver.hxx>
#include <OSD_Timer.hxx>
#include <Prs3d_DatumAspect.hxx>
#include <Standard_Version.hxx>

#include <BRepPrimAPI_MakeBox.hxx>

#include <RWStl.hxx>
#include <IGESControl_Reader.hxx>
#include <STEPControl_Reader.hxx>
#include <XSControl_WorkSession.hxx>

#include <EGL/egl.h>

#include <sys/types.h>
#include <sys/stat.h>

#include <jni.h>

// =======================================================================
// function : OcctJni_Viewer
// purpose  :
// =======================================================================
OcctJni_Viewer::OcctJni_Viewer (float theDispDensity)
: myDevicePixelRatio (theDispDensity),
  myIsJniMoreFrames (false)
{
  SetTouchToleranceScale (theDispDensity);
#ifndef NDEBUG
  // Register printer for logging messages into global Android log.
  // Should never be used in production (or specify higher gravity for logging only failures).
  Handle(Message_Messenger) aMsgMgr = Message::DefaultMessenger();
  aMsgMgr->RemovePrinters (STANDARD_TYPE (Message_PrinterSystemLog));
  aMsgMgr->AddPrinter (new Message_PrinterSystemLog ("OcctJni_Viewer"));
#endif
}

// ================================================================
// Function : dumpGlInfo
// Purpose  :
// ================================================================
void OcctJni_Viewer::dumpGlInfo (bool theIsBasic)
{
  TColStd_IndexedDataMapOfStringString aGlCapsDict;
  myView->DiagnosticInformation (aGlCapsDict, Graphic3d_DiagnosticInfo_Basic); //theIsBasic ? Graphic3d_DiagnosticInfo_Basic : Graphic3d_DiagnosticInfo_Complete);
  if (theIsBasic)
  {
    TCollection_AsciiString aViewport;
    aGlCapsDict.FindFromKey ("Viewport", aViewport);
    aGlCapsDict.Clear();
    aGlCapsDict.Add ("Viewport", aViewport);
  }
  aGlCapsDict.Add ("Display scale", TCollection_AsciiString(myDevicePixelRatio));

  // beautify output
  {
    TCollection_AsciiString* aGlVer   = aGlCapsDict.ChangeSeek ("GLversion");
    TCollection_AsciiString* aGlslVer = aGlCapsDict.ChangeSeek ("GLSLversion");
    if (aGlVer   != NULL
     && aGlslVer != NULL)
    {
      *aGlVer = *aGlVer + " [GLSL: " + *aGlslVer + "]";
      aGlslVer->Clear();
    }
  }

  TCollection_AsciiString anInfo;
  for (TColStd_IndexedDataMapOfStringString::Iterator aValueIter (aGlCapsDict); aValueIter.More(); aValueIter.Next())
  {
    if (!aValueIter.Value().IsEmpty())
    {
      if (!anInfo.IsEmpty())
      {
        anInfo += "\n";
      }
      anInfo += aValueIter.Key() + ": " + aValueIter.Value();
    }
  }

  Message::SendWarning (anInfo);
}

// =======================================================================
// function : init
// purpose  :
// =======================================================================
bool OcctJni_Viewer::init()
{
  EGLint aCfgId = 0;
  int aWidth = 0, aHeight = 0;
  EGLDisplay anEglDisplay = eglGetCurrentDisplay();
  EGLContext anEglContext = eglGetCurrentContext();
  EGLSurface anEglSurf    = eglGetCurrentSurface (EGL_DRAW);
  if (anEglDisplay == EGL_NO_DISPLAY
   || anEglContext == EGL_NO_CONTEXT
   || anEglSurf    == EGL_NO_SURFACE)
  {
    Message::SendFail ("Error: No active EGL context!");
    release();
    return false;
  }

  eglQuerySurface (anEglDisplay, anEglSurf, EGL_WIDTH,     &aWidth);
  eglQuerySurface (anEglDisplay, anEglSurf, EGL_HEIGHT,    &aHeight);
  eglQuerySurface (anEglDisplay, anEglSurf, EGL_CONFIG_ID, &aCfgId);
  const EGLint aConfigAttribs[] = { EGL_CONFIG_ID, aCfgId, EGL_NONE };
  EGLint       aNbConfigs = 0;
  void*        anEglConfig = NULL;
  if (eglChooseConfig (anEglDisplay, aConfigAttribs, &anEglConfig, 1, &aNbConfigs) != EGL_TRUE)
  {
    Message::SendFail ("Error: EGL does not provide compatible configurations!");
    release();
    return false;
  }

  if (!myViewer.IsNull())
  {
    Handle(OpenGl_GraphicDriver) aDriver = Handle(OpenGl_GraphicDriver)::DownCast (myViewer->Driver());
    Handle(Aspect_NeutralWindow) aWindow = Handle(Aspect_NeutralWindow)::DownCast (myView->Window());
    if (!aDriver->InitEglContext (anEglDisplay, anEglContext, anEglConfig))
    {
      Message::SendFail ("Error: OpenGl_GraphicDriver can not be initialized!");
      release();
      return false;
    }

    aWindow->SetSize (aWidth, aHeight);
    myView->SetWindow (aWindow, (Aspect_RenderingContext )anEglContext);
    dumpGlInfo (true);
    return true;
  }

  Handle(OpenGl_GraphicDriver) aDriver = new OpenGl_GraphicDriver (NULL, Standard_False);
  aDriver->ChangeOptions().buffersNoSwap = true;
  aDriver->ChangeOptions().buffersOpaqueAlpha = true;
  aDriver->ChangeOptions().useSystemBuffer = false;
  if (!aDriver->InitEglContext (anEglDisplay, anEglContext, anEglConfig))
  {
    Message::SendFail ("Error: OpenGl_GraphicDriver can not be initialized!");
    release();
    return false;
  }

  myTextStyle = new Prs3d_TextAspect();
  myTextStyle->SetFont (Font_NOF_ASCII_MONO);
  myTextStyle->SetHeight (12);
  myTextStyle->Aspect()->SetColor (Quantity_NOC_GRAY95);
  myTextStyle->Aspect()->SetColorSubTitle (Quantity_NOC_BLACK);
  myTextStyle->Aspect()->SetDisplayType (Aspect_TODT_SHADOW);
  myTextStyle->Aspect()->SetTextFontAspect (Font_FA_Bold);
  myTextStyle->Aspect()->SetTextZoomable (false);
  myTextStyle->SetHorizontalJustification (Graphic3d_HTA_LEFT);
  myTextStyle->SetVerticalJustification (Graphic3d_VTA_BOTTOM);

  // create viewer
  myViewer = new V3d_Viewer (aDriver);
  myViewer->SetDefaultBackgroundColor (Quantity_NOC_BLACK);
  myViewer->SetDefaultLights();
  myViewer->SetLightOn();

  // create AIS context
  myContext = new AIS_InteractiveContext (myViewer);
  myContext->SetPixelTolerance (int(myDevicePixelRatio * 6.0)); // increase tolerance and adjust to hi-dpi screens
  myContext->SetDisplayMode (AIS_Shaded, false);

  Handle(Aspect_NeutralWindow) aWindow = new Aspect_NeutralWindow();
  aWindow->SetSize (aWidth, aHeight);
  myView = myViewer->CreateView();
  myView->SetImmediateUpdate (false);
  myView->ChangeRenderingParams().Resolution = (unsigned int )(96.0 * myDevicePixelRatio + 0.5);
  myView->ChangeRenderingParams().ToShowStats = true;
  myView->ChangeRenderingParams().CollectedStats = (Graphic3d_RenderingParams::PerfCounters ) (Graphic3d_RenderingParams::PerfCounters_FrameRate | Graphic3d_RenderingParams::PerfCounters_Triangles);
  myView->ChangeRenderingParams().StatsTextAspect = myTextStyle->Aspect();
  myView->ChangeRenderingParams().StatsTextHeight = (int )myTextStyle->Height();

  myView->SetWindow (aWindow, (Aspect_RenderingContext )anEglContext);
  dumpGlInfo (false);
  //myView->TriedronDisplay (Aspect_TOTP_RIGHT_LOWER, Quantity_NOC_WHITE, 0.08 * myDevicePixelRatio, V3d_ZBUFFER);

  initContent();
  return true;
}

// =======================================================================
// function : release
// purpose  :
// =======================================================================
void OcctJni_Viewer::release()
{
  myContext.Nullify();
  myView.Nullify();
  myViewer.Nullify();
}

// =======================================================================
// function : resize
// purpose  :
// =======================================================================
void OcctJni_Viewer::resize (int theWidth,
                             int theHeight)
{
  if (myContext.IsNull())
  {
    Message::SendFail ("Resize failed - view is unavailable");
    return;
  }

  Handle(OpenGl_GraphicDriver) aDriver = Handle(OpenGl_GraphicDriver)::DownCast (myViewer->Driver());
  Handle(Aspect_NeutralWindow) aWindow = Handle(Aspect_NeutralWindow)::DownCast (myView->Window());
  aWindow->SetSize (theWidth, theHeight);
  //myView->MustBeResized(); // can be used instead of SetWindow() when EGLsurface has not been changed

  EGLContext anEglContext = eglGetCurrentContext();
  myView->SetWindow (aWindow, (Aspect_RenderingContext )anEglContext);
  dumpGlInfo (true);
}

// =======================================================================
// function : initContent
// purpose  :
// =======================================================================
void OcctJni_Viewer::initContent()
{
  myContext->RemoveAll (Standard_False);

  if (myViewCube.IsNull())
  {
    myViewCube = new AIS_ViewCube();
    {
      // setup view cube size
      static const double THE_CUBE_SIZE = 60.0;
      myViewCube->SetSize (myDevicePixelRatio * THE_CUBE_SIZE, false);
      myViewCube->SetBoxFacetExtension (myViewCube->Size() * 0.15);
      myViewCube->SetAxesPadding (myViewCube->Size() * 0.10);
      myViewCube->SetFontHeight  (THE_CUBE_SIZE * 0.16);
    }
    // presentation parameters
    myViewCube->SetTransformPersistence (new Graphic3d_TransformPers (Graphic3d_TMF_TriedronPers, Aspect_TOTP_RIGHT_LOWER, Graphic3d_Vec2i (200, 200)));
    myViewCube->Attributes()->SetDatumAspect (new Prs3d_DatumAspect());
    myViewCube->Attributes()->DatumAspect()->SetTextAspect (myTextStyle);
    // animation parameters
    myViewCube->SetViewAnimation (myViewAnimation);
    myViewCube->SetFixedAnimationLoop (false);
    myViewCube->SetAutoStartAnimation (true);
  }
  myContext->Display (myViewCube, false);

  OSD_Timer aTimer;
  aTimer.Start();
  if (!myShape.IsNull())
  {
    Handle(AIS_Shape) aShapePrs = new AIS_Shape (myShape);
    myContext->Display (aShapePrs, Standard_False);
  }
  else
  {
    BRepPrimAPI_MakeBox aBuilder (1.0, 2.0, 3.0);
    Handle(AIS_Shape) aShapePrs = new AIS_Shape (aBuilder.Shape());
    myContext->Display (aShapePrs, Standard_False);
  }
  myView->FitAll();

  aTimer.Stop();
  Message::SendInfo (TCollection_AsciiString() + "Presentation computed in " + aTimer.ElapsedTime() + " seconds");
}

//! Load shape from IGES file
static TopoDS_Shape loadIGES (const TCollection_AsciiString& thePath)
{
  TopoDS_Shape          aShape;
  IGESControl_Reader    aReader;
  IFSelect_ReturnStatus aReadStatus = IFSelect_RetFail;
  try
  {
    aReadStatus = aReader.ReadFile (thePath.ToCString());
  }
  catch (Standard_Failure)
  {
    Message::SendFail ("Error: IGES reader, computation error");
    return aShape;
  }

  if (aReadStatus != IFSelect_RetDone)
  {
    Message::SendFail ("Error: IGES reader, bad file format");
    return aShape;
  }

  // now perform the translation
  aReader.TransferRoots();
  if (aReader.NbShapes() <= 0)
  {
    Handle(XSControl_WorkSession) aWorkSession = new XSControl_WorkSession();
    aWorkSession->SelectNorm ("IGES");
    aReader.SetWS (aWorkSession, Standard_True);
    aReader.SetReadVisible (Standard_False);
    aReader.TransferRoots();
  }
  if (aReader.NbShapes() <= 0)
  {
    Message::SendFail ("Error: IGES reader, no shapes has been found");
    return aShape;
  }
  return aReader.OneShape();
}

//! Load shape from STEP file
static TopoDS_Shape loadSTEP (const TCollection_AsciiString& thePath)
{
  STEPControl_Reader    aReader;
  IFSelect_ReturnStatus aReadStatus = IFSelect_RetFail;
  try
  {
    aReadStatus = aReader.ReadFile (thePath.ToCString());
  }
  catch (Standard_Failure)
  {
    Message::SendFail ("Error: STEP reader, computation error");
    return TopoDS_Shape();
  }

  if (aReadStatus != IFSelect_RetDone)
  {
    Message::SendFail ("Error: STEP reader, bad file format");
    return TopoDS_Shape();
  }
  else if (aReader.NbRootsForTransfer() <= 0)
  {
    Message::SendFail ("Error: STEP reader, shape is empty");
    return TopoDS_Shape();
  }

  // now perform the translation
  aReader.TransferRoots();
  return aReader.OneShape();
}

//! Load shape from STL file
static TopoDS_Shape loadSTL (const TCollection_AsciiString& thePath)
{
  Handle(Poly_Triangulation) aTri = RWStl::ReadFile (thePath.ToCString());
  TopoDS_Face aFace;
  BRep_Builder().MakeFace (aFace, aTri);
  return aFace;
}

// =======================================================================
// function : open
// purpose  :
// =======================================================================
bool OcctJni_Viewer::open (const TCollection_AsciiString& thePath)
{
  myShape.Nullify();
  if (!myContext.IsNull())
  {
    myContext->RemoveAll (Standard_False);
    if (!myViewCube.IsNull())
    {
      myContext->Display (myViewCube, false);
    }
  }
  if (thePath.IsEmpty())
  {
    return false;
  }

  OSD_Timer aTimer;
  aTimer.Start();
  TCollection_AsciiString aFileName, aFormatStr;
  OSD_Path::FileNameAndExtension (thePath, aFileName, aFormatStr);
    aFormatStr.LowerCase();

  TopoDS_Shape aShape;
  if (aFormatStr == "stp"
   || aFormatStr == "step")
  {
    aShape = loadSTEP (thePath);
  }
  else if (aFormatStr == "igs"
        || aFormatStr == "iges")
  {
    aShape = loadIGES (thePath);
  }
  else if (aFormatStr == "stl")
  {
      aShape = loadSTL (thePath);
  }
  else
      // if (aFormatStr == "brep"
      //  || aFormatStr == "rle")
  {
    BRep_Builder aBuilder;
    if (!BRepTools::Read (aShape, thePath.ToCString(), aBuilder))
    {
      Message::SendInfo (TCollection_AsciiString() + "Error: file '" + thePath + "' can not be opened");
      return false;
    }
  }
  if (aShape.IsNull())
  {
    return false;
  }
  aTimer.Stop();
  Message::SendInfo (TCollection_AsciiString() + "File '" + thePath + "' loaded in " + aTimer.ElapsedTime() + " seconds");

  myShape = aShape;
  if (myContext.IsNull())
  {
    return true;
  }

  aTimer.Reset();
  aTimer.Start();

  Handle(AIS_Shape) aShapePrs = new AIS_Shape (aShape);
  myContext->Display (aShapePrs, Standard_False);
  myView->FitAll();

  aTimer.Stop();
  Message::SendInfo (TCollection_AsciiString() + "Presentation computed in " + aTimer.ElapsedTime() + " seconds");
  return true;
}

// =======================================================================
// function : saveSnapshot
// purpose  :
// =======================================================================
bool OcctJni_Viewer::saveSnapshot (const TCollection_AsciiString& thePath,
                                   int theWidth,
                                   int theHeight)
{
  if (myContext.IsNull()
   || thePath.IsEmpty())
  {
    Message::SendFail ("Image dump failed - view is unavailable");
    return false;
  }

  if (theWidth  < 1
   || theHeight < 1)
  {
    myView->Window()->Size (theWidth, theHeight);
  }
  if (theWidth  < 1
   || theHeight < 1)
  {
    Message::SendFail ("Image dump failed - view is unavailable");
    return false;
  }

  Image_AlienPixMap anImage;
  if (!anImage.InitTrash (Image_Format_BGRA, theWidth, theHeight))
  {
    Message::SendFail (TCollection_AsciiString() + "RGBA image " + theWidth + "x" + theHeight + " allocation failed");
    return false;
  }

  if (!myView->ToPixMap (anImage, theWidth, theHeight, Graphic3d_BT_RGBA))
  {
    Message::SendFail (TCollection_AsciiString() + "View dump to the image " + theWidth + "x" + theHeight + " failed");
  }
  if (!anImage.Save (thePath))
  {
    Message::SendFail (TCollection_AsciiString() + "Image saving to path '" + thePath + "' failed");
    return false;
  }
  Message::SendInfo (TCollection_AsciiString() + "View " + theWidth + "x" + theHeight + " dumped to image '" + thePath + "'");
  return true;
}

// ================================================================
// Function : handleViewRedraw
// Purpose  :
// ================================================================
void OcctJni_Viewer::handleViewRedraw (const Handle(AIS_InteractiveContext)& theCtx,
                                       const Handle(V3d_View)& theView)
{
  AIS_ViewController::handleViewRedraw (theCtx, theView);
  myIsJniMoreFrames = myToAskNextFrame;
}

// =======================================================================
// function : redraw
// purpose  :
// =======================================================================
bool OcctJni_Viewer::redraw()
{
  if (myView.IsNull())
  {
    return false;
  }

  // handle user input
  myIsJniMoreFrames = false;
  myView->InvalidateImmediate();
  FlushViewEvents (myContext, myView, true);
  return myIsJniMoreFrames;
}

// =======================================================================
// function : fitAll
// purpose  :
// =======================================================================
void OcctJni_Viewer::fitAll()
{
  if (myView.IsNull())
  {
    return;
  }

  myView->FitAll (0.01, Standard_False);
  myView->Invalidate();
}

#define jexp extern "C" JNIEXPORT

jexp jlong JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppCreate (JNIEnv* theEnv,
                                                                             jobject theObj,
                                                                             jfloat  theDispDensity)
{
  return jlong(new OcctJni_Viewer (theDispDensity));
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppDestroy (JNIEnv* theEnv,
                                                                             jobject theObj,
                                                                             jlong   theCppPtr)
{
  delete (OcctJni_Viewer* )theCppPtr;

  Handle(Message_Messenger) aMsgMgr = Message::DefaultMessenger();
  aMsgMgr->RemovePrinters (STANDARD_TYPE (OcctJni_MsgPrinter));
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppRelease (JNIEnv* theEnv,
                                                                             jobject theObj,
                                                                             jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->release();
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppInit (JNIEnv* theEnv,
                                                                          jobject theObj,
                                                                          jlong   theCppPtr)
{
  Handle(Message_Messenger) aMsgMgr = Message::DefaultMessenger();
  aMsgMgr->RemovePrinters (STANDARD_TYPE (OcctJni_MsgPrinter));
  aMsgMgr->AddPrinter (new OcctJni_MsgPrinter (theEnv, theObj));
  ((OcctJni_Viewer* )theCppPtr)->init();
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppResize (JNIEnv* theEnv,
                                                                            jobject theObj,
                                                                            jlong   theCppPtr,
                                                                            jint    theWidth,
                                                                            jint    theHeight)
{
  ((OcctJni_Viewer* )theCppPtr)->resize (theWidth, theHeight);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppOpen (JNIEnv* theEnv,
                                                                          jobject theObj,
                                                                          jlong   theCppPtr,
                                                                          jstring thePath)
{
  const char* aPathPtr = theEnv->GetStringUTFChars (thePath, 0);
  const TCollection_AsciiString aPath (aPathPtr);
  theEnv->ReleaseStringUTFChars (thePath, aPathPtr);
  ((OcctJni_Viewer* )theCppPtr)->open (aPath);
}

jexp jboolean JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppRedraw (JNIEnv* theEnv,
                                                                                jobject theObj,
                                                                                jlong   theCppPtr)
{
  return ((OcctJni_Viewer* )theCppPtr)->redraw() ? JNI_TRUE : JNI_FALSE;
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetAxoProj (JNIEnv* theEnv,
                                                                                jobject theObj,
                                                                                jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_XposYnegZpos);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetXposProj (JNIEnv* theEnv,
                                                                                 jobject theObj,
                                                                                 jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_Xpos);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetYposProj (JNIEnv* theEnv,
                                                                                 jobject theObj,
                                                                                 jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_Ypos);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetZposProj (JNIEnv* theEnv,
                                                                                 jobject theObj,
                                                                                 jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_Zpos);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetXnegProj (JNIEnv* theEnv,
                                                                                 jobject theObj,
                                                                                 jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_Xneg);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetYnegProj (JNIEnv* theEnv,
                                                                                 jobject theObj,
                                                                                 jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_Yneg);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSetZnegProj (JNIEnv* theEnv,
                                                                                 jobject theObj,
                                                                                 jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->setProj (V3d_Zneg);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppFitAll (JNIEnv* theEnv,
                                                                            jobject theObj,
                                                                            jlong   theCppPtr)
{
  ((OcctJni_Viewer* )theCppPtr)->fitAll();
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppAddTouchPoint (JNIEnv* theEnv,
                                                                                   jobject theObj,
                                                                                   jlong   theCppPtr,
                                                                                   jint    theId,
                                                                                   jfloat  theX,
                                                                                   jfloat  theY)
{
  ((OcctJni_Viewer* )theCppPtr)->AddTouchPoint (theId, Graphic3d_Vec2d (theX, theY));
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppUpdateTouchPoint (JNIEnv* theEnv,
                                                                                   jobject theObj,
                                                                                   jlong   theCppPtr,
                                                                                   jint    theId,
                                                                                   jfloat  theX,
                                                                                   jfloat  theY)
{
  ((OcctJni_Viewer* )theCppPtr)->UpdateTouchPoint (theId, Graphic3d_Vec2d (theX, theY));
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppRemoveTouchPoint (JNIEnv* theEnv,
                                                                                   jobject theObj,
                                                                                   jlong   theCppPtr,
                                                                                   jint    theId)
{
  ((OcctJni_Viewer* )theCppPtr)->RemoveTouchPoint (theId);
}

jexp void JNICALL Java_com_opencascade_jnisample_OcctJniRenderer_cppSelectInViewer (JNIEnv* theEnv,
                                                                                    jobject theObj,
                                                                                    jlong   theCppPtr,
                                                                                    jfloat  theX,
                                                                                    jfloat  theY)
{
  ((OcctJni_Viewer* )theCppPtr)->SelectInViewer (Graphic3d_Vec2i ((int )theX, (int )theY));
}

jexp jlong JNICALL Java_com_opencascade_jnisample_OcctJniActivity_cppOcctMajorVersion (JNIEnv* theEnv,
                                                                                       jobject theObj)
{
  return OCC_VERSION_MAJOR;
}

jexp jlong JNICALL Java_com_opencascade_jnisample_OcctJniActivity_cppOcctMinorVersion (JNIEnv* theEnv,
                                                                                       jobject theObj)
{
  return OCC_VERSION_MINOR;
}

jexp jlong JNICALL Java_com_opencascade_jnisample_OcctJniActivity_cppOcctMicroVersion (JNIEnv* theEnv,
                                                                                       jobject theObj)
{
  return OCC_VERSION_MAINTENANCE;
}
