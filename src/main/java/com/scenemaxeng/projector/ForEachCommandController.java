package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ForEachCommand;
import com.scenemaxeng.compiler.FunctionInvocationCommand;
import com.scenemaxeng.compiler.ProgramDef;

import java.util.List;

public class ForEachCommandController extends CompositeController{

    public ForEachCommandController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ForEachCommand cmd) {
        super(app, prg, thread, cmd);
    }

    public boolean run(float tpf) {

        if (forceStop) return true;
        if(!targetCalculated) {
            targetCalculated=true;

            ForEachCommand cmd = (ForEachCommand)this.cmd;

            List<EntityInstBase> entities = app.getAllEntities(cmd.entityType,cmd.name, cmd.nameComparator);
            for(EntityInstBase e:entities) {
                FunctionInvocationCommand fic = new FunctionInvocationCommand();
                fic.funcName = cmd.funcDef.name;
                fic.funcParam = e;
                SceneMaxBaseController ctl = app.runFunctionInvocationCommand(prg, thread, fic);
                this.add(ctl);
            }

        }

        return super.run(tpf);
    }

//    private EntityInstBase decideWhichObjectIsRes(SceneMaxThread thread, String res) {
//
//        ModelInst mi = thread.getModel(res);
//        if(mi!=null) {
//            return mi;
//        } else {
//            SpriteInst si = thread.getSprite(res);
//            if(si!=null) {
//                return si;
//            } else {
//                SphereInst sphi = thread.getSphere(res);
//                if(sphi!=null) {
//                    return sphi;
//                } else {
//                    BoxInst bxinst = thread.getBox(res);
//                    if(bxinst!=null) {
//                        return bxinst;
//                    } else {
////                        isObject = false;
////                        hasRuntimeError = true;
////                        app.handleRuntimeError("Line: " + ctx.start.getLine() + ", '" + res.toString() + "' is not a valid number or variable");
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//

}
