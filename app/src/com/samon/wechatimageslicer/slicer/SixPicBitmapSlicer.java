package com.samon.wechatimageslicer.slicer;

/**
 * Created by xushanmeng on 2017/12/12.
 */

public class SixPicBitmapSlicer extends BitmapSlicer {
    @Override
    protected int getHorizontalPicNumber() {
        return 3;
    }

    @Override
    protected int getVerticalPicNumber() {
        return 2;
    }
}
