package com.android.libjpeg_turbo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class MyAdpter extends BaseAdapter {
    private Context context;

    public MyAdpter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return 999;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHodler hodler;
        if (null == convertView) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
            hodler = new ViewHodler(convertView);
            convertView.setTag(hodler);
        }else {
            hodler = (ViewHodler) convertView.getTag();
        }
        Bitmap bitmap = ImageCache.getImageCache().getBitmapFromMemory(String.valueOf(position));
        if (null == bitmap){
            //从复用池
            Bitmap reuseable = ImageCache.getImageCache().getReuseable(60,60,1);



            bitmap = ImageCache.getImageCache().getBitmapFromDisk(String.valueOf(position),reuseable);
            if (null == bitmap){
                //从网络加载
                bitmap = ImageResize.resizeBitmap(context,R.mipmap.wyz_p,80,80,false,reuseable);
                ImageCache.getImageCache().putBitmapToDisk(String.valueOf(position),bitmap);
                Log.d("zhangbin","从网络加载");
            }else {
                Log.d("zhangbin","从磁盘加载");
            }
        }else {
            Log.d("zhangbin","从内存加载");
        }
        hodler.imageView.setImageBitmap(bitmap);
        return convertView;
    }

    private class ViewHodler {
        ImageView imageView;
        public ViewHodler(View view) {
            imageView = view.findViewById(R.id.iv);
        }
    }
}
