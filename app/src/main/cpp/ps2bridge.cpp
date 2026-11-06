#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_ps2emulatorpro_NativeBridge_coreStatus(JNIEnv* env, jobject /* thiz */) {
    return env->NewStringUTF("CORE_NOT_INTEGRATED");
}
