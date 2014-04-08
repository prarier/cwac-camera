/***
  Copyright (c) 2013-2014 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.camera;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.android.mms.exif.ExifInterface;

public class ImageCleanupTask extends Thread {
  private byte[] data;
  private int cameraId;
  private PictureTransaction xact=null;
  private boolean applyMatrix=true;

  ImageCleanupTask(Context ctxt, byte[] data, int cameraId,
                   PictureTransaction xact) {
    this.data=data;
    this.cameraId=cameraId;
    this.xact=xact;

    float heapPct=(float)data.length / calculateHeapSize(ctxt);

    applyMatrix=(heapPct < xact.host.maxPictureCleanupHeapUsage());
  }

  @Override
  public void run() {
    Camera.CameraInfo info=new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);

    DeviceProfile profile=xact.host.getDeviceProfile();
    
    Matrix matrix=new Matrix();
    Bitmap cleaned=null;
    ExifInterface exif;

    if (applyMatrix) {
      if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        if (profile.portraitFFCFlipped()
            && (xact.displayOrientation == 90 || xact.displayOrientation == 270)) {
          matrix.postScale(-1.0f, -1.0f);  // flip
        }
        else if (xact.mirrorFFC()) {
          matrix.postScale(-1.0f, 1.0f);  // mirror X axis
        }
      }

      boolean useDeviceRotation=profile.useDeviceOrientation();
      boolean useExifRotation=profile.useExifInfo();

      int angleDevice=0;
      int angleExif=0;

      if (useDeviceRotation) {
        angleDevice=xact.displayOrientation;
      }
      if (useExifRotation) {
        try {
          exif=new ExifInterface();
          exif.readExif(data);

          Integer exifOrientation=
              exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);

          if (exifOrientation != null) {
            if (exifOrientation == 6) {
              angleExif=90;
            }
            else if (exifOrientation == 8) {
              angleExif=270;
            }
            else if (exifOrientation == 3) {
              angleExif=180;
            }
            else if (exifOrientation == 1) {
              angleExif=0;
            }
            else {
              // angleExif=profile.getDefaultOrientation();
              //
              // if (angleExif == -1) {
              // angleExif=xact.displayOrientation;
              // }
            }
          } else {
            angleExif=-1;
          }
        }
        catch (IOException e) {
          Log.e("CWAC-Camera", "Exception parsing JPEG", e);
          // TODO: ripple to client
        }
      }

      if (useDeviceRotation) {
        matrix.preRotate(angleDevice);
      }
      if (useExifRotation && angleExif != -1) {
        matrix.preRotate(angleExif);
      }

      Matrix postProcess=xact.host.getPostProcessingMatrix(angleDevice, angleExif);
      if (postProcess != null) {
        matrix.postConcat(postProcess);
      }

      if (!matrix.isIdentity()) {
        Bitmap original=
            BitmapFactory.decodeByteArray(data, 0, data.length);
        cleaned=
            Bitmap.createBitmap(original, 0, 0, original.getWidth(),
                                original.getHeight(), matrix, true);
        original.recycle();
      }
    }

    if (xact.needBitmap) {
      if (cleaned == null) {
        cleaned=BitmapFactory.decodeByteArray(data, 0, data.length);
      }

      xact.host.saveImage(xact, cleaned);
    }

    if (xact.needByteArray) {
      if (!matrix.isIdentity()) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();

        // if (exif == null) {
        cleaned.compress(Bitmap.CompressFormat.JPEG, 100, out);
        // }
        // else {
        // exif.deleteTag(ExifInterface.TAG_ORIENTATION);
        //
        // try {
        // exif.writeExif(cleaned, out);
        // }
        // catch (IOException e) {
        // Log.e("CWAC-Camera", "Exception writing to JPEG",
        // e);
        // // TODO: ripple to client
        // }
        // }

        data=out.toByteArray();

        try {
          out.close();
        }
        catch (IOException e) {
          Log.e(CameraView.TAG, "Exception in closing a BAOS???", e);
        }
      }

      xact.host.saveImage(xact, data);
    }

    System.gc();
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private static int calculateHeapSize(Context ctxt) {
    ActivityManager am=
        (ActivityManager)ctxt.getSystemService(Context.ACTIVITY_SERVICE);
    int memoryClass=am.getMemoryClass();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      if ((ctxt.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0) {
        memoryClass=am.getLargeMemoryClass();
      }
    }

    return(memoryClass * 1048576); // MB * bytes in MB
  }
}