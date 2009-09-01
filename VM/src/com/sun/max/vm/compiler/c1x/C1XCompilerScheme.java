/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.xir.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.d.e.amd64.target.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;

/**
 * @author Ben L. Titzer
 */
public class C1XCompilerScheme extends AbstractVMScheme implements CompilerScheme {

    private CiTarget c1xTarget;
    private MaxRiRuntime c1xRuntime;
    private C1XCompiler compiler;
    private XirRuntime xirRuntime;

    @PROTOTYPE_ONLY
    private final Map<TargetMethod, C1XTargetMethodGenerator> targetMap = new HashMap<TargetMethod, C1XTargetMethodGenerator>();

    public C1XCompilerScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public IrGenerator irGenerator() {
        return null;
    }

    public Sequence<IrGenerator> irGenerators() {
        return null;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PROTOTYPING) {
            // create the Target object passed to C1X
            InstructionSet isa = vmConfiguration().platform().processorKind.instructionSet;
            CiArchitecture arch = CiArchitecture.findArchitecture(isa.name().toLowerCase());
            TargetABI targetABI = vmConfiguration().targetABIsScheme().optimizedJavaABI();

            // get the unallocatable registers
            Set<String> unallocatable = new HashSet<String>();
            RegisterRoleAssignment roles = targetABI.registerRoleAssignment();
            markUnallocatable(unallocatable, roles, VMRegister.Role.SAFEPOINT_LATCH);
            markUnallocatable(unallocatable, roles, VMRegister.Role.CPU_STACK_POINTER);
            markUnallocatable(unallocatable, roles, VMRegister.Role.CPU_FRAME_POINTER);
            markUnallocatable(unallocatable, roles, VMRegister.Role.ABI_SCRATCH);
            markUnallocatable(unallocatable, roles, VMRegister.Role.LITERAL_BASE_POINTER);

            // create the RiRuntime object passed to C1X
            c1xRuntime = MaxRiRuntime.globalRuntime;

            // configure the allocatable registers
            List<CiRegister> allocatable = new ArrayList<CiRegister>(arch.registers.length);
            for (CiRegister r : arch.registers) {
                if (!unallocatable.contains(r.name.toLowerCase()) && r != c1xRuntime.threadRegister()) {
                    allocatable.add(r);
                }
            }
            CiRegister[] allocRegs = allocatable.toArray(new CiRegister[allocatable.size()]);

            // TODO (tw): Initialize target differently
            c1xTarget = new CiTarget(arch, allocRegs, allocRegs, vmConfiguration().platform.pageSize, true);
            c1xTarget.stackAlignment = targetABI.stackFrameAlignment();
            xirRuntime = new MaxXirRuntime(vmConfiguration(), c1xTarget);

            compiler = new C1XCompiler(c1xRuntime, c1xTarget);
        }
    }

    private void markUnallocatable(Set<String> unallocatable, RegisterRoleAssignment roles, VMRegister.Role register) {
        Symbol intReg = roles.integerRegisterActingAs(register);
        if (intReg != null) {
            unallocatable.add(intReg.name().toLowerCase());
        }
        Symbol floatReg = roles.floatingPointRegisterActingAs(register);
        if (floatReg != null) {
            unallocatable.add(floatReg.name().toLowerCase());
        }
    }

    public long numberOfCompilations() {
        return 0;
    }

    public void createBuiltins(PackageLoader packageLoader) {
        // do nothing.
    }

    public void createSnippets(PackageLoader packageLoader) {
        // do nothing.
    }

    public boolean areSnippetsCompiled() {
        return true;
    }

    public void compileSnippets() {
        // do nothing
    }

    public Word createInitialVTableEntry(int index, VirtualMethodActor dynamicMethodActor) {
        return Word.zero();
    }

    public Word createInitialITableEntry(int index, VirtualMethodActor dynamicMethodActor) {
        return Word.zero();
    }

    public void staticTrampoline() {
        throw new UnsupportedOperationException();
    }

    public final IrMethod compile(ClassMethodActor classMethodActor) {
        // ignore compilation directive for now
        RiMethod method = c1xRuntime.getRiMethod(classMethodActor);
        CiTargetMethod compiledMethod = compiler.compileMethod(method, xirRuntime).targetMethod();
        if (compiledMethod != null) {

            C1XTargetMethodGenerator generator = new C1XTargetMethodGenerator(this, classMethodActor, null, compiledMethod);
            C1XTargetMethod targetMethod = generator.finish();

            if (MaxineVM.isPrototyping()) {
                // in prototyping mode, we need to be able to iterate over the calls in the code
                // for the closure process
                targetMap.put(targetMethod, generator);
            }
            assert targetMethod != null;
            return targetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }


    @PROTOTYPE_ONLY
    public void gatherCalls(TargetMethod targetMethod, AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        // iterate over all the calls in this target method and add them to the appropriate lists
        // this is used in code reachability during prototyping
        C1XTargetMethodGenerator ciTargetMethod = targetMap.get(targetMethod);
        assert ciTargetMethod != null : "no registered MaxCiTargetMethod for this TargetMethod";
        ciTargetMethod.gatherCalls(directCalls, virtualCalls, interfaceCalls);
    }

    @PROTOTYPE_ONLY
    public void initializeForJitCompilations() {
    }

    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, TargetMethod lastJavaCallee, StackFrameWalker.Purpose purpose, Object context) {
        return BcdeTargetAMD64Compiler.walkFrameHelper(stackFrameWalker, isTopFrame, targetMethod, lastJavaCallee, purpose, context);
    }

    public Pointer namedVariablesBasePointer(Pointer stackPointer, Pointer framePointer) {
        return stackPointer;
    }

    public boolean isBuiltinImplemented(Builtin builtin) {
        return true;
    }
}
