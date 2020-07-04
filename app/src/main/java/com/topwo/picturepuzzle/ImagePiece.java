package com.topwo.picturepuzzle;

import android.graphics.Bitmap;

/**
 * Description: 图片保存类
 * Data：2016/9/11-19:54
 * Blog：www.qiuchengjia.cn
 * Author: qiu
 */
public class ImagePiece {
    public int index=0;//图片的索引
    public Bitmap bitmap=null;//图片
    public int x = 0;
    public int y = 0;

    public int getWidth(){
        return bitmap.getWidth();
    }
    public int getHeight(){
        return bitmap.getHeight();
    }
}
