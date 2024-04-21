#ifndef SERIALPORT_H
#define SERIALPORT_H

#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <limits.h>
#include <dirent.h>
#include <termios.h>
#include <errno.h>
#include <stdio.h>
#include <sys/select.h>
#include <string.h>
#include <string>
#include <vector>

class Serialport
{
private:
    int ttyFd;
public:
    Serialport():ttyFd(-1){}
	
	static std::vector<std::string> enumerate()
	{
		std::vector<std::string> devList;
		DIR *p_dir = opendir("/dev");
        if (p_dir == nullptr) {
            return devList;
        }

        while (1) {
            struct dirent *p_ent = readdir(p_dir);
            if (p_ent == nullptr) {
                break;
            }
            devList.push_back(p_ent->d_name);
        }
		return devList;
	}
	
    int openDevice(const std::string &path, unsigned long baudrate)
	{
        if (ttyFd != -1) {
            return 0;
        }
        ttyFd = open(path.c_str(), O_RDWR | O_NOCTTY | O_NDELAY);
        if (ttyFd == -1){
			return -1;
		}
		struct termios options;
        tcgetattr(ttyFd, &options); //获取原有的串口属性的配置
        if (tcgetattr(ttyFd, &options)<0){
             //return -2;
        }
        bzero(&options, sizeof(options));
        options.c_cflag |= (CLOCAL|CREAD ); // CREAD 开启串行数据接收，CLOCAL并打开本地连接模式
        options.c_cflag |= CS8; //设置8位数据位
        options.c_cflag &= ~PARENB; //无校验位
        /* 设置9600波特率为B9600
		   如果是115200则为B115200
		 */
        if (baudrate == 9600) {
            cfsetispeed(&options, B9600);
            cfsetospeed(&options, B9600);
        } else if (baudrate == 19200) {
            cfsetispeed(&options, B19200);
            cfsetospeed(&options, B19200);
        } else if (baudrate == 38400) {
            cfsetispeed(&options, B38400);
            cfsetospeed(&options, B38400);
        } else if (baudrate == 115200) {
            cfsetispeed(&options, B115200);
            cfsetospeed(&options, B115200);
        } else if (baudrate == 230400) {
            cfsetispeed(&options, B230400);
            cfsetospeed(&options, B230400);
        }else if (baudrate == 460800) {
            cfsetispeed(&options, B460800);
            cfsetospeed(&options, B460800);
        }
        options.c_cflag &= ~CSTOPB;/* 设置一位停止位; */
        options.c_cc[VTIME] = 1;     // 读取一个字符等待1*(1/10)s
        options.c_cc[VMIN] = 1;        // 读取字符的最少个数为1

        tcflush(ttyFd, TCIOFLUSH);    //清掉串口缓存
        fcntl(ttyFd, F_SETFL, 0);    //串口阻塞  0阻塞1非阻塞

        if (tcsetattr(ttyFd, TCSANOW, &options) != 0) {
            //return -3;
        }
		return 0;
	}

    int closeDevice()
    {
        if (ttyFd != -1) {
            int ret = close(ttyFd);
            ttyFd = -1;
            return ret;
        }
        return 0;
    }

	int readRaw(char* &data, int datasize)
	{
        return read(ttyFd, &data, datasize);
	}
	
	int writeRaw(const char* data, int datasize)
	{
         return write(ttyFd, data, datasize);
	}

};

#endif // SERIALPORT_H
