/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.asm;

import com.sun.max.lang.*;

/**
 * This class provides a skeletal implementation of the {@link SymbolicArgument} interface, to minimize the
 * effort required to implement this interface.
 *
 * @author Doug Simon
 */
public abstract class AbstractSymbolicArgument implements SymbolicArgument, StaticFieldName {

    private String name;
    private final int value;

    protected AbstractSymbolicArgument(String name, int value) {
        this.name = name;
        this.value = value;
    }

    protected AbstractSymbolicArgument(int value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public int value() {
        return value;
    }

    public String externalValue() {
        return "%" + name().toLowerCase();
    }

    public long asLong() {
        return value();
    }

    public String disassembledValue() {
        return externalValue();
    }

    @Override
    public String toString() {
        return name();
    }
}
