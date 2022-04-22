package com.scenemaxeng.projector;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.scenemaxeng.compiler.DoBlockCommand;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.RayCheckCommand;

public class RayCheckCommandController extends CompositeController {

    public RayCheckCommandController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, RayCheckCommand cmd) {
        this.app=app;
        this.prg=prg;
        this.thread=thread;
        this.cmd=cmd;
    }

    public boolean run(float tpf) {

        if (forceStop) return true;

        if (!targetCalculated) {

            RayCheckCommand cmd = (RayCheckCommand) this.cmd;

            targetCalculated = true;
            Vector3f pos=null;
            Vector3f dir=null;
            if(cmd.posX!=null) {
                Double x = (Double) new ActionLogicalExpression(cmd.posX,thread).evaluate();
                Double y = (Double) new ActionLogicalExpression(cmd.posY,thread).evaluate();
                Double z = (Double) new ActionLogicalExpression(cmd.posZ,thread).evaluate();
                pos=new Vector3f(x.floatValue(),y.floatValue(),z.floatValue());
            } else {
                if(cmd.entityPos!=null) {// entityPos exists when not using mouse cursor
                    RunTimeVarDef rtvar = app.findVarRuntime(prg, thread, cmd.entityPos);
                    Spatial sp = app.getEntitySpatial(rtvar.varName, rtvar.varDef.varType);
                    pos = sp.getWorldTranslation();
                    dir = sp.getWorldRotation().mult(Vector3f.UNIT_Z);
                }
            }

            GroupInst gi = thread.getGroup(cmd.targetGroup);

            if (this.app.rayCastCheck(gi,pos,dir)) {
                DoBlockCommand doCmd = cmd.doBlock;
                DoBlockController c = new DoBlockController(app, thread,doCmd);
                c.app = app;
                c.async = cmd.isAsync;
                this.add(c);
            } else {
                return true; // ray cast failed - nothing to do
            }

        }

        return super.run(tpf);

    }
}
