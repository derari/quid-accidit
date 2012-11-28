package de.hpi.accidit.asmtracer;

import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class TracerTransformer2 {

    private static final String TRACER = "de/hpi/accidit/asmtracer/Tracer";
    private static final String ENTER = "enter";
    private static final String ENTER_DESC = "(Ljava/lang/String;)V";
    
    public static void tranform(ClassReader cr, ClassWriter cw) throws Exception{

        ClassNode classNode=new ClassNode();
        cr.accept(classNode, 0);

        //Let's move through all the methods
        System.out.println(classNode.name);

        for(MethodNode methodNode: (List<MethodNode>) classNode.methods){
            System.out.println(methodNode.name+"  "+methodNode.desc);

            //Lets insert the begin logger
            InsnList beginList=new InsnList();
            beginList.add(new LdcInsnNode(methodNode.name));
            beginList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRACER, ENTER, ENTER_DESC));

            Iterator<AbstractInsnNode> insnNodes;
//            Iterator<AbstractInsnNode> insnNodes=methodNode.instructions.iterator();
//            while(insnNodes.hasNext()){
//                System.out.println(insnNodes.next().getOpcode());
//            }
//                
            methodNode.instructions.insert(beginList);
//                System.out.println(methodNode.instructions);
                
            //A method can have multiple places for return
            //All of them must be handled.
            insnNodes=methodNode.instructions.iterator();
            while(insnNodes.hasNext()){
                AbstractInsnNode insn=insnNodes.next();
//                System.out.println(insn.getOpcode());

                if(insn.getOpcode()==Opcodes.IRETURN
                        ||insn.getOpcode()==Opcodes.RETURN
                        ||insn.getOpcode()==Opcodes.ARETURN
                        ||insn.getOpcode()==Opcodes.LRETURN
                        ||insn.getOpcode()==Opcodes.DRETURN){
                    InsnList endList=new InsnList();
                    endList.add(new LdcInsnNode(methodNode.name));
                    endList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/geekyarticles/asm/Logger", "logMethodReturn", "(Ljava/lang/String;)V"));
                    methodNode.instructions.insertBefore(insn, endList);
                }

            }
        }

        classNode.accept(cw);
    }
    
}
