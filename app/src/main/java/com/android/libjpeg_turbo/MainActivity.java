package com.android.libjpeg_turbo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Bitmap bitmap;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        //bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.timg);

       /* compress(bitmap,Bitmap.CompressFormat.JPEG,50,
                Environment.getExternalStorageDirectory()
                +"/test_q.jpeg");

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,300,300,true);
        compress(scaledBitmap,Bitmap.CompressFormat.JPEG,100,
                Environment.getExternalStorageDirectory() +"/test_scaled.jpeg");

        compress(bitmap,Bitmap.CompressFormat.PNG,100,
                Environment.getExternalStorageDirectory()+"/test.png");
        (bitmap,Bitmap.CompressFormat.WEBP,100,
                Environment.getExternalStorageDirectory()+"/test.wep");*/
        File input = new File(Environment.getExternalStorageDirectory(),"test.jpg");
        bitmap= BitmapFactory.decodeFile(input.getAbsolutePath());


    }

    /**
     * @param bitmap 待压缩图片
     * @param compressFormat 压缩格式
     * @param quarity 质量
     * @param path 文件地址
     */
    private void compress(Bitmap bitmap, Bitmap.CompressFormat compressFormat,int quarity,String path){
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(path);
            bitmap.compress(compressFormat,quarity,fileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (null != fileOutputStream){
                try {
                    fileOutputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native void nativeCompress(Bitmap bitmap,int quarity,String path);

    public void test(View view) {
        nativeCompress(bitmap,50,Environment.getExternalStorageDirectory()+"/timg22.jpg");
    }
}
