#include "tenantenv.h"
#include "runtime/globals.hpp"

/**
 * Be careful: any change to the following constant defintions, you MUST
 * synch up them with ones defined in com.alibaba.tenant.TenantGlobals
 */

#define TENANT_FLAG_MULTI_TENANT_ENABLED             (0x1)    // bit 0 to indicate if the tenant feature is enabled.
#define TENANT_FLAG_HEAP_THROTTLING_ENABLED          (0x2)    // bit 1 to indicate if heap throttling feature is enabled.
#define TENANT_FLAG_CPU_THROTTLING_ENABLED           (0x4)    // bit 2 to indicate if cpu throttling feature is enabled.
#define TENANT_FLAG_DATA_ISOLATION_ENABLED           (0x8)    // bit 3 to indicate if data isolation(e.g static vairable isolation) feature is enabled.
#define TENANT_FLAG_THREAD_STOP_ENABLED             (0x10)    // bit 4 to indicate if spawned threads will be killed at TenantContainer.destroy()
#define TENANT_FLAG_IOHANDLE_RECLAIMING_ENABLED     (0x20)    // bit 5 to indicate if IO handle reclaiming feature is enabled.
#define TENANT_FLAG_CPU_ACCOUNTING_ENABLED          (0x40)    // bit 6 to indicate if cpu accounting feature is enabled.
#define TENANT_FLAG_HEAP_ISOLATION_ENABLED          (0x80)    // bit 7 to indicate if heap isolation feature is enabled.
#define TENANT_FLAG_DIRECT_TENURED_ALLOC_ENABLED     (0x100)   // bit 8 to indicate if direct tenure alloc feature is enabled.
#define TENANT_FLAG_USE_TENANT_GCIH                  (0x200)    // bit 9 to indicate if tenant GCIH is enabled.
#define TENANT_FLAG_SOCKET_THROTTLING_ENABLED        (0x400)    // bit 10 to indicate if tenant socket throttling feature is enabled.

jint tenant_GetTenantFlags(TenantEnv *env, jclass cls);

static struct TenantNativeInterface_ tenantNativeInterface = {
  tenant_GetTenantFlags
};

struct TenantNativeInterface_* tenant_functions()
{
  return &tenantNativeInterface;
}

jint
tenant_GetTenantFlags(TenantEnv *env, jclass cls)
{
  jint result = 0x0;

  if (MultiTenant) {
    result |= TENANT_FLAG_MULTI_TENANT_ENABLED;
  }

  /*
  if (TenantHeapThrottling) {
    result |= TENANT_FLAG_HEAP_THROTTLING_ENABLED;
  }

  if (TenantCpuThrottling) {
    result |= TENANT_FLAG_CPU_THROTTLING_ENABLED;
  }

  if (TenantSocketThrottling) {
    result |= TENANT_FLAG_SOCKET_THROTTLING_ENABLED;
  }
  */

  if (TenantDataIsolation) {
    result |= TENANT_FLAG_DATA_ISOLATION_ENABLED;
  }

  if (TenantThreadStop) {
    result |= TENANT_FLAG_THREAD_STOP_ENABLED;
  }

  /*
  if (TenantIOHandleReclaim) {
    result |= TENANT_FLAG_IOHANDLE_RECLAIMING_ENABLED;
  }

  if (TenantCpuAccounting) {
    result |= TENANT_FLAG_CPU_ACCOUNTING_ENABLED;
  }

  if (TenantHeapIsolation) {
    result |= TENANT_FLAG_HEAP_ISOLATION_ENABLED;
  }

  if (DirectTenuredAlloc) {
    result |= TENANT_FLAG_DIRECT_TENURED_ALLOC_ENABLED;
  }

  if (UseZenGC && GCIHSize > 0) {
    result |= TENANT_FLAG_USE_TENANT_GCIH;
  }
  */

  return result;
}
