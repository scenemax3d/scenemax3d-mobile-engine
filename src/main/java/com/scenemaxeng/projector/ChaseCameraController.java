package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ChaseCameraCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class ChaseCameraController extends SceneMaxBaseController{

    private boolean targetCalculated=false;

    public ChaseCameraController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ChaseCameraCommand cmd) {
        this.app=app;
        this.prg=prg;
        this.thread=thread;
        this.cmd=cmd;

        this.targetVarDef=cmd.varDef;
    }

    public boolean run(float tpf) {

        if (forceStop) return true;

        ChaseCameraCommand cmd = (ChaseCameraCommand)this.cmd;
        if(cmd.command==ChaseCameraCommand.STOP) {
            app.setChaseCameraOff();

            return true;
        }

        if(!targetCalculated) {

            findTargetVar();

            if(cmd.havingAttributesExists) {
                if(cmd.rotationSpeedExpr!=null) {
                    cmd.rotationSpeedVal = (Double) new ActionLogicalExpression(cmd.rotationSpeedExpr,thread).evaluate();
                }

                if(cmd.verticalRotationExpr!=null) {
                    cmd.verticalRotationVal = (Double) new ActionLogicalExpression(cmd.verticalRotationExpr,thread).evaluate();
                }

                if(cmd.horizontalRotationExpr!=null) {
                    cmd.horizontalRotationVal = (Double) new ActionLogicalExpression(cmd.horizontalRotationExpr,thread).evaluate();
                }

                if(cmd.minDistanceExpr!=null) {
                    cmd.minDistanceVal = (Double) new ActionLogicalExpression(cmd.minDistanceExpr,thread).evaluate();
                }

                if(cmd.maxDistanceExpr!=null) {
                    cmd.maxDistanceVal = (Double) new ActionLogicalExpression(cmd.maxDistanceExpr,thread).evaluate();
                }
            }

            targetCalculated=true;
        }

        app.setChaseCameraOn(targetVar,targetVarDef.varType, cmd);

        return true;

    }

}
