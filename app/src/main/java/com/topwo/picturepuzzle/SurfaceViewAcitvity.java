package com.topwo.picturepuzzle;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;


public class SurfaceViewAcitvity extends Activity implements SensorEventListener {

    /**
     * SensorManager管理器
     **/
    private SensorManager mSensorMgr = null;
    Sensor mSensor = null;

    MyView mAnimView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全屏显示窗口
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //强制横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        /**得到SensorManager对象**/
        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // 显示自定义的游戏View
        mAnimView = new MyView(this);
        setContentView(mAnimView);
    }

    @Override
    public void onPause(){
        super.onPause();

        mSensorMgr.unregisterListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();

        // 注册listener，第三个参数是检测的精确度
        //SENSOR_DELAY_FASTEST 最灵敏 因为太快了没必要使用
        //SENSOR_DELAY_GAME    游戏开发中使用
        //SENSOR_DELAY_NORMAL  正常速度
        //SENSOR_DELAY_UI 	       最慢的速度
        mSensorMgr.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor ,int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mAnimView.onSensorChanged(event);
    }

    public class MyView extends SurfaceView implements Callback, Runnable {

        /**
         * 每50帧刷新一次屏幕
         **/
        public static final int TIME_IN_FRAME = 50;

        /**
         * 游戏画笔
         **/
        Paint mPaint = null;
        Paint mPaintTran = null;
        Paint mTextPaint = null;
        SurfaceHolder mSurfaceHolder = null;

        /**
         * 控制游戏更新循环
         **/
        boolean mRunning = false;

        /**
         * 游戏画布
         **/
        Canvas mCanvas = null;

        /**
         * 控制游戏循环
         **/
        boolean mIsRunning = false;

        /**
         * 手机屏幕宽高
         **/
        int mScreenWidth = 0;
        int mScreenHeight = 0;

        /**
         * 小球资源文件越界区域
         **/
        private int mBallWidth = 0;
        private int mBallHeight = 0;

        /**
         * 小球资源文件越界区域
         **/
        private int mScreenBallWidth = 0;
        private int mScreenBallHeight = 0;

        /**
         * 游戏背景文件
         **/
        private Bitmap mbitmapBg;
//        private Bitmap transparentBitmap;

        /**
         * 小球资源文件
         **/
        private ImagePiece mImagePiece;

        private List<ImagePiece> mImagePieces;

        /**
         * 小球的坐标位置
         **/
        private float mPosX = 200;
        private float mPosY = 0;
        private float timeV = 0;
        private long timeVBefore = 0;
        /**
         * 重力感应X轴 Y轴 Z轴的重力值
         **/
        private float mGX = 0;
        private float mGY = 0;
        private float mGZ = 0;

        /**
         * 设置Item的数量n*n；默认为3
         */
        private int mColumn = 3;

        ImageSplitter mImageSplitter;

        public MyView(Context context) {
            super(context);
            mImagePieces = new ArrayList<ImagePiece>();
            /** 设置当前View拥有控制焦点 **/
            this.setFocusable(true);
            /** 设置当前View拥有触摸事件 **/
            this.setFocusableInTouchMode(true);
            this.setKeepScreenOn(true);
            /** 拿到SurfaceHolder对象 **/
            mSurfaceHolder = this.getHolder();
            /** 将mSurfaceHolder添加到Callback回调函数中 **/
            mSurfaceHolder.addCallback(this);
            /** 创建画布 **/
            mCanvas = new Canvas();
            /** 创建曲线画笔 **/
            mPaint = new Paint();
            mPaint.setColor(Color.WHITE);
            mPaintTran = new Paint();
            //清屏
            mPaintTran.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mPaintTran.setColor(Color.TRANSPARENT);
            /**加载游戏背景**/
            mbitmapBg = BitmapFactory.decodeResource(this.getResources(), R.mipmap.aa);

            /**裁切碎片**/
            mImageSplitter = new ImageSplitter();
            mImageSplitter.split(BitmapFactory.decodeResource(this.getResources(), R.mipmap.aa), mColumn);

            newPiece();

            /**得到小球宽高**/
            mBallWidth = mImagePiece.getWidth();
            mBallHeight = mImagePiece.getHeight();

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mScreenWidth = size.x;
            mScreenHeight = size.y;

            /**得到小球越界区域**/
            mScreenBallWidth = mScreenWidth - mBallWidth;
            mScreenBallHeight = mScreenHeight - mBallHeight;
        }

        private Bitmap createColorBitmap(int rgb, int width, int height) {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(rgb);
            return bmp;
        }


        private void beginDraw() {
            if(mPaintTran == null){
                mCanvas.drawPaint(mPaintTran);
            }
            mCanvas.drawPaint(mPaintTran);
            //**绘制游戏背景**//*
            mCanvas.drawBitmap(mbitmapBg, 0, 0, mPaint);

            for (int i = 0; i < mImagePieces.size(); ++i){
                ImagePiece imagePiece = mImagePieces.get(i);
                mCanvas.drawBitmap(imagePiece.bitmap, imagePiece.x, imagePiece.y, mPaint);
            }
            /**绘制↑次小球**/
//            mCanvas.drawBitmap(transparentBitmap, mPosXBefor, mPosYBefor, mPaintTran);
            /**绘制小球**/
            if(mImagePiece != null){
                Log.i("info", "------>x--->" + mPosX + "------>y--->" + mPosY);
                mCanvas.drawBitmap(mImagePiece.bitmap, mPosX, mPosY, mPaint);
            }
            /**X轴 Y轴 Z轴的重力值*
             mCanvas.drawText("X轴重力值 ：" + mGX, 0, 20, mPaint);
             mCanvas.drawText("Y轴重力值 ：" + mGY, 0, 40, mPaint);
             mCanvas.drawText("Z轴重力值 ：" + mGZ, 0, 60, mPaint);*/
            if(isPositionOK()){
                fixPiecePosition();
                newPiece();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            /**开始游戏主循环线程**/
            mIsRunning = true;
            new Thread(this).start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mIsRunning = false;
        }

        @Override
        public void run() {
            while (mIsRunning) {

                /** 取得更新游戏之前的时间 **/
//                long startTime = System.currentTimeMillis();

                /** 在这里加上线程安全锁 **/
                synchronized (mSurfaceHolder) {

                    try {
                        /** 拿到当前画布 然后锁定 **/
                        mCanvas = mSurfaceHolder.lockCanvas();
                        if (mCanvas != null) {
                            beginDraw();
                        }
                    } catch (Exception e) {
                        Log.v("Himi", "draw is Error!");
                    } finally {//备注1
                        if (mCanvas != null)//备注2
                            /** 绘制结束后解锁显示在屏幕上 **/
                            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                    }
                }

                /** 取得更新游戏结束的时间 **/
//                long endTime = System.currentTimeMillis();

                /** 计算出游戏一次更新的毫秒数 **/
//                int diffTime = (int) (endTime - startTime);

               /* *//** 确保每次更新时间为50帧 **//*
                while (diffTime <= TIME_IN_FRAME) {
                    diffTime = (int) (System.currentTimeMillis() - startTime);
                    *//** 线程等待 **//*
                    Thread.yield();
                }*/

            }

        }

        public void onSensorChanged(SensorEvent event) {
            if (timeVBefore == 0) {
                timeVBefore = System.currentTimeMillis() - 1;
            }
            timeV = 1 + (System.currentTimeMillis() - timeVBefore) / 1000f;
            Configuration cf = this.getResources().getConfiguration();
            int ori = cf.orientation;
            if (ori == cf.ORIENTATION_LANDSCAPE) {// 横屏
                mGX = event.values[SensorManager.DATA_Y];
                mGY = event.values[SensorManager.DATA_X];
            } else if (ori == cf.ORIENTATION_PORTRAIT) {// 竖屏
                mGX = event.values[SensorManager.DATA_X];
                mGY = event.values[SensorManager.DATA_Y];
            }

            mGZ = event.values[SensorManager.DATA_Z];

            //这里乘以2是为了让小球移动的更快
            mPosX += mGX * (Math.abs(mGX) * 3);
            mPosY += mGY * (Math.abs(mGY) * 3);
            //检测小球是否超出边界
            if (mPosX < 0) {
                mPosX = 0;
            } else if (mPosX > mScreenBallWidth) {
                mPosX = mScreenBallWidth;
            }
            if (mPosY < 0) {
                mPosY = 0;
            } else if (mPosY > mScreenBallHeight) {
                mPosY = mScreenBallHeight;
            }
        }

        //判断位置是否达标
        public boolean isPositionOK(){
            if(mImagePiece == null){
                return false;
            }
            if(mPosX >= mImagePiece.x - 5 && mPosX <= mImagePiece.x + 5 && mPosY >= mImagePiece.y - 5 && mPosY <= mImagePiece.y + 5){
                mImagePieces.add(mImagePiece);
                mImagePiece = null;
                return true;
            }
            return false;
        }

        //把碎片位置矫正
        public void fixPiecePosition(){

        }

        //生成新的碎片
        public void newPiece(){
            /**加载小球资源**/
            mImagePiece = mImageSplitter.getImagePiece();
//            transparentBitmap = createColorBitmap(R.color.transparent, mbitmapBall.getWidth(), mbitmapBall.getHeight());//BitmapFactory.decodeResource(this.getResources(), R.drawable.transparent);
        }
    }
}