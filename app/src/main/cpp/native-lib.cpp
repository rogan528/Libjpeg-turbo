#include <jni.h>
#include <string>
#include <malloc.h>
#include <android/bitmap.h>
#include <jpeglib.h>

void write_JPEG_file(uint8_t *temp, int w, int h, jint quarity, const char *path);

extern "C" JNIEXPORT jstring JNICALL
Java_com_android_libjpeg_1turbo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

void write_JPEG_file(uint8_t *data, int w, int h, jint quarity, const char *path) {

    jpeg_compress_struct jcs;
    jpeg_error_mgr error_mgr;
    jcs.err = jpeg_std_error(&error_mgr);
    jpeg_create_compress(&jcs);
    FILE *f=fopen(path,"wd");
    jpeg_stdio_dest(&jcs, f);
    jcs.image_width = w;
    jcs.image_height=h;

    jcs.input_components =3;
    jcs.in_color_space=JCS_RGB;
    jpeg_set_defaults(&jcs);
    //开启hash
    jcs.optimize_coding= true;
    jpeg_set_quality(&jcs,quarity,1);
    jpeg_start_compress(&jcs,1);
    int row_stride=w*3;
    JSAMPROW row[1];
    while (jcs.next_scanline<jcs.image_height){
        uint8_t *pixels=data+jcs.next_scanline*row_stride;
        row[0]=pixels;
        jpeg_write_scanlines(&jcs,row,1);


    }
    jpeg_finish_compress(&jcs);
    fclose(f);
    jpeg_destroy_compress(&jcs);


}

extern "C"
JNIEXPORT void JNICALL
Java_com_android_libjpeg_1turbo_MainActivity_nativeCompress(JNIEnv *env, jobject instance,
                                                            jobject bitmap, jint quarity,
                                                            jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);

    // TODO
    //获取argb数据
    AndroidBitmapInfo bitmapInfo;
    //获取里面的信息
    AndroidBitmap_getInfo(env,bitmap,&bitmapInfo);

    uint8_t *pixels;
    AndroidBitmap_lockPixels(env,bitmap,(void **)&pixels);

    int w = bitmapInfo.width;
    int h = bitmapInfo.height;
    int color;
    uint8_t r,g,b;
    uint8_t *data=(uint8_t *)malloc(w*h*3);
    uint8_t *temp = data;
    for (int i=0;i<h;i++){
        for (int j=0;j<w;j++){
            color=*(int *)pixels;
            r=(color>>16) & 0xFF;
            g=(color>>8) & 0xFF;
            b=color & 0xFF;
            *data=b;
            *(data+1)=g;
            *(data+2)=r;
            data+=3;
            pixels +=4;
        }

    }
    write_JPEG_file(temp,w,h,quarity,path);

    free(data);
    AndroidBitmap_unlockPixels(env,bitmap);
    env->ReleaseStringUTFChars(path_, path);
}