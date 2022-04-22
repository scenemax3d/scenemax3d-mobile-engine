package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VehicleSetupCommand;

public class CarSetupController extends SceneMaxBaseController{

    public CarSetupController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, VehicleSetupCommand cmd) {
        super(app, prg, thread, cmd);

    }


    public boolean run(float tpf) {

        if (forceStop) return true;

        VehicleSetupCommand cmd = (VehicleSetupCommand) this.cmd;
        RunTimeVarDef entity = findTargetVar(cmd.targetVar);

        if(cmd.setupEngine) {
            if(cmd.enginePowerExp!=null) {
                cmd.enginePowerVal = (Double) new ActionLogicalExpression(cmd.enginePowerExp, thread).evaluate();
            }

            if(cmd.engineBreakingExp!=null) {
                cmd.engineBreakingVal = (Double) new ActionLogicalExpression(cmd.engineBreakingExp, thread).evaluate();
            }

        } else {


            if (cmd.compressionExpr != null) {
                cmd.compressionVal = (Double) new ActionLogicalExpression(cmd.compressionExpr, thread).evaluate();
            }

            if (cmd.dampingExpr != null) {
                cmd.dampingVal = (Double) new ActionLogicalExpression(cmd.dampingExpr, thread).evaluate();
            }

            if (cmd.frictionExpr != null) {
                cmd.frictionVal = (Double) new ActionLogicalExpression(cmd.frictionExpr, thread).evaluate();
            }

            if (cmd.stiffnessExpr != null) {
                cmd.stiffnessVal = (Double) new ActionLogicalExpression(cmd.stiffnessExpr, thread).evaluate();
            }

            if (cmd.lengthExpr != null) {
                cmd.lengthVal = (Double) new ActionLogicalExpression(cmd.lengthExpr, thread).evaluate();
            }

        }


        app.setupVehicle(entity,cmd);

        return true;
    }

}
