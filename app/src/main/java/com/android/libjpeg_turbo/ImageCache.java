package com.android.libjpeg_turbo;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 管理内存中的图片
 */
public class ImageCache {
    private static ImageCache imageCache;
    private Context context;
    private LruCache<String, Bitmap> memoryCache;
    private DiskLruCache diskLruCache;
    BitmapFactory.Options options = new BitmapFactory.Options();

    /**
     * 定义一个复用池
     */
    public static Set<WeakReference<Bitmap>> reuserablePool;
    ReferenceQueue referenceQueue;
    Thread clearReferenceQueue;
    boolean shutDown;

    /**
     * 得到引用队列
     * @return
     */
    private ReferenceQueue<Bitmap> getReferenceQueue(){
        if (null == referenceQueue){
            //弱引用需要被回收的时候，就进会这个队列
            referenceQueue = new ReferenceQueue<Bitmap>();
            clearReferenceQueue = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!shutDown){
                        try {
                            Reference<Bitmap> reference = referenceQueue.remove();
                            Bitmap bitmap = reference.get();
                            if (null != bitmap && !bitmap.isRecycled()){
                                bitmap.recycle();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            clearReferenceQueue.start();
        }
        return referenceQueue;
    }

    public static ImageCache getImageCache(){
        if (null == imageCache){
            synchronized (ImageCache.class){
                if (null == imageCache) imageCache = new ImageCache();
            }

        }
        return imageCache;

    }

    public  void init(Context context,String dir){
        this.context = context.getApplicationContext();
         reuserablePool = Collections.synchronizedSet(new HashSet<WeakReference<Bitmap>>());

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClass = activityManager.getMemoryClass();
        memoryCache = new LruCache<String, Bitmap>(memoryClass/8*1024*1024){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (Build.VERSION.SDK_INT >Build.VERSION_CODES.KITKAT){
                    return value.getAllocationByteCount();
                }
                return value.getByteCount();
            }

            /**
             * 当lru满了,bitmap从lru中移除，回调
             * @param evicted
             * @param key
             * @param oldValue
             * @param newValue
             */
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                //super.entryRemoved(evicted, key, oldValue, newValue);
                if (oldValue.isMutable()){
                    reuserablePool.add(new WeakReference<Bitmap>(oldValue,referenceQueue));
                }else {
                    oldValue.recycle();
                }

            }
        };
        try {
            diskLruCache = DiskLruCache.open(new File(dir),BuildConfig.VERSION_CODE,1,10*1024*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void putBitmapMemory(String key,Bitmap bitmap){
        memoryCache.put(key,bitmap);
    }
    public Bitmap getBitmapFromMemory(String key){
        return memoryCache.get(key);
    }
    public void clearMemoryCache(){
        memoryCache.evictAll();
    }

    /**
     * 获取复用池中的内容
     * @param w
     * @param h
     * @param inSampleSize
     * @return
     */
    public Bitmap getReuseable(int w,int h,int inSampleSize){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
            return null;
        }
        Bitmap reuseable = null;
        Iterator<WeakReference<Bitmap>> iterator = reuserablePool.iterator();
        while (iterator.hasNext()){
            Bitmap bitmap = iterator.next().get();
            if (null != bitmap){
                if (checkBitmap(bitmap,w,h,inSampleSize)){
                    reuseable = bitmap;
                    iterator.remove();
                    break;
                }else {
                    iterator.remove();;
                }
            }

        }
        return reuseable;
    }

    private boolean checkBitmap(Bitmap bitmap, int w, int h, int inSampleSize) {
        if (Build.VERSION.SDK_INT <Build.VERSION_CODES.KITKAT){
            return bitmap.getWidth() == w && bitmap.getHeight() == h && inSampleSize ==1;
        }
        if (inSampleSize >=1){
            w/=inSampleSize;
            h/=inSampleSize;
        }
        int byteCount = w*h*getPixelsCount(bitmap.getConfig());
        return byteCount<=bitmap.getAllocationByteCount();
    }

    private int getPixelsCount(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888){
            return 4;
        }
        return 2;
    }

    /**
     * 磁盘缓存的处理
     * @param key
     * @param bitmap
     */
    public void putBitmapToDisk(String key,Bitmap bitmap){
        DiskLruCache.Snapshot snapshot =null;
        OutputStream os = null;
        try {
            snapshot = diskLruCache.get(key);
            if (null == snapshot){
                DiskLruCache.Editor edit = diskLruCache.edit(key);
                if (null != edit){
                    os = edit.newOutputStream(0);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,50,os);
                    edit.commit();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != snapshot){
                snapshot.close();
            }
            if (null != os){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从磁盘取，取到放在内存一份
     * @param key
     * @param reuseable
     * @return
     */
    public Bitmap getBitmapFromDisk(String key,Bitmap reuseable){
        DiskLruCache.Snapshot snapshot =null;
        Bitmap bitmap = null;
        try {
            snapshot = diskLruCache.get(key);
            if (null == snapshot){
                return null;
            }
            //获取文件输入流
            InputStream inputStream = snapshot.getInputStream(0);
            //解码图片写入
            options.inMutable =true;
            options.inBitmap = reuseable;
            bitmap=BitmapFactory.decodeStream(inputStream,null,options);
            if (null != bitmap){
                memoryCache.put(key,bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != snapshot){
                snapshot.close();
            }

        }
        return bitmap;
    }
}
