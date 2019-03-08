#include "jni.h"
#include "jvm.h"
#include "com_alibaba_aot_AppAOTController.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

static JNINativeMethod appaot_methods[] = {
  {"loadAOTLibraryForLoader0","(Ljava/lang/ClassLoader;Ljava/lang/String;)I", (void *)&JVM_LoadAOTLibrary},
  {"unloadAOTLibraryForLoader0","(Ljava/lang/ClassLoader;)V", (void *)&JVM_UnloadAOTLibrary}
};

JNIEXPORT void JNICALL
Java_com_alibaba_aot_AppAOTController_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, appaot_methods, ARRAY_LENGTH(appaot_methods));
}
