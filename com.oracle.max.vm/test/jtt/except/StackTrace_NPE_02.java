/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.except;

/*
 * @Harness: java
 * @Runs: 0=0; 1=0; -2=-1; 3=0
 */
public class    StackTrace_NPE_02 {

    private static String[] trace = {"test1", "test"};

    public static int test(int a) {
        try {
            if (a >= 0) {
                return test1();
            }
        } catch (NullPointerException npe) {
            String thisClass = StackTrace_NPE_02.class.getName();
            StackTraceElement[] stackTrace = npe.getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement e = stackTrace[i];
                if (e.getClassName().equals(thisClass)) {
                    for (int j = 0; j < trace.length; j++) {
                        StackTraceElement f = stackTrace[i + j];
                        if (!f.getClassName().equals(thisClass)) {
                            return -2;
                        }
                        if (!f.getMethodName().equals(trace[j])) {
                            return -3;
                        }
                    }
                    return 0;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("all")
    private static int test1() {
        final Object o = null;
        return o.hashCode();
    }
}