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

#ifndef OcctJni_MsgPrinter_H
#define OcctJni_MsgPrinter_H

#include <Message_Printer.hxx>

#include <jni.h>

// Class providing connection between messenger interfaces in C++ and Java layers.
class OcctJni_MsgPrinter : public Message_Printer
{
  DEFINE_STANDARD_RTTIEXT(OcctJni_MsgPrinter, Message_Printer)
public:

  //! Default constructor
  OcctJni_MsgPrinter (JNIEnv* theJEnv,
                      jobject theJObj);

  //! Destructor.
  ~OcctJni_MsgPrinter();

protected:

  //! Main printing method
  virtual void send (const TCollection_AsciiString& theString,
                     const Message_Gravity theGravity) const override;

private:

  JNIEnv*   myJEnv;
  jobject   myJObj;
  jmethodID myJMet;

};

#endif // OcctJni_MsgPrinter_H
