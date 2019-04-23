/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

DoNothingClosure do_nothing_cl;

void CLDToOopClosure::do_cld(ClassLoaderData* cld) {
  cld->oops_do(_oop_closure, _must_claim_cld);
}

void ObjectToOopClosure::do_object(oop obj) {
  obj->oop_iterate(_cl);
}

void VoidClosure::do_void() {
  ShouldNotCallThis();
}

void CodeBlobToOopClosure::do_nmethod(nmethod* nm) {
  nm->oops_do(_cl);
  if (_fix_relocations) {
    nm->fix_oop_relocations();
  }
}

void CodeBlobToOopClosure::do_code_blob(CodeBlob* cb) {
  nmethod* nm = cb->as_nmethod_or_null();
  if (nm != NULL) {
    do_nmethod(nm);
  }
}

void MarkingCodeBlobClosure::do_code_blob(CodeBlob* cb) {
  nmethod* nm = cb->as_nmethod_or_null();
  if (nm != NULL && !nm->test_set_oops_do_mark()) {
    do_nmethod(nm);
  }
}

bool IsTenantClassLoaderAlive::do_object_b(oop obj) {
  assert(TenantThreadStop && SafepointSynchronize::is_at_safepoint(), "pre-condition");
  if (obj != NULL) {
    oop loader_obj = NULL;
    if (obj->klass_or_null() != NULL && obj->is_instance()) {
      HandleMark hm;
      if (obj->is_a(SystemDictionary::Class_klass())) {
        loader_obj = java_lang_Class::class_loader(obj);
        if (loader_obj == NULL) {
          return true; // bootstrap classloader
        }
      } else if (obj->is_a(SystemDictionary::ProtectionDomain_klass())) {
        loader_obj = java_security_ProtectionDomain::class_loader(Handle(Thread::current(), obj));
      } else if (obj->klass()->id() == InstanceClassLoaderKlassID) {
        loader_obj = obj;
      }
    }

    if (loader_obj != NULL && java_lang_ClassLoader::is_dead(loader_obj)) {
      _found_any = true;
      return false;
    }
  }
  // alive by default
  return true;
}
