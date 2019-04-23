#ifndef _TENANT_ENV_H_
#define _TENANT_ENV_H_

#include "jni.h"

#define TENANT_ENV_VERSION_1_0  0x00200010 // 0x00200000 represents tenant module and the last 10 represents version 1.0

/*
 * Tenant Native Method Interface.
 */

struct TenantNativeInterface_;
struct TenantEnv_;

#ifdef __cplusplus
typedef TenantEnv_ TenantEnv;
#else
typedef const struct TenantNativeInterface_* TenantEnv;
#endif

/*
 * We use inlined functions for C++ so that programmers can write:
 *   tenantEnv->GetTenantFlags(cls);
 * in C++ rather than:
 *   (*tenantEnv)->GetTenantFlags(tenantEnv, cls);
 * in C.
 */
struct TenantNativeInterface_ {
  jint (JNICALL *GetTenantFlags)(TenantEnv *env, jclass cls);
};

struct TenantEnv_ {
  const struct TenantNativeInterface_ *functions;
#ifdef __cplusplus
  jint GetTenantFlags(jclass cls) {
    return functions->GetTenantFlags(this, cls);
  }
#endif
};

#endif
