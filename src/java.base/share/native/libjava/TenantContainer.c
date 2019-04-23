#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jmm.h"
#include "com_alibaba_tenant_TenantContainer.h"

#define TENANT "Lcom/alibaba/tenant/TenantContainer;"
#define THREAD "Ljava/lang/Thread;"
#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

static const JmmInterface* jmm_interface = NULL;

static JNINativeMethod methods[] = {
    {"current0",                "()" TENANT,        (void *)&JVM_CurrentTenant },
    {"attach0",                 "()I",              (void *)&JVM_AttachToTenant },
    {"detach0",                 "()I",              (void *)&JVM_DetachFromTenant },
    {"prepareForDestroy0",      "(Z)V",             (void *)&JVM_TenantPrepareForDestroy },
    {"wakeUpTenantThread",      "(" THREAD ")V",    (void *)&JVM_WakeUpTenantThread },
    {"maskTenantShutdown0",     "()V",              (void *)&JVM_MaskTenantShutdown },
    {"unmaskTenantShutdown0",   "()V",              (void *)&JVM_UnmaskTenantShutdown },
    {"interruptTenantThread",   "(" THREAD ")V",    (void *)&JVM_InterruptTenantThread },
    {"dumpThreads",             "([" THREAD ")V",   (void *)&JVM_DumpTenantThreadStacks },
    {"hasTenantDeathException", "(" THREAD ")Z",    (void *)&JVM_IsKilledByTenant }
};

JNIEXPORT void JNICALL
Java_com_alibaba_tenant_TenantContainer_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}

JNIEXPORT void JNICALL
Java_com_alibaba_tenant_TenantContainer_getThreadsAllocatedMemory(JNIEnv *env,
                                                                jobject obj,
                                                                jlongArray ids,
                                                                jlongArray sizeArray)
{
    if (NULL == jmm_interface) {
        jmm_interface = (JmmInterface*) JVM_GetManagement(JMM_VERSION_2);
    }

    jmm_interface->GetThreadAllocatedMemory(env, ids, sizeArray);
}

