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

#include "OcctJni_MsgPrinter.hxx"

#include <TCollection_AsciiString.hxx>
#include <TCollection_ExtendedString.hxx>

#include <android/log.h>

IMPLEMENT_STANDARD_RTTIEXT(OcctJni_MsgPrinter, Message_Printer)

// =======================================================================
// function : OcctJni_MsgPrinter
// purpose  :
// =======================================================================
OcctJni_MsgPrinter::OcctJni_MsgPrinter (JNIEnv* theJEnv,
                                        jobject theJObj)
: myJEnv (theJEnv),
  myJObj (theJEnv->NewGlobalRef (theJObj)),
  myJMet (NULL)
{
  jclass aJClass = theJEnv->GetObjectClass (theJObj);
  myJMet = theJEnv->GetMethodID (aJClass, "postMessage", "(Ljava/lang/String;)V");
  if (myJMet == NULL)
  {
    __android_log_write (ANDROID_LOG_FATAL, "jniSample", "Broken initialization of OcctJni_MsgPrinter!");
  }
}

// =======================================================================
// function : ~OcctJni_MsgPrinter
// purpose  :
// =======================================================================
OcctJni_MsgPrinter::~OcctJni_MsgPrinter()
{
  //myJEnv->DeleteGlobalRef (myJObj);
}

// =======================================================================
// function : send
// purpose  :
// =======================================================================
void OcctJni_MsgPrinter::send (const TCollection_AsciiString& theString,
                               const Message_Gravity theGravity) const
{
  if (theGravity < myTraceLevel)
  {
    return;
  }

  ///__android_log_write (ANDROID_LOG_DEBUG, "OcctJni_MsgPrinter", (TCollection_AsciiString(" @@ ") + theString).ToCString());
  if (myJMet == NULL)
  {
    return;
  }

  jstring aJStr = myJEnv->NewStringUTF ((theString + "\n").ToCString());
  myJEnv->CallVoidMethod (myJObj, myJMet, aJStr);
  myJEnv->DeleteLocalRef (aJStr);
}
