package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ChangeVelocityCommand;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableDef;

public class ChangeVelocityController extends SceneMaxBaseController {

    public ChangeVelocityController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ChangeVelocityCommand cmd) {
        super(app, prg, thread, cmd);
    }

    public boolean run(float tpf) {

        if (forceStop) return true;
        findTargetVar();

        ChangeVelocityCommand cmd = (ChangeVelocityCommand)this.cmd;
        Double velocity = (Double) new ActionLogicalExpression(cmd.velocityExpr,this.thread).evaluate();

        if(this.targetVarDef.varType== VariableDef.VAR_TYPE_3D) {
            this.app.applyModelVelocity(this.targetVar,velocity);
        } else if(this.targetVarDef.varType== VariableDef.VAR_TYPE_BOX) {

        } else if(this.targetVarDef.varType== VariableDef.VAR_TYPE_SPHERE) {

        }

        return true;
    }

}
