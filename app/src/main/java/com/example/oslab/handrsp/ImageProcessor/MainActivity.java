package com.example.oslab.handrsp.ImageProcessor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oslab.handrsp.CustomSurfaceView;
import com.example.oslab.handrsp.R;
import com.example.oslab.handrsp.databases.DbMainActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }
    public static final int        JAVA_DETECTOR       = 0;

    private Mat                    mRgba;
    private Mat                    mGray;

    private CustomSurfaceView mOpenCvCameraView;

    private SeekBar minTresholdSeekbar = null;
    private TextView minTresholdSeekbarText = null;
    private ImageView numberOfFingersView = null;

    double iThreshold = 0;

    private Scalar               	mBlobColorHsv;
    private Scalar               	mBlobColorRgba;
    private ColorBlobDetector mDetector;
    private Mat                  	mSpectrum;
    private boolean				mIsColorSelected = false;
    private Size                 	SPECTRUM_SIZE;       //double height, width
    private Scalar               	CONTOUR_COLOR;
    private Scalar               	CONTOUR_COLOR_WHITE;
    private Scalar                 BizonBlack;

    final Handler mHandler = new Handler();
    final Runnable mUpdateFingerCountResults = new Runnable() {
        public void run() {
            updateNumberOfFingers();
        }
    };
    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Mat test = ColorBlobDetector.mMask.clone();
            ImageView imageView = (ImageView)findViewById(R.id.imageView2);
            //Imgproc.cvtColor(ColorBlobDetector.mHsvMat, test, Imgproc.COLOR_HSV2BGR);
            Imgproc.cvtColor(test, test, Imgproc.COLOR_GRAY2RGBA, 4);
            Bitmap bm = Bitmap.createBitmap(test.cols(), test.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(test, bm);
            imageView.setImageBitmap(bm);
        }
    };

    int numberOfFingers = 0;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("open", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    // 640x480
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i("open", "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("onCreate", "called onCreate");

        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main_surface_view);

        if (!OpenCVLoader.initDebug()) {
            Log.d("onCreate","initDebug");
        }else{

        }

        Button confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DbMainActivity.class);
                intent.putExtra("finger", numberOfFingers);
                startActivity(intent);
            }
        });

        mOpenCvCameraView = (CustomSurfaceView) findViewById(R.id.main_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        minTresholdSeekbarText = (TextView) findViewById(R.id.textView3);

        numberOfFingersView = (ImageView) findViewById(R.id.numberOfFingers);

        minTresholdSeekbar = (SeekBar)findViewById(R.id.seekBar1);
        minTresholdSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
            }
        });
        minTresholdSeekbar.setProgress(8700);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();

        Camera.Size resolution = mOpenCvCameraView.getResolution();
        String caption = "Resolution "+ Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
        Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();

        Camera.Parameters cParams = mOpenCvCameraView.getParameters();
        cParams.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        mOpenCvCameraView.setParameters(cParams);

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        //카메라 화면 크기만큼을 Mat에 저장
        /*CV_8UC(n) (n channel array with 8 bit unsigned integers (n can be from 1 to 512) )
                                                               It means 4 channel ( R G B ALPHA ) Matrix exist
                                                                Each Channels has Matrix    (n * m) * 4

                                                             */
        mDetector = new ColorBlobDetector(this);
        mSpectrum = new Mat();
        BizonBlack = new Scalar(255, 255, 255);
        mBlobColorRgba = new Scalar(255);               //Black
        mBlobColorHsv = new Scalar(255);                //Black
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(0,255,0,0);          //GREEN
        CONTOUR_COLOR_WHITE = new Scalar(255,255,255,255);      //RGBA, WHITE

    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();        //카메라 뷰 y1280
        int rows = mRgba.rows();        //카메라 뷰 x720

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;          // (1920 - 1280) /2 = 320
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;               //142.5

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.d("onTouch", "Touch image coordinates: (" + x + ", " + y + ")");

        if (    (x < 0) ||
                (y < 0) ||
                (x > cols) ||
                (y > rows))     return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>5) ? x-5 : 0;            /*터치한 지점을 감싸기 위한 좌표*/
        touchedRect.y = (y>5) ? y-5 : 0;
        Log.d("touched", Integer.toString(touchedRect.x));
        Log.d("touched", Integer.toString(touchedRect.y));

        touchedRect.width = (x+5 < cols) ? x + 5 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+5 < rows) ? y + 5 - touchedRect.y : rows - touchedRect.y;

        Log.d("touched", "width" + Integer.toString(touchedRect.width));
        Log.d("touched", "height" + Integer.toString(touchedRect.height));

        Mat touchedRegionRgba = mRgba.submat(touchedRect);  //직사각형 서브매트릭스 추출
        /*submat
        public Mat submat(Rect roi)
            Extracts a rectangular submatrix.

                The operators make a new header for the specified sub-array of *this.
                They are the most generalized forms of "Mat.row", "Mat.col", "Mat.rowRange",
                and "Mat.colRange". For example, A(Range(0, 10), Range.all()) is equivalent to A.rowRange(0, 10).
                Similarly to all of the above, the operators are O(1) operations, that is, no matrix data is copied.

            Parameters:
            roi - Extracted submatrix specified as a rectangle.*/

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);      //RGBA -> HSV

        /*
        void CvtColor(InputArray, OutputArray, int code int dstcn = 0)
                    inputArray   : input image
                    outputArray  : output image
                    code         : Color Space Conversion Code
                    dstcn        : number of channels in the destion image

          This Method convert RGBA color TO HSV color
         */

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);        //sum of array elements
        int pointCount = touchedRect.width*touchedRect.height;      //Rectangle(Detected range) of FullSize
        for (int i = 0; i < mBlobColorHsv.val.length; i++) {
            mBlobColorHsv.val[i] /= pointCount;
            Log.d("mbob", Double.toString(mBlobColorHsv.val[i]));
        }

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i("onTouch", "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();


        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);         //4개의 채널로로


       return new Scalar(pointMatRgba.get(0, 0));
    }
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        ImageView imageView = (ImageView)findViewById(R.id.imageView2);
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        iThreshold = minTresholdSeekbar.getProgress();  //탐색 범위 설정 ( 0   << 범위 >> 30000 )
                                                                            //많이            //적게
        //Imgproc.blur(mRgba, mRgba, new Size(5,5));
        Imgproc.GaussianBlur(mRgba, mRgba, new org.opencv.core.Size(3, 3), 1, 1);       // 3* 3 Ksize 커널을 적용한 가우시안 블러
        /*이미지에 잡음을 제거하여 영상처리에 용이하도록 셋팅한다. */
        /*KERNEL : 1/16 1/8 1/16
                   1/8  1/4 1/8
                   1/16 1/8 1/16
        */
        /*public static void GaussianBlur(
                Mat src,
                Mat dst,
                Size ksize,
                double sigmaX,
                double sigmaY,
                int borderType)

                Blurs an image using a Gaussian filter.

                The function convolves the source image with the specified Gaussian kernel. In-place filtering is supported.*/
        if (!mIsColorSelected) return mRgba;

        //List<MatOfPoint> contours = mDetector.getContours();
        mDetector.process(mRgba);

        List<MatOfPoint> contours = mDetector.getContours();

        Imgproc.drawContours(mRgba, contours, -1, new Scalar(255, 0, 0, 0));
        new Thread()

        {

            public void run()

            {

                Message msg = handler.obtainMessage();

                handler.sendMessage(msg);

            }

        }.start();

        Log.d("onCameraFrame", "Contours count: " + contours.size());

        if (contours.size() <= 0) {
            return mRgba;
        }

        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0)	.toArray()));

        /*
        * public static RotatedRect minAreaRect(MatOfPoint2f points)
                Finds a rotated rectangle of the minimum area enclosing the input 2D point set.

                   The function calculates and returns the minimum-area bounding rectangle
                   (possibly rotated) for a specified point set. See the OpenCV sample minarea.cpp.
                   Developer should keep in mind that the returned rotatedRect
                   can contain negative indices when data is close the the containing Mat element boundary.*/

        double boundWidth = rect.size.width;
        double boundHeight = rect.size.height;
        int boundPos = 0;

        for (int i = 1; i < contours.size(); i++) {
            rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
            if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
                boundWidth = rect.size.width;
                boundHeight = rect.size.height;
                boundPos = i;
            }
        }

        Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));

       // Imgproc.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0 );


        Log.d("onCameraFrame",
                " Row start ["+
                        (int) boundRect.tl().y + "] row end ["+
                        (int) boundRect.br().y+"] Col start ["+
                        (int) boundRect.tl().x+"] Col end ["+
                        (int) boundRect.br().x+"]");

        double a = boundRect.br().y - boundRect.tl().y;
        a = a * 0.7;
        a = boundRect.tl().y + a;

        Log.d("onCameraFrame",
                " A ["+a+"] br y - tl y = ["+(boundRect.br().y - boundRect.tl().y)+"]");

        //Core.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR, 2, 8, 0 );
       // Imgproc.rectangle( mRgba, boundRect.tl(), new Point(boundRect.br().x, a), CONTOUR_COLOR, 2, 8, 0 );

        MatOfPoint2f pointMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
        /*void approxPolyDP(InputArray curve, OutputArray approxCurve, double epsilon, bool closed)
        * Input vector of a 2D point stored in MatOfPoint*/

        contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

        MatOfInt hull = new MatOfInt();
        MatOfInt4 convexDefect = new MatOfInt4();

        Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);
        /*
        * void convexHull(InputArray points, OutputArray hull, bool clockwise=false, bool returnPoints=true )*/

        if(hull.toArray().length < 3) return mRgba;

        Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos)	.toArray()), hull, convexDefect);
        /*void convexityDefects(InputArray contour, InputArray convexhull, OutputArray convexityDefects)*/

        List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
        List<Point> listPo = new LinkedList<Point>();
        for (int j = 0; j < hull.toList().size(); j++) {
            listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
        }
        MatOfPoint e = new MatOfPoint();
        e.fromList(listPo);
        hullPoints.add(e);

        List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
        List<Point> listPoDefect = new LinkedList<Point>();
        for (int j = 0; j < convexDefect.toList().size(); j = j+4) {
            Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j+2));
            Integer depth = convexDefect.toList().get(j+3);
            if(depth > iThreshold && farPoint.y < a){
                listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j+2)));
            }
        }

        MatOfPoint e2 = new MatOfPoint();
        e2.fromList(listPo);
        defectPoints.add(e2);

        Log.d("onCameraFrame", "hull: " + hull.toList());
        Log.d("onCameraFrame", "defects: " + convexDefect.toList());

        Imgproc.drawContours(mRgba, hullPoints, -1, CONTOUR_COLOR, 3);

        int defectsTotal = (int) convexDefect.total();
        Log.d("onCameraFrame", "Defect total " + defectsTotal);

        this.numberOfFingers = listPoDefect.size();
        if(this.numberOfFingers > 5) this.numberOfFingers = 5;

        mHandler.post(mUpdateFingerCountResults);

        for(Point p : listPoDefect){
            Imgproc.circle(mRgba, p, 6, BizonBlack);
        }

        return mRgba;
    }

    public void updateNumberOfFingers(){
        if(numberOfFingers >= 0 && numberOfFingers <= 1){
            this.numberOfFingers = 0;
            numberOfFingersView.setImageResource(R.drawable.camera_rock);
        }
        else if(numberOfFingers >= 2 && numberOfFingers <= 3){
            this.numberOfFingers = 2;
            numberOfFingersView.setImageResource(R.drawable.camera_scissors);
        }
        else{
            this.numberOfFingers = 5;
            numberOfFingersView.setImageResource(R.drawable.camera_paper);
        }
    }


}