#pragma version(1)
#pragma rs java_package_name(com.urun.camera_test)
#pragma rs_fp_relaxed

uchar4 __attribute__((kernel)) convert(uint32_t x, uint32_t y) {
    uchar4 ret = {(uchar) x, (uchar) y, (uchar) (x+y), (uchar)255};
    return ret;
}