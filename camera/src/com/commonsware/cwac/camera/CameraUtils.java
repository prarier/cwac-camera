/***
  Copyright (c) 2013 CommonsWare, LLC
  
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

import android.hardware.Camera;
import android.hardware.Camera.Size;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtils {
  // based on ApiDemos

  private static final double ASPECT_TOLERANCE=0.1;

  public static Camera.Size getOptimalPreviewSize(
      int displayOrientation, int width, int height, Camera.Parameters parameters
  ) {
    return getComparedSize(displayOrientation, width, height,
        SizeComparator.MODE_PREVIEW, parameters.getSupportedPreviewSizes());
  }

  public static Camera.Size getLargestPictureSize(
      int displayOrientation, int width, int height, Camera.Parameters parameters
  ) {
    return getComparedSize(displayOrientation, width, height,
        SizeComparator.MODE_LARGEST, parameters.getSupportedPictureSizes());
  }

  public static Camera.Size getClosestPictureSize(
      int displayOrientation, int width, int height, Camera.Parameters parameters
  ) {
    return getComparedSize(displayOrientation, width, height,
        SizeComparator.MODE_CLOSEST, parameters.getSupportedPictureSizes());
  }

  public static Camera.Size getBestAspectSize(
      int displayOrientation, int width, int height, List<Camera.Size> sizes
  ) {
    double targetRatio=(double)width / height;
    Camera.Size optimalSize=null;
    double minDiff=Double.MAX_VALUE;

    if (displayOrientation == 90 || displayOrientation == 270) {
      targetRatio=(double)height / width;
    }

    Collections.sort(sizes, Collections.reverseOrder(getComparator(
        displayOrientation, width, height, SizeComparator.MODE_LARGEST
    )));

    for (Size size : sizes) {
      double ratio=(double)size.width / size.height;

      if (Math.abs(ratio - targetRatio) < minDiff) {
        optimalSize=size;
        minDiff=Math.abs(ratio - targetRatio);
      }
    }

    return optimalSize;
  }

  public static Camera.Size getBestAspectPreviewSize(
      int displayOrientation, int width, int height, Camera.Parameters parameters
  ) {
    return getBestAspectSize(displayOrientation, width, height, parameters.getSupportedPreviewSizes());
  }

  public static Camera.Size getBestAspectPictureSize(
      int displayOrientation, int width, int height, Camera.Parameters parameters
  ) {
    return getBestAspectSize(displayOrientation, width, height, parameters.getSupportedPictureSizes());
  }

  public static Camera.Size getLargestPictureSize(CameraHost host,
                                                  Camera.Parameters parameters) {
    return(getLargestPictureSize(host, parameters, true));
  }

  public static Camera.Size getLargestPictureSize(CameraHost host,
                                                  Camera.Parameters parameters,
                                                  boolean enforceProfile) {
    Camera.Size result=null;

    for (Camera.Size size : parameters.getSupportedPictureSizes()) {

      // android.util.Log.d("CWAC-Camera",
      // String.format("%d x %d", size.width, size.height));

      if (!enforceProfile
          || (size.height <= host.getDeviceProfile()
                                 .getMaxPictureHeight() && size.height >= host.getDeviceProfile()
                                                                              .getMinPictureHeight())) {
        if (result == null) {
          result=size;
        }
        else {
          int resultArea=result.width * result.height;
          int newArea=size.width * size.height;

          if (newArea > resultArea) {
            result=size;
          }
        }
      }
    }

    if (result == null && enforceProfile) {
      result=getLargestPictureSize(host, parameters, false);
    }

    return(result);
  }

  public static Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
    Camera.Size result=null;

    for (Camera.Size size : parameters.getSupportedPictureSizes()) {
      if (result == null) {
        result=size;
      }
      else {
        int resultArea=result.width * result.height;
        int newArea=size.width * size.height;

        if (newArea < resultArea) {
          result=size;
        }
      }
    }

    return(result);
  }

  public static String findBestFlashModeMatch(Camera.Parameters params,
                                              String... modes) {
    String match=null;

    List<String> flashModes=params.getSupportedFlashModes();

    if (flashModes != null) {
      for (String mode : modes) {
        if (flashModes.contains(mode)) {
          match=mode;
          break;
        }
      }
    }

    return(match);
  }

  private static int calcArea(Size size) {
    return size.width * size.height;
  }

  private static double calcRatio(Size size) {
    return (double)size.width / size.height;
  }

  private static double diffRatio(double ratio, Size size) {
    return Math.abs(ratio - calcRatio(size));
  }

  private static class SizeComparator implements Comparator<Size> {
    public static final int MODE_LARGEST = 1;
    public static final int MODE_PREVIEW = 2;
    public static final int MODE_CLOSEST = 3;

    public final int mode;

    public final int width;
    public final int height;
    public final double ratio;

    public SizeComparator(int mode, int width, int height) {
      this.mode = mode;
      this.width = width;
      this.height = height;
      this.ratio = (double)width / height;
    }

    @Override
    public int compare(Size size1, Size size2) {
      if (mode == MODE_LARGEST) return compareLargest(size1, size2);
      if (mode == MODE_PREVIEW) return comparePreview(size1, size2);
      if (mode == MODE_CLOSEST) return compareClosest(size1, size2);
      return 0;
    }

    public int compareLargest(Size size1, Size size2) {
      boolean fitRatio1 = diffRatio(ratio, size1) <= ASPECT_TOLERANCE;
      boolean fitRatio2 = diffRatio(ratio, size2) <= ASPECT_TOLERANCE;

      if (fitRatio1 && !fitRatio2) return -1;
      if (fitRatio2 && !fitRatio1) return 1;

      int area1 = calcArea(size1);
      int area2 = calcArea(size2);

      return area2 - area1;
    }

    public int compareFit(Size size1, Size size2) {
      boolean fitSize1 = size1.width <= width && size1.height <= height;
      boolean fitSize2 = size2.width <= width && size2.height <= height;

      if (fitSize1 && !fitSize2) return -1;
      if (fitSize2 && !fitSize1) return 1;

      return 0;
    }

    public int comparePreview(Size size1, Size size2) {
      int compareFit = compareFit(size1, size2);
      if (compareFit != 0) return compareFit;

      return compareLargest(size1, size2);
    }

    public int compareClosest(Size size1, Size size2) {
      int diffWidth1 = Math.abs(width - size1.width);
      int diffWidth2 = Math.abs(width - size2.width);
      int diffHeight1 = Math.abs(height - size1.height);
      int diffHeight2 = Math.abs(height - size2.height);

      // Convoluted, but viable
      int diffSize1 = (height * diffWidth1) + (width * diffHeight1);
      int diffSize2 = (height * diffWidth2) + (width * diffHeight2);

      return diffSize1 - diffSize2;
    }
  }

  private static double calcRatio(int displayOrientation, int width, int height) {
    double targetRatio = (double)width / height;
    if (displayOrientation == 90 || displayOrientation == 270) {
      targetRatio = (double)height / width;
    }
    return targetRatio;
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private static SizeComparator getComparator(
      int displayOrientation, int width, int height, int mode
  ) {
    int normalizedWidth, normalizedHeight;
    if (displayOrientation == 90 || displayOrientation == 270) {
      normalizedWidth = height;
      normalizedHeight = width;
    } else {
      normalizedWidth = width;
      normalizedHeight = height;
    }
    return new SizeComparator(mode, normalizedWidth, normalizedHeight);
  }

  public static Camera.Size getComparedSize(
      int displayOrientation, int width, int height, int mode, List<Camera.Size> sizes
  ) {
    Collections.sort(sizes, getComparator(displayOrientation, width, height, mode));
    return sizes.get(0);
  }
}
