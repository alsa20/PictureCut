package com.alsa.picturecut.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

/**
 * PictureCutView [ 实现剪裁功能的自定义View ]
 * created by alsa on 2019/12/12
 */
public class PictureCutView extends View {
    /**
     * 画笔
     */
    private Paint mPaint;

    /**
     * 绘制的图像
     */
    private Bitmap mBitmap;

    /**
     * 屏幕可用宽高
     * 可用宽 = 屏幕宽
     * 可用高 = 除底部按钮导航栏外的屏幕高 - 状态栏高度 - 标题栏高度
     */
    private float mAvailableScreenWidth;
    private float mAvailableScreenHeight;

    /**
     * 图片绘制的起始x,y坐标
     * x = （可用宽 - 图片宽）/ 2
     * y = ( 可用高 - 图片高）/ 2
     */
    private float mDrawBitmapStartX;
    private float mDrawBitmapStartY;

    /**
     * 图像的左、上、右、下值
     */
    private float mBitmapLeft;
    private float mBitmapTop;
    private float mBitmapRight;
    private float mBitmapBottom;

    /**
     * 九宫格每个单元格的宽高
     */
    private float mRectWidth;
    private float mRectHeight;

    /**
     * 手指触摸九宫格的位置标识值
     */
    private static final int LEFT_TOP_CORNER = 1;
    private static final int RIGHT_TOP_CORNER = 2;
    private static final int RIGHT_BOTTOM_CORNER = 3;
    private static final int LEFT_BOTTOM_CORNER = 4;
    private static final int LEFT_BORDER = 5;
    private static final int TOP_BORDER = 6;
    private static final int RIGHT_BORDER = 7;
    private static final int BOTTOM_BORDER = 8;
    private static final int CENTER = 9;

    /**
     * 手指触摸区域的标识
     */
    private int mTouchFlag;

    /**
     * 移动后绘制九宫格的起始、结束X，Y值
     */
    private float mCutStartX;
    private float mCutStartY;
    private float mCutStopX;
    private float mCutStopY;

    /**
     * 移动后九宫格的宽度，高度
     */
    private float mCutWidth;
    private float mCutHeight;

    /**
     * 九宫格的最小宽度和高度
     */
    private static final int MIN_MASK_WIDTH_HEIGHT = 300;

    /**
     * 手指可触摸区域的左、上、右、下值
     */
    float mTouchLeft;
    float mTouchTop;
    float mTouchRight;
    float mTouchBottom;

    /**
     * 手指触摸到九宫格中心区域时的坐标
     */
    private float mLastEventX;
    private float mLastEventY;

    /**
     * Activity通知View剪裁图像的Flag
     */
    private int mCutFlag;

    public PictureCutView(Context context) {
        this(context, null);
    }

    public PictureCutView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PictureCutView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * activity设置图片路径
     *
     * @param photoPath 图片路径
     */
    public void setPhotoPath(String photoPath) {
        mBitmap = BitmapFactory.decodeFile(photoPath);
    }

    /**
     * Activity设置标题栏的高度
     *
     * @param height 标题栏的高度
     */
    public void setActionBarHeight(int height) {
        scaleBitmap(getContext(), height);
        calculateBitmapPos();
        invalidate();
    }

    /**
     * 外部调用接口，Activity通知View剪裁图像
     *
     * @param flag >0
     */
    public void cutPicure(int flag) {
        mCutFlag = flag;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, mDrawBitmapStartX, mDrawBitmapStartY, mPaint);
        // 绘制九宫格
        if (mTouchFlag == 0) {
            mCutHeight = mBitmap.getHeight();
            mCutWidth = mBitmap.getWidth();
            mCutStartX = mDrawBitmapStartX;
            mCutStartY = mDrawBitmapStartY;
            mCutStopX = mDrawBitmapStartX + mCutWidth;
            mCutStopY = mDrawBitmapStartY + mCutHeight;
        }
        drawMask(canvas);
        if (mCutFlag != 0) {
            // 清除屏幕原有图像
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            // 裁剪图像
            canvas.clipRect(mCutStartX, mCutStartY, mCutStopX, mCutStopY);
            // 绘制图像
            canvas.drawBitmap(mBitmap, mDrawBitmapStartX, mDrawBitmapStartY, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 获取手指的触摸区域
            mTouchFlag = getTouchFlag(event);
            if (mTouchFlag == CENTER) {
                mLastEventX = event.getX();
                mLastEventY = event.getY();
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // 获取手指移动点的X,Y值
            float eventX = event.getX();
            float eventY = event.getY();

            // 改变九宫格的大小和位置
            switch (mTouchFlag) {
                case LEFT_BORDER:
                    // 超出图像左边界
                    if (eventX <= mBitmapLeft) {
                        mCutWidth = mCutStopX - mBitmapLeft;
                        mCutStartX = mBitmapLeft;
                    }
                    // 达到最小尺寸
                    if (eventX >= mCutStopX - MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = MIN_MASK_WIDTH_HEIGHT;
                        mCutStartX = mCutStopX - MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 在图像左边界和最小尺寸之间移动
                    if (eventX > mBitmapLeft && eventX < mCutStopX - MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = mCutStopX - eventX;
                        mCutStartX = eventX;
                    }
                    break;
                case TOP_BORDER:
                    // 超出图像上边界
                    if (eventY <= mBitmapTop) {
                        mCutHeight = mCutStopY - mBitmapTop;
                        mCutStartY = mBitmapTop;
                    }
                    // 达到最小尺寸
                    if (eventY >= mCutStopY - MIN_MASK_WIDTH_HEIGHT) {
                        mCutHeight = MIN_MASK_WIDTH_HEIGHT;
                        mCutStartY = mCutStopY - MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 在图像上边界和最小尺寸之间移动
                    if (eventY > mBitmapTop && eventY < mCutStopY - MIN_MASK_WIDTH_HEIGHT) {
                        mCutHeight = mCutStopY - eventY;
                        mCutStartY = eventY;
                    }
                    break;
                case RIGHT_BORDER:
                    // 超出图像右边界
                    if (eventX >= mBitmapRight) {
                        mCutWidth = mBitmap.getWidth() - mCutStartX;
                        mCutStopX = mBitmapRight;
                    }
                    // 达到最小尺寸
                    if (eventX <= mCutStartX + MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = MIN_MASK_WIDTH_HEIGHT;
                        mCutStopX = mCutStartX + MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 在图像右边界和最小尺寸之间移动
                    if (eventX < mBitmapRight && eventX > mCutStartX + MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = eventX - mCutStartX;
                        mCutStopX = eventX;
                    }
                    break;
                case BOTTOM_BORDER:
                    // 超出图像下边界
                    if (eventY >= mBitmapBottom) {
                        mCutHeight = mBitmap.getHeight() - mCutStartY;
                        mCutStopY = mBitmapBottom;
                    }
                    // 达到最小尺寸
                    if (eventY <= mCutStartY + MIN_MASK_WIDTH_HEIGHT) {
                        mCutHeight = MIN_MASK_WIDTH_HEIGHT;
                        mCutStopY = mCutStartY + MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 在图像下边界和最小尺寸之间移动
                    if (eventY < mBitmapBottom && eventY > mCutStartY + MIN_MASK_WIDTH_HEIGHT) {
                        mCutHeight = eventY - mCutStartY;
                        mCutStopY = eventY;
                    }
                    break;
                case LEFT_TOP_CORNER:
                    // 手指的X值超出图像左侧边界
                    if (eventX <= mBitmapLeft) {
                        mCutWidth = mCutStopX - mBitmapLeft;
                        mCutStartX = mBitmapLeft;
                    } else if (eventX >= mCutStopX - MIN_MASK_WIDTH_HEIGHT) { // 手指的X值达到最小尺寸
                        mCutWidth = MIN_MASK_WIDTH_HEIGHT;
                        mCutStartX = mCutStopX - MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指的Y值超出图像顶部边界
                    if (eventY <= mBitmapTop) {
                        mCutHeight = mCutStopY - mBitmapTop;
                        mCutStartY = mBitmapTop;
                    } else if (eventY >= mCutStopY - MIN_MASK_WIDTH_HEIGHT) { // 手指的Y值达到最小尺寸
                        mCutHeight = MIN_MASK_WIDTH_HEIGHT;
                        mCutStartY = mCutStopY - MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指在图像边界和最小尺寸之间移动
                    if (eventX > mBitmapLeft && eventX < mCutStopX - MIN_MASK_WIDTH_HEIGHT
                            && eventY > mBitmapTop && eventY < mCutStopY - MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = mCutStopX - eventX;
                        mCutHeight = mCutStopY - eventY;
                        mCutStartX = eventX;
                        mCutStartY = eventY;
                    }
                    break;
                case RIGHT_TOP_CORNER:
                    // 手指的X值超出图像右侧边界
                    if (eventX >= mBitmapRight) {
                        mCutWidth = mBitmap.getWidth() - mCutStartX;
                        mCutStopX = mBitmapRight;
                    } else if (eventX <= mCutStartX + MIN_MASK_WIDTH_HEIGHT) { // 手指的X值达到最小尺寸
                        mCutWidth = MIN_MASK_WIDTH_HEIGHT;
                        mCutStopX = mCutStartX + MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指的Y值超出图像顶部边界
                    if (eventY <= mBitmapTop) {
                        mCutHeight = mCutStopY - mBitmapTop;
                        mCutStartY = mBitmapTop;
                    } else if (eventY >= mCutStopY - MIN_MASK_WIDTH_HEIGHT) { // 手指的Y值达到最小尺寸
                        mCutHeight = MIN_MASK_WIDTH_HEIGHT;
                        mCutStartY = mCutStopY - MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指在图像边界和最小尺寸之间移动
                    if (eventX > mCutStartX + MIN_MASK_WIDTH_HEIGHT && eventX < mBitmapRight
                            && eventY > mBitmapTop && eventY < mCutStopY - MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = eventX - mCutStartX;
                        mCutHeight = mCutStopY - eventY;
                        mCutStopX = eventX;
                        mCutStartY = eventY;
                    }
                    break;
                case LEFT_BOTTOM_CORNER:
                    // 手指的X值超出图像左侧边界
                    if (eventX <= mBitmapLeft) {
                        mCutWidth = mCutStopX - mBitmapLeft;
                        mCutStartX = mBitmapLeft;
                    } else if (eventX >= mCutStopX - MIN_MASK_WIDTH_HEIGHT) { // 手指的X值达到最小尺寸
                        mCutWidth = MIN_MASK_WIDTH_HEIGHT;
                        mCutStartX = mCutStopX - MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指的Y值超出图像底部边界
                    if (eventY >= mBitmapBottom) {
                        mCutHeight = mBitmapBottom - mCutStartY;
                        mCutStopY = mBitmapBottom;
                    } else if (eventY <= mCutStartY + MIN_MASK_WIDTH_HEIGHT) { // 手指的Y值达到最小尺寸
                        mCutHeight = MIN_MASK_WIDTH_HEIGHT;
                        mCutStopY = mCutStartY + MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指在图像边界和最小尺寸之间移动
                    if (eventX > mBitmapLeft && eventX < mCutStopX - MIN_MASK_WIDTH_HEIGHT
                            && eventY < mBitmapBottom && eventY > mCutStartY + MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = mCutStopX - eventX;
                        mCutHeight = eventY - mCutStartY;
                        mCutStartX = eventX;
                        mCutStopY = eventY;
                    }
                    break;
                case RIGHT_BOTTOM_CORNER:
                    // 手指的X值超出图像右侧边界
                    if (eventX >= mBitmapRight) {
                        mCutWidth = mBitmap.getWidth() - mCutStartX;
                        mCutStopX = mBitmapRight;
                    } else if (eventX <= mCutStartX + MIN_MASK_WIDTH_HEIGHT) { // 手指的X值达到最小尺寸
                        mCutWidth = MIN_MASK_WIDTH_HEIGHT;
                        mCutStopX = mCutStartX + MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指的Y值超出图像底部边界
                    if (eventY >= mBitmapBottom) {
                        mCutHeight = mBitmapBottom - mCutStartY;
                        mCutStopY = mBitmapBottom;
                    } else if (eventY <= mCutStartY + MIN_MASK_WIDTH_HEIGHT) { // 手指的Y值达到最小尺寸
                        mCutHeight = MIN_MASK_WIDTH_HEIGHT;
                        mCutStopY = mCutStartY + MIN_MASK_WIDTH_HEIGHT;
                    }
                    // 手指在图像边界和最小尺寸之间移动
                    if (eventX > mCutStartX + MIN_MASK_WIDTH_HEIGHT && eventX < mBitmapRight
                            && eventY < mBitmapBottom && eventY > mCutStartY + MIN_MASK_WIDTH_HEIGHT) {
                        mCutWidth = eventX - mCutStartX;
                        mCutHeight = eventY - mCutStartY;
                        mCutStopX = eventX;
                        mCutStopY = eventY;
                    }
                    break;
                case CENTER:
                    if (mCutStartX + eventX - mLastEventX <= mBitmapLeft) {  // 到达左边界
                        mCutStartX = mBitmapLeft;
                        mCutStopX = mBitmapLeft + mCutWidth;
                    } else if (mCutStopX + eventX - mLastEventX >= mBitmapRight) {  // 到达右边界
                        mCutStartX = mBitmapRight - mCutWidth;
                        mCutStopX = mBitmapRight;
                    } else {
                        mCutStartX += eventX - mLastEventX;
                        mCutStopX += eventX - mLastEventX;
                    }
                    if (mCutStartY + eventY - mLastEventY <= mBitmapTop) {  // 到达上边界
                        mCutStartY = mBitmapTop;
                        mCutStopY = mBitmapTop + mCutHeight;
                    } else if (mCutStopY + eventY - mLastEventY >= mBitmapBottom) {  // 到达下边界
                        mCutStartY = mBitmapBottom - mCutHeight;
                        mCutStopY = mBitmapBottom;
                    } else {
                        mCutStartY += eventY - mLastEventY;
                        mCutStopY += eventY - mLastEventY;
                    }
                    mLastEventX = eventX;
                    mLastEventY = eventY;
                    break;
            }
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    /**
     * 获取触摸区域类型
     *
     * @param event 事件
     * @return -1~9
     */
    private int getTouchFlag(MotionEvent event) {
        // 计算触摸区域的x,y值
        float touchX = event.getX();
        float touchY = event.getY();
        if (mTouchFlag == 0) {
            mTouchLeft = mBitmapLeft;
            mTouchTop = mBitmapTop;
            mTouchRight = mBitmapRight;
            mTouchBottom = mBitmapBottom;
        } else {
            mTouchLeft = mCutStartX;
            mTouchTop = mCutStartY;
            // 此时获取上次绘制后的单元格宽高，计算right和bottom
            mTouchRight = mCutStartX + 3 * mRectWidth;
            mTouchBottom = mCutStartY + 3 * mRectHeight;
        }
        // 计算手指触摸的位置
        if (touchX >= mTouchLeft && touchX <= mTouchLeft + 50 && touchY >= mTouchTop && touchY <= mTouchTop + 50) { // 左上角
            return LEFT_TOP_CORNER;
        }
        if (touchX >= mTouchLeft && touchX <= mTouchLeft + 50 && touchY >= mTouchBottom - 50 && touchY <= mTouchBottom) { // 左下角
            return LEFT_BOTTOM_CORNER;
        }
        if (touchX >= mTouchRight - 50 && touchX <= mTouchRight && touchY >= mTouchTop && touchY <= mTouchTop + 50) { // 右上角
            return RIGHT_TOP_CORNER;
        }
        if (touchX >= mTouchRight - 50 && touchX <= mTouchRight && touchY >= mTouchBottom - 50 && touchY <= mTouchBottom) { // 右下角
            return RIGHT_BOTTOM_CORNER;
        }
        if (touchX >= mTouchLeft + 50 && touchX <= mTouchRight - 50 && touchY >= mTouchTop && touchY <= mTouchTop + 50) { // 上边线
            return TOP_BORDER;
        }
        if (touchX >= mTouchLeft + 50 && touchX <= mTouchRight - 50 && touchY >= mTouchBottom - 50 && touchY <= mTouchBottom) { // 下边线
            return BOTTOM_BORDER;
        }
        if (touchX >= mTouchLeft && touchX <= mTouchLeft + 50 && touchY >= mTouchTop + 50 && touchY <= mTouchBottom - 50) { // 左边线
            return LEFT_BORDER;
        }
        if (touchX >= mTouchRight - 50 && touchX <= mTouchRight && touchY >= mTouchTop + 50 && touchY <= mTouchBottom - 50) { // 右边线
            return RIGHT_BORDER;
        }
        if (touchX >= mTouchLeft + 50 && touchX <= mTouchRight - 50 && touchY >= mTouchTop + 50 && touchY <= mTouchBottom - 50) {
            return CENTER;
        }
        // 在上述边界外，则返回-1
        return -1;
    }

    /**
     * 绘制九宫格
     *
     * @param canvas 画布
     */
    private void drawMask(Canvas canvas) {
        // 计算每个单元格的宽高
        mRectWidth = mCutWidth / 3;
        mRectHeight = mCutHeight / 3;

        canvas.save();
        // 绘制九宫格
        mPaint.setStrokeWidth(1);
        for (int i = 0; i < 4; i++) {
            // 竖线
            canvas.drawLine(mCutStartX + mRectWidth * i, mCutStartY, mCutStartX + mRectWidth * i, mCutStartY + mCutHeight, mPaint);
            // 横线
            canvas.drawLine(mCutStartX, mCutStartY + mRectHeight * i, mCutStartX + mCutWidth, mCutStartY + mRectHeight * i, mPaint);
        }

        mPaint.setStrokeWidth(4);
        // 左上角边角
        canvas.drawLine(mCutStartX, mCutStartY, mCutStartX + 50, mCutStartY, mPaint);
        canvas.drawLine(mCutStartX, mCutStartY, mCutStartX, mCutStartY + 50, mPaint);
        // 右上角边角
        canvas.drawLine(mCutStartX + mCutWidth - 50, mCutStartY, mCutStartX + mCutWidth, mCutStartY, mPaint);
        canvas.drawLine(mCutStartX + mCutWidth, mCutStartY, mCutStartX + mCutWidth, mCutStartY + 50, mPaint);
        // 左下角边角
        canvas.drawLine(mCutStartX, mCutStartY + mCutHeight, mCutStartX + 50, mCutStartY + mCutHeight, mPaint);
        canvas.drawLine(mCutStartX, mCutStartY + mCutHeight, mCutStartX, mCutStartY + mCutHeight - 50, mPaint);
        // 右下角边角
        canvas.drawLine(mCutStartX + mCutWidth - 50, mCutStartY + mCutHeight, mCutStartX + mCutWidth, mCutStartY + mCutHeight, mPaint);
        canvas.drawLine(mCutStartX + mCutWidth, mCutStartY + mCutHeight - 50, mCutStartX + mCutWidth, mCutStartY + mCutHeight, mPaint);
        // 顶部边线
        canvas.drawLine(mCutStartX + mCutWidth / 2 - 25, mCutStartY, mCutStartX + mCutWidth / 2 + 25, mCutStartY, mPaint);
        // 底部边线
        canvas.drawLine(mCutStartX + mCutWidth / 2 - 25, mCutStartY + mCutHeight, mCutStartX + mCutWidth / 2 + 25, mCutStartY + mCutHeight, mPaint);
        // 左部边线
        canvas.drawLine(mCutStartX, mCutStartY + mCutHeight / 2 - 25, mCutStartX, mCutStartY + mCutHeight / 2 + 25, mPaint);
        // 右部边线
        canvas.drawLine(mCutStartX + mCutWidth, mCutStartY + mCutHeight / 2 - 25, mCutStartX + mCutWidth, mCutStartY + mCutHeight / 2 + 25, mPaint);
        canvas.restore();
    }

    /**
     * 计算绘制图像的起始位置及left、top、right、bottom值
     */
    private void calculateBitmapPos() {
        // 计算绘制图片的起始x，y值
        mDrawBitmapStartX = (mAvailableScreenWidth - mBitmap.getWidth()) / 2;
        mDrawBitmapStartY = (mAvailableScreenHeight - mBitmap.getHeight()) / 2;

        // 计算图片的left/top/right/bottom值
        mBitmapLeft = mDrawBitmapStartX;
        mBitmapTop = mDrawBitmapStartY;
        mBitmapRight = mDrawBitmapStartX + mBitmap.getWidth();
        mBitmapBottom = mDrawBitmapStartY + mBitmap.getHeight();
    }

    /**
     * 缩放处理图片，使之充满整个View|宽铺满或高铺满
     *
     * @param context context
     */
    private void scaleBitmap(Context context, int height) {
        // 计算屏幕可用宽高
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            mAvailableScreenWidth = metrics.widthPixels;
            mAvailableScreenHeight = metrics.heightPixels - getStatusBarHeight(context) - height;
        }

        // 获取bitmap的宽高
        float bitmapWidth = mBitmap.getWidth();
        float bitmapHeight = mBitmap.getHeight();

        // 计算缩放比
        float scale = mAvailableScreenWidth / bitmapWidth;
        // 如果bitmap缩放之后高大于屏幕可用高度，则以高为基准计算缩放比
        if (scale * bitmapHeight > mAvailableScreenHeight) {
            scale = mAvailableScreenHeight / bitmapHeight;
        }

        // 缩放bitmap
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
    }

    /**
     * 获取系统状态栏高度
     *
     * @param context context
     * @return float
     */
    private static float getStatusBarHeight(Context context) {
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return 0;
    }

    /**
     * 初始化
     */
    private void init() {
        // 初始化画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);  // 抗锯齿
        mPaint.setColor(Color.WHITE);   // 画笔颜色为白色
        mPaint.setStyle(Paint.Style.STROKE);    // 画笔样式为线条
        mPaint.setStrokeWidth(1);   // 画笔线条宽度为1
    }
}
