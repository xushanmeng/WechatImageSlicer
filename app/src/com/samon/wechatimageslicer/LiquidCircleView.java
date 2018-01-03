package com.samon.wechatimageslicer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class LiquidCircleView extends View {

    private static final int DURATION = 1000;
    private static final int COLOR_BACKGROUND = Color.WHITE;
    private static final int COLOR_FORGROUND = 0xff22bb2f;

    private Paint mPaint;
    private float mWidth;
    private float mHeight;
    private float mSize;
    private float strokeWidth;

    private long startTime = 0;
    private RectF mRectF = null;

    public LiquidCircleView(Context context) {
        super(context);
        init(context);
    }

    public LiquidCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LiquidCircleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint();
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mRectF = new RectF();
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mSize = (mWidth < mHeight ? mWidth : mHeight) * 0.8f;
        mRectF.left = (mWidth - mSize) / 2;
        mRectF.right = (mWidth + mSize) / 2;
        mRectF.top = (mHeight - mSize) / 2;
        mRectF.bottom = (mHeight + mSize) / 2;
        strokeWidth = mSize / 10;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        long current = 0;
        current = System.currentTimeMillis();

        //Draw background
        mPaint.setColor(COLOR_BACKGROUND);
        mPaint.setAlpha(0xf0);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);
        canvas.drawCircle(mWidth / 2, mHeight / 2, mSize / 2, mPaint);

        //Draw progress body
        mPaint.setAlpha(0xFF);
        mPaint.setColor(COLOR_FORGROUND);
        float offsetPercent = ((current - startTime) % DURATION) * 1.0f / DURATION;
        float centerAngle = 360 * offsetPercent - 90;
        float sweepAngle = 360 * offsetPercent;
        if (sweepAngle >= 180) {
            sweepAngle = 360 - sweepAngle;
        }
        canvas.drawArc(mRectF, centerAngle - sweepAngle / 2, sweepAngle, false, mPaint);

        //Draw progress head and tail
        float startAngle = centerAngle - sweepAngle / 2 + 90;
        float endAngle = centerAngle + sweepAngle / 2 + 90;
        float startX, startY, endX, endY;
        float radius = mSize / 2;
        startX = (float) (radius * Math.sin(startAngle * Math.PI / 180) + mWidth / 2);
        startY = (float) (mHeight / 2 - radius * Math.cos(startAngle * Math.PI / 180));
        endX = (float) (radius * Math.sin(endAngle * Math.PI / 180) + mWidth / 2);
        endY = (float) (mHeight / 2 - radius * Math.cos(endAngle * Math.PI / 180));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(startX, startY, strokeWidth / 2, mPaint);
        canvas.drawCircle(endX, endY, strokeWidth / 2, mPaint);

        if (isShown()) {
            invalidate();
        }
    }
}
