package com.lewis.audiovideostudy.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.lewis.audiovideostudy.R;

/**
 * 显示图片的自定义View
 */
public class ShowPictureView extends View {

    private Bitmap mBitmap;
    private Paint mPicPaint;

    public ShowPictureView(Context context) {
        this(context, null);
    }

    public ShowPictureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShowPictureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);

        mPicPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * 设置Bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 30, 10, mPicPaint);
            canvas.save();
        }
    }
}
