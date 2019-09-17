package com.android.libjpeg_turbo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageResize {
    public static Bitmap resizeBitmap(Context context,int id,int maxW,int maxH,boolean isAlapha){

        Resources resources = context.getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();

        //第一次解码，拿到系统处理的信息
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(resources,id,options);

        int w = options.outWidth;
        int h = options.outHeight;

        options.inSampleSize = calcuteInSampleSize(w,h,maxW,maxH);

        if (!isAlapha){
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources,id,options);

    }

    private static int calcuteInSampleSize(int w, int h, int maxW, int maxH) {
        int inSampleSize = 1;
        if (w>maxW && h >maxH){
            inSampleSize =2;
            while (w/inSampleSize >maxH && h/inSampleSize >maxH){
                inSampleSize *=2;
            }
        }
        inSampleSize /= 2;
        return inSampleSize;
    }
}
