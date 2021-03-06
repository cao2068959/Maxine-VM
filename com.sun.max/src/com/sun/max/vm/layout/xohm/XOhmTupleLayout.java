/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.layout.xohm;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 */
public final class XOhmTupleLayout extends XOhmGeneralLayout implements TupleLayout {

    public Layout.Category category() {
        return Layout.Category.TUPLE;
    }

    @Override
    public boolean isTupleLayout() {
        return true;
    }

    public boolean isArrayLayout() {
        return false;
    }

    @INLINE
    public Size specificSize(Accessor accessor) {
        final Hub hub = UnsafeCast.asHub(readHubReference(accessor).toJava());
        return hub.tupleSize;
    }

    private final int headerSize = (2 + xtraCount) * Word.size();

    @INLINE
    public int headerSize() {
        return headerSize;
    }

    public HeaderField[] headerFields() {
        return XHeaderField.headerFields(false, xtraCount);
    }

    @INLINE
    public int getFieldOffsetInCell(FieldActor fieldActor) {
        return fieldActor.offset();
    }

    private static final int INVALID_OFFSET = -1;

    private static boolean setInvalidOffsets(FieldActor[] fieldActors) {
        for (FieldActor fieldActor : fieldActors) {
            fieldActor.setOffset(INVALID_OFFSET);
        }
        return true;
    }

    private static boolean hasValidOffsets(FieldActor[] fieldActors) {
        for (FieldActor fieldActor : fieldActors) {
            if (fieldActor.offset() == INVALID_OFFSET) {
                return false;
            }
        }
        return true;
    }

    private int fillAlignmentGap(FieldActor[] fieldActors, int offset, int nAlignmentBytes) {
        final int nBytesToFill = nAlignmentBytes - (offset % nAlignmentBytes);
        assert nBytesToFill > 0;
        int scale = nAlignmentBytes;
        int currentOffset = offset;
        while (scale >= 1) {
            for (FieldActor fieldActor : fieldActors) {
                if (scale > nBytesToFill) {
                    break;
                }
                if (fieldActor.offset() == INVALID_OFFSET && fieldActor.kind.width.numberOfBytes == scale) {
                    fieldActor.setOffset(currentOffset);
                    currentOffset += scale;
                    assert nBytesToFill >= 0;
                    if (nBytesToFill == 0) {
                        assert currentOffset % nAlignmentBytes == 0;
                        return currentOffset;
                    }
                }
            }
            scale >>= 1;
        }
        return Ints.roundUp(currentOffset, nAlignmentBytes);
    }


    Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors, int headerSize) {
        setInvalidOffsets(fieldActors);
        final int nAlignmentBytes = Word.size();
        int offset = (superClassActor == null || superClassActor.typeDescriptor == JavaTypeDescriptor.HYBRID) ? headerSize : superClassActor.dynamicTupleSize().toInt();
        if (offset % nAlignmentBytes != 0) {
            offset = fillAlignmentGap(fieldActors, offset, nAlignmentBytes);
        }
        for (int scale = 8; scale >= 1; scale >>= 1) {
            for (FieldActor fieldActor : fieldActors) {
                if (fieldActor.offset() == INVALID_OFFSET && fieldActor.kind.width.numberOfBytes == scale) {
                    fieldActor.setOffset(offset);
                    offset += scale;
                }
            }
        }
        assert hasValidOffsets(fieldActors);
        offset = Ints.roundUp(offset, nAlignmentBytes);
        return Size.fromInt(offset);
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        return layoutFields(superClassActor, fieldActors, headerSize());
    }

    @HOSTED_ONLY
    private void visitFields(ObjectCellVisitor visitor, Object tuple, FieldActor[] fieldActors) {
        for (FieldActor fieldActor : fieldActors) {
            final Value value = fieldActor.getValue(tuple);
            visitor.visitField(getFieldOffsetInCell(fieldActor), fieldActor.name, fieldActor.descriptor(), value);
        }
    }

    @HOSTED_ONLY
    void visitFields(ObjectCellVisitor visitor, Object tuple) {
        final Hub hub = ObjectAccess.readHub(tuple);
        ClassActor classActor = hub.classActor;
        if (hub instanceof StaticHub) {
            visitFields(visitor, tuple, classActor.localStaticFieldActors());
        } else {
            do {
                visitFields(visitor, tuple, classActor.localInstanceFieldActors());
                classActor = classActor.superClassActor;
            } while (classActor != null);
        }
    }

    @HOSTED_ONLY
    public void visitObjectCell(Object tuple, ObjectCellVisitor visitor) {
        visitHeader(visitor, tuple);
        visitFields(visitor, tuple);
    }

    @HOSTED_ONLY
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        return mirror.readField(offset);
    }

    @HOSTED_ONLY
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        mirror.writeField(offset, value);
    }

}
