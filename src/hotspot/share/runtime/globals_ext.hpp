/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_VM_RUNTIME_GLOBALS_EXT_HPP
#define SHARE_VM_RUNTIME_GLOBALS_EXT_HPP

#include "runtime/flags/jvmFlag.hpp"

// globals_extension.hpp extension
#define AJDK_FLAGS(develop, develop_pd, product, product_pd, diagnostic, experimental, notproduct, manageable, product_rw, lp64_product) \
                                                                            \
  diagnostic(bool, DisableInterfaceMethodrefCheck, false,                   \
          "Disable JVM_CONSTANT_InterfaceMethodref bytecode check as JDK8") \
                                                                            \
  product(bool, UseAppAOT, false,                                           \
          "Use AOT for app class loader")                                   \
                                                                            \
  product(bool, PromoteAOTtoFullProfile, true,                              \
          "promote to full profile level for aot tiered-compiled method")   \
                                                                            \
  product(bool, MultiTenant, false,                                         \
          "Enable the multi-tenant feature.")                               \
                                                                            \
  product(bool, TenantDataIsolation, false,                                 \
          "Enable data isolation(e.g static vairable) per tenant")          \
                                                                            \
  product(bool, TenantThreadStop, false,                                    \
          "Stop all spawned threads when calling TenantContainer.destroy()")\
                                                                            \
  develop(bool, TraceTenantThreadStop, false,                               \
          "Trace threads killing details from TenantContainer.destroy()")   \
                                                                            \
  diagnostic(bool, TraceEagerlyPurgeDeadOops, false,                        \
          "Trace eagerly purging dead oops")                                \
                                                                            \
  product(bool, TenantCpuThrottling, false,                                 \
          "Enable cpu throttling per tenant")                               \
                                                                            \
  product(bool, TenantCpuAccounting, false,                                 \
          "Enable cpu accounting per tenant")                               \
                                                                            \
  product(bool, EnableCoroutine, false,                                     \
          "Enable coroutine support")                                       \
                                                                            \
  product(uintx, DefaultCoroutineStackSize, 128*K,                          \
          "Default size of stack that is associated with new coroutine")    \
                                                                            \
  product(bool, UseWispMonitor, false,                                      \
          "yields to next coroutine when ObjectMonitor is contended")       \
                                                                            \
  product(bool, UseWisp2, false,                                            \
          "Enable Wisp2")                                                   \
                                                                            \
  manageable(bool, PrintThreadCoroutineInfo, false,                         \
          "print the park/unpark information for thread coroutine")         \
                                                                            \
  diagnostic(bool, VerboseWisp, false,                                      \
          "Print verbose Wisp information")                                 \
                                                                            \
  product(bool, EagerAppCDS, false,                                         \
          "aggressively skip over loadClass() to speed up boot time")       \
                                                                            \
  product(bool, NotFoundClassOpt, false,                                    \
          "optimization for not found class in EagerAppCDS flow")           \
                                                                            \
  experimental(bool, EagerAppCDSLegacyVerisonSupport, false,                \
          "dump the classes which is compiled JDK1.5 or below")             \
                                                                            \
  product(bool, AppCDSClassFingerprintCheck, false,                         \
          "Use class fingerprint to do the sanity check for AppCDS "        \
          "instead of class path")                                          \
                                                                            \
  product(double, ZHighUsagePercent, 95.0,                                  \
          "Percentage of heap usage for ZGC high usage rule")               \
                                                                            \
  product(bool, ZBalancePageCache, false,                                   \
          "Balance small and medium cached pages to avoid "                 \
          "page cache flush while relocating")                              \
                                                                            \
  diagnostic(double, ZMinPageCachePercent, 1.0,                             \
          "Lower bound of percentage of heap capacity for "                 \
          "cached small or medium pages")                                   \
                                                                            \
  diagnostic(uintx, ZPageAllocRateSampleWindow, 60,                         \
          "Sample windows (seconds) for small and medium page "             \
          "allocation rate")                                                \
                                                                            \
  product(bool, IgnoreInvalidEntryAtStringTableExpansion, true,             \
          "ignore invalid node in StringTable when growing")                \
                                                                            \
  manageable(bool, PrintCodeCacheFlushStackTrace, false,                    \
          "Print stack trace when code cache flushing occurred due to "     \
          "class re-definition")                                            \
                                                                            \
  product(bool, JFREnableEarlyNativeEventSupport, false,                    \
          "Enable early native events support")                             \
                                                                            \
  notproduct(bool, TestJFREnableEarlyNativeEventSupport, true,              \
          "Test early native events support")                               \
                                                                            \
  manageable(intx, ArrayAllocationWarningSize, (512*M),                     \
             "Desired size in bytes of array space allocation before "      \
             "printing a warning")                                          \

  //add new AJDK specific flags here


AJDK_FLAGS(DECLARE_DEVELOPER_FLAG, DECLARE_PD_DEVELOPER_FLAG, DECLARE_PRODUCT_FLAG, DECLARE_PD_PRODUCT_FLAG, DECLARE_DIAGNOSTIC_FLAG, DECLARE_EXPERIMENTAL_FLAG, DECLARE_NOTPRODUCT_FLAG, DECLARE_MANAGEABLE_FLAG, DECLARE_PRODUCT_RW_FLAG, DECLARE_LP64_PRODUCT_FLAG)

// Additional JVMFlags enum values
#define JVMFLAGS_EXT AJDK_FLAGS(RUNTIME_DEVELOP_FLAG_MEMBER, RUNTIME_PD_DEVELOP_FLAG_MEMBER,             \
                                       RUNTIME_PRODUCT_FLAG_MEMBER, RUNTIME_PD_PRODUCT_FLAG_MEMBER,      \
                                       RUNTIME_DIAGNOSTIC_FLAG_MEMBER, RUNTIME_EXPERIMENTAL_FLAG_MEMBER, \
                                       RUNTIME_NOTPRODUCT_FLAG_MEMBER, RUNTIME_MANAGEABLE_FLAG_MEMBER,   \
                                       RUNTIME_PRODUCT_RW_FLAG_MEMBER, RUNTIME_LP64_PRODUCT_FLAG_MEMBER)

// Additional JVMFlagsWithType enum values
#define JVMFLAGSWITHTYPE_EXT AJDK_FLAGS(RUNTIME_DEVELOP_FLAG_MEMBER_WITH_TYPE,              \
                                               RUNTIME_PD_DEVELOP_FLAG_MEMBER_WITH_TYPE,    \
                                               RUNTIME_PRODUCT_FLAG_MEMBER_WITH_TYPE,       \
                                               RUNTIME_PD_PRODUCT_FLAG_MEMBER_WITH_TYPE,    \
                                               RUNTIME_DIAGNOSTIC_FLAG_MEMBER_WITH_TYPE,    \
                                               RUNTIME_EXPERIMENTAL_FLAG_MEMBER_WITH_TYPE,  \
                                               RUNTIME_NOTPRODUCT_FLAG_MEMBER_WITH_TYPE,    \
                                               RUNTIME_MANAGEABLE_FLAG_MEMBER_WITH_TYPE,    \
                                               RUNTIME_PRODUCT_RW_FLAG_MEMBER_WITH_TYPE,    \
                                               RUNTIME_LP64_PRODUCT_FLAG_MEMBER_WITH_TYPE)

// globals.cpp extension

// Additional flag definitions
#define MATERIALIZE_FLAGS_EXT AJDK_FLAGS(MATERIALIZE_DEVELOPER_FLAG, MATERIALIZE_PD_DEVELOPER_FLAG,  \
                                         MATERIALIZE_PRODUCT_FLAG, MATERIALIZE_PD_PRODUCT_FLAG,      \
                                         MATERIALIZE_DIAGNOSTIC_FLAG, MATERIALIZE_EXPERIMENTAL_FLAG, \
                                         MATERIALIZE_NOTPRODUCT_FLAG, MATERIALIZE_MANAGEABLE_FLAG,   \
                                         MATERIALIZE_PRODUCT_RW_FLAG, MATERIALIZE_LP64_PRODUCT_FLAG)

// Additional flag descriptors: see flagTable definition
#define FLAGTABLE_EXT AJDK_FLAGS(RUNTIME_DEVELOP_FLAG_STRUCT, RUNTIME_PD_DEVELOP_FLAG_STRUCT,      \
                                 RUNTIME_PRODUCT_FLAG_STRUCT, RUNTIME_PD_PRODUCT_FLAG_STRUCT,      \
                                 RUNTIME_DIAGNOSTIC_FLAG_STRUCT, RUNTIME_EXPERIMENTAL_FLAG_STRUCT, \
                                 RUNTIME_NOTPRODUCT_FLAG_STRUCT, RUNTIME_MANAGEABLE_FLAG_STRUCT,   \
                                 RUNTIME_PRODUCT_RW_FLAG_STRUCT, RUNTIME_LP64_PRODUCT_FLAG_STRUCT)


// Default method implementations

inline bool JVMFlag::is_unlocker_ext() const {
  return false;
}

inline bool JVMFlag::is_unlocked_ext() const {
  return true;
}

inline bool JVMFlag::is_writeable_ext() const {
  return false;
}

inline bool JVMFlag::is_external_ext() const {
  return false;
}

inline JVMFlag::MsgType JVMFlag::get_locked_message_ext(char* buf, int buflen) const {
  assert(buf != NULL, "Buffer cannot be NULL");
  buf[0] = '\0';
  return JVMFlag::NONE;
}

#endif // SHARE_VM_RUNTIME_GLOBALS_EXT_HPP
