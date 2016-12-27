package com.example.oslab.handrsp.ImageProcessor;

import android.content.Context;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColorBlobDetector {
    Context main;
    ColorBlobDetector(Context main){
        this.main = main;
    }
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);   // Gray Color
    private Scalar mUpperBound = new Scalar(0);   // Gray Color
    // Minimum contour area in percent for contours filtering

    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space

    private Scalar mColorRadius = new Scalar(25,50,50, 0);
    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();   // Mat의 정보를 가르키는 포인터들의 모임

    // Cache
    Mat mPyrDownMat = new Mat();
    static Mat mHsvMat = new Mat();
    static Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();


    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;                   // 기준색보다
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }
        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);            // divide / 2
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);         // divide / 2

        /*
        * public static void pyrDown(Mat src, Mat dst)

            Blurs an image and downsamples it.
            The function performs the downsampling step of the Gaussian pyramid construction.
            First, it convolves the source image with the kernel:
            1/256 1 4 6 4 1 4 16 24 16 4 6 24 36 24 6 4 16 24 16 4 1 4 6 4 1
            Then, it downsamples the image by rejecting even rows and columns.

                Parameters:
                    src - input image.
                    dst - output image; it has the specified size and the same type as src.

           */

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);


        /*
        * public static void inRange(Mat src,
           Scalar lowerb,
           Scalar upperb,
           Mat dst)   Checks if array elements lie between the elements of two other
              arrays.
                The function checks the range as follows:
                For every element of a single-channel input array:
                dst(I)= lowerb(I)_0 <= src(I)_0 <= upperb(I)_0

                For two-channel arrays:
                dst(I)= lowerb(I)_0 <= src(I)_0 <= upperb(I)_0 land lowerb(I)_1 <= src(I)_1 <= upperb(I)_1

                and so forth.
                That is, dst (I) is set to 255 (all 1 -bits) if src (I) is within the specified 1D, 2D, 3D,... box and 0 otherwise.

                When the lower and/or upper boundary parameters are scalars, the indexes (I) at lowerb and upperb
                in the above formulas should be omitted.

                Parameters:
                 src - first         input array.
                 lowerb - inc        lusive lower boundary array or a scalar.
                 upperb - inc        lusive upper boundary array or a scalar.
                 dst - output        array of the same size as src and CV_8U type.*/

          Imgproc.dilate(mMask, mDilatedMask, new Mat());
       // Imgproc.erode(mMask, mDilatedMask, new Mat());
        /*dilate
            public static void dilate(Mat src,
                Mat dst,
                 Mat kernel)
            Dilates an image by using a specific structuring element.

            The function dilates the source image using the specified structuring element
            that determines the shape of a pixel neighborhood over which the maximum is taken:

            dst(x,y) = max _((x',y'): element(x',y') != 0) src(x+x',y+y')

            The function supports the in-place mode. Dilation can be applied several (iterations) times. I
            n case of multi-channel images, each channel is processed independently.*/

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        /*
        * void findContours(InputOutputArray image, OutputArrayOfArrays contours, int mode, int method, Point offset=Point())
        *   CV_RETR_EXTERNAL retrieves only the extreme outer contours. It sets hierarchy[i][2]=hierarchy[i][3]=-1 for all the contours.
            CV_RETR_LIST retrieves all of the contours without establishing any hierarchical relationships.
            CV_RETR_CCOMP retrieves all of the contours and organizes them into a two-level hierarchy. At the top level, there are external boundaries of the components. At the second level, there are boundaries of the holes. If there is another contour inside a hole of a connected component, it is still put at the top level.
            CV_RETR_TREE retrieves all of the contours and reconstructs a full hierarchy of nested contours. This full hierarchy is built and shown in the OpenCV contours.c demo.
            method –
            Contour approximation method (if you use Python see also a note below).

            CV_CHAIN_APPROX_NONE stores absolutely all the contour points. That is, any 2 subsequent points (x1,y1) and (x2,y2) of the contour will be either horizontal, vertical or diagonal neighbors, that is, max(abs(x1-x2),abs(y2-y1))==1.
            CV_CHAIN_APPROX_SIMPLE compresses horizontal, vertical, and diagonal segments and leaves only their end points. For example, an up-right rectangular contour is encoded with 4 points.
            ( 수평, 수직, 대각 성분의 끝점만 저장한다. )
            CV_CHAIN_APPROX_TC89_L1,CV_CHAIN_APPROX_TC89_KCOS applies one of the flavors of the Teh-Chin chain approximation algorithm. See [TehChin89] for details.*/


        // Find max contour area


        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            /*contourArea
            public static double contourArea(Mat contour)
                Calculates a contour area.
                The function computes a contour area. Similarly to "moments",
                the area is computed using the Green formula.
                Thus, the returned area and the number of non-zero pixels,
                if you draw the contour using "drawContours" or "fillPoly",
                can be different. Also, the function will most certainly give a wrong
                results for contours with self-intersections. Example: */

            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);           //원상 복구 ( 피라미드에서 줄인 값 )
                mContours.add(contour);
            }
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }

    public void imshow(Mat src){

    }
}