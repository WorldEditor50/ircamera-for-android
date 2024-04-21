#include <jni.h>
#include "ircamera.h"

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("ircamera");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("ircamera")
//      }
//    }

IRCamera::IRCamera():img(nullptr) {
    img = new unsigned char[hi*wi*4];
}
IRCamera::~IRCamera() {
    if (img != nullptr) {
        delete [] img;
    }
}
void IRCamera::process(unsigned char *data, int len, int h, int w, unsigned char *image) {
    Packet *packet = (Packet*)data;
    //float ta = packet->ta;
    //float envTemp = ta - 800;
    unsigned short* temperatures = packet->temperatures;
    /* find max-min temperature */
    float maxValue = temperatures[0];
    float minValue = temperatures[0];
    for (int i = 0; i < hi; i++) {
        for (int j = 0; j < wi; j++) {
            int k = i*wi + j;
            if (maxValue < temperatures[k]) {
                maxValue = temperatures[k];
                maxTemp.value = maxValue;
                maxTemp.i = i;
                maxTemp.j = j;
            }
            if (minValue > temperatures[k]) {
                minValue = temperatures[k];
                minTemp.value = maxValue;
                minTemp.i = i;
                minTemp.j = j;
            }
        }
    }
    /* convert to argb image */
    int widthstep = wi*4;
    for (int i = 0; i < hi; i++) {
        for (int j = 0; j < wi; j++) {
            float temp = temperatures[i*wi + j];
            /* normalize */
            double pixel = (temp - minValue)/(maxValue - minValue)*255;
            /* argb */
            img[i*widthstep + j*4]     = pixel;
            img[i*widthstep + j*4 + 1] = 0;
            img[i*widthstep + j*4 + 2] = 0;
            img[i*widthstep + j*4 + 3] = 0;
        }
    }
    cv::Mat src(hi, wi, CV_8UC4, img);
    cv::Mat dst(h, w, CV_8UC4, image);
    /* gaussian filter */
    cv::GaussianBlur(src, src, cv::Size(3, 3), 0);
    /* resize */
    cv::resize(src, dst, cv::Size(w, h), 0, 0, cv::INTER_LINEAR);
    /* max temperature */
    maxTemp.value /= 100;
    maxTemp.i *= h/hi;
    maxTemp.j *= w/wi;
    cv::putText(dst,
                std::to_string(maxTemp.value),
                cv::Point(maxTemp.i, maxTemp.j),
                cv::FONT_HERSHEY_SIMPLEX,
                0.5,
                cv::Scalar(255, 255, 255,0));

    return;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ircamera_IRCamera_parseToImage(JNIEnv *env, jobject thiz, jbyteArray data,
                                                jint len, jint h, jint w, jbyteArray image) {
    // TODO: implement parseToImage()
    unsigned char *cdata = (unsigned char *)env->GetByteArrayElements(data, nullptr);
    unsigned char *cimage = (unsigned char *)env->GetByteArrayElements(image, nullptr);
    IRCamera::instance().process(cdata, len, h, w, cimage);
    env->ReleaseByteArrayElements(data, (jbyte*)cdata, 0);
    env->ReleaseByteArrayElements(image, (jbyte*)cimage, 0);
}