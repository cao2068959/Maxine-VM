/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package jtt.lang;

/*
 * @Harness: java
 * @Runs: 0 = true; 1 = true; 2 = true; 3 = false
 */
public class System_identityHashCode01 {
    private static final Object object0 = new Object();
    private static final Object object1 = new Object();
    private static final Object object2 = new Object();

    private static final int hash0 = System.identityHashCode(object0);
    private static final int hash1 = System.identityHashCode(object1);
    private static final int hash2 = System.identityHashCode(object2);

    public static boolean test(int i) {
        if (i == 0) {
            return hash0 == System.identityHashCode(object0);
        }
        if (i == 1) {
            return hash1 == System.identityHashCode(object1);
        }
        if (i == 2) {
            return hash2 == System.identityHashCode(object2);
        }
        return false;
    }
}
