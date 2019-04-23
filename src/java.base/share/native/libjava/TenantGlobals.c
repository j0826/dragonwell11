#include "jni.h"
#include "tenantenv.h"
#include "jni_util.h"
#include "jvm.h"
#include "com_alibaba_tenant_TenantGlobals.h"

static TenantEnv*
getTenantEnv(JNIEnv *env)
{
    jint rc               = JNI_ERR;
    JavaVM *jvm           = NULL;
    TenantEnv  *tenantEnv = NULL;
    (*env)->GetJavaVM(env, &jvm);
    if(NULL != jvm) {
        /* Get tenant environment */
        rc = (*jvm)->GetEnv(jvm, (void **)&tenantEnv, TENANT_ENV_VERSION_1_0);
        if (JNI_OK != rc) {
        tenantEnv = NULL;
    }
  }
  return tenantEnv;
}

JNIEXPORT jint JNICALL
Java_com_alibaba_tenant_TenantGlobals_getTenantFlags(JNIEnv *env, jclass cls)
{
    jint rc  =  JNI_ERR;
    TenantEnv* tenantEnv = getTenantEnv(env);
    if(NULL != tenantEnv) {
        rc = (*tenantEnv)->GetTenantFlags(tenantEnv, cls);
    } else {
        //throw exception
        JNU_ThrowByName(env, "java/lang/InternalError", "Can not get tenant environment.");
    }
  return rc;
}
