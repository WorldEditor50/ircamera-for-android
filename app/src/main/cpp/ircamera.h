//
// Created by Admin on 4/16/2024.
//

#ifndef IRCAMERA_H
#define IRCAMERA_H
#include <cmath>
#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"

class IRCamera
{
public:
    static constexpr int hi = 24;
    static constexpr int wi = 32;
    static constexpr int totalSize = hi*wi;
    struct Packet {
        unsigned short header;
        unsigned short size;
        unsigned short temperatures[768];
        unsigned short ta;
        unsigned short checksum;
    };
    struct Temperature {
        int i;
        int j;
        float value;
    };
private:
    unsigned char *img;
    Temperature maxTemp;
    Temperature minTemp;
public:
    IRCamera();
    ~IRCamera();
    inline static IRCamera& instance() {
        static IRCamera camera;
        return camera;
    }
    void process(unsigned char* data, int len, int h, int w, unsigned char* img);
};

#endif //IRCAMERA_H
