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

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;
import com.commonsware.cwac.camera.CameraHost.RecordingHint;

abstract public class DeviceProfile {
  abstract public boolean useTextureView();

  abstract public boolean portraitFFCFlipped();

  abstract public int getMinPictureHeight();

  abstract public int getMaxPictureHeight();

  abstract public boolean doesZoomActuallyWork(boolean isFFC);

  abstract public int getDefaultOrientation();

  abstract public boolean useDeviceOrientation();

  abstract public int getPictureDelay();

  abstract public RecordingHint getDefaultRecordingHint();

  private static volatile DeviceProfile SINGLETON=null;

  synchronized public static DeviceProfile getInstance(Context ctxt) {
    if (SINGLETON == null) {
//       android.util.Log.wtf("DeviceProfile",
//       String.format("\"%s\" \"%s\"", Build.MANUFACTURER,
//       Build.PRODUCT));

      if (isMotorolaRazrI()) {
        SINGLETON=new SimpleDeviceProfile.MotorolaRazrI();
      }
      else {
        int resource=findResource(ctxt);

        if (resource != 0) {
          SINGLETON=
              new SimpleDeviceProfile().load(ctxt.getResources()
                                                 .getXml(resource));
        }
        else {
          SINGLETON=new SimpleDeviceProfile();
        }
      }
    }

    return(SINGLETON);
  }

  private static int findResource(Context ctxt) {
    Resources res=ctxt.getResources();
    StringBuilder buf=new StringBuilder("cwac_camera_profile_");

    buf.append(clean(Build.MANUFACTURER));

    int mfrResult=
        res.getIdentifier(buf.toString(), "xml", ctxt.getPackageName());

    buf.append("_");
    buf.append(clean(Build.PRODUCT));

    int result=
        res.getIdentifier(buf.toString(), "xml", ctxt.getPackageName());

    return(result == 0 ? mfrResult : result);
  }

  private static String clean(String input) {
    return(input.replaceAll("[\\W]", "_").toLowerCase(Locale.US));
  }

  // based on http://stackoverflow.com/a/9801191/115145
  // and
  // https://github.com/commonsguy/cwac-camera/issues/43#issuecomment-23791446
  public static boolean isCyanogenMod() {
    return(System.getProperty("os.version").contains("cyanogenmod") || Build.HOST.contains("cyanogenmod"));
  }

  public static boolean isNexus3() {
    return("takju".equals(Build.PRODUCT) || "yakju".equals(Build.PRODUCT));
  }

  public static boolean isNexus4() {
    return("occam".equals(Build.PRODUCT));
  }

  public static boolean isAsus() {
    return("asus".equalsIgnoreCase(Build.MANUFACTURER));
  }

  public static boolean isNexusSeven2012() {
    return(isAsus() && "nakasi".equals(Build.PRODUCT));
  }

  public static boolean isHtc() {
    return("HTC".equalsIgnoreCase(Build.MANUFACTURER));
  }

  public static boolean isHtcOne() {
    return(isHtc() && "m7".equals(Build.PRODUCT));
  }

  public static boolean isSamsung() {
    return("samsung".equalsIgnoreCase(Build.MANUFACTURER));
  }

  public static boolean isSamsungGalaxyS3() {
    return(isSamsung() && ("d2att".equals(Build.PRODUCT) || "d2spr".equals(Build.PRODUCT)));
  }

  public static boolean isSamsungGalaxyS4Mini() {
    return(isSamsung() && (
        "serrano3gxx".equals(Build.PRODUCT) ||
        "GT-I9190".equalsIgnoreCase(Build.MODEL) ||
        "GT-I9195".equalsIgnoreCase(Build.MODEL)
    ));
  }

  public static boolean isSamsungGalaxyTab2() {
    return("espressowifiue".equals(Build.PRODUCT));
  }

  public static boolean isSamsungGalaxyCamera() {
    return("gd1wifiue".equals(Build.PRODUCT));
  }

  public static boolean isSamsungGalaxyAce3() {
    return("loganub".equals(Build.PRODUCT));
  }

  public static boolean isDroidIncredible2() {
    return("htc_vivow".equalsIgnoreCase(Build.PRODUCT));
  }

  public static boolean isXperiaE() {
    return("C1505_1271-7585".equalsIgnoreCase(Build.PRODUCT));
  }

  public static boolean isMotorola() {
    return("motorola".equalsIgnoreCase(Build.MANUFACTURER));
  }

  public static boolean isMotorolaRazrI() {
    return(isMotorola() && "XT890_rtgb".equals(Build.PRODUCT));
  }
}
