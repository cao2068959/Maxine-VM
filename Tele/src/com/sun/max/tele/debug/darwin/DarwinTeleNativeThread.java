/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.tele.debug.darwin;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public class DarwinTeleNativeThread extends TeleNativeThread {

    private DarwinTeleChannelProtocol protocol;

    public DarwinTeleNativeThread(DarwinTeleProcess teleProcess, Params params) {
        super(teleProcess, params);
        protocol = (DarwinTeleChannelProtocol) TeleVM.teleChannelProtocol();
    }

    @Override
    public DarwinTeleProcess teleProcess() {
        return (DarwinTeleProcess) super.teleProcess();
    }

    @Override
    public boolean updateInstructionPointer(Address address) {
        return protocol.setInstructionPointer(machThread(), address.toLong());
    }

    private long machThread() {
        return localHandle();
    }

    @Override
    protected boolean readRegisters(
                    byte[] integerRegisters,
                    byte[] floatingPointRegisters,
                    byte[] stateRegisters) {
        return protocol.readRegisters(machThread(),
                        integerRegisters, integerRegisters.length,
                        floatingPointRegisters, floatingPointRegisters.length,
                        stateRegisters, stateRegisters.length);
    }

    @Override
    protected boolean singleStep() {
        return protocol.singleStep(machThread());
    }

    @Override
    protected boolean threadResume() {
        throw TeleError.unimplemented();
    }

    @Override
    public boolean threadSuspend() {
        throw TeleError.unimplemented();
    }

}
