package com.scenemaxeng.projector;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.scenemaxeng.compiler.ActionCommandPos;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableDef;

public class EntityPosController extends SceneMaxBaseController {

    private final SceneMaxApp app;
    private final SceneMaxThread thread;
    private final ActionCommandPos cmd;
    private final ProgramDef prg;
    private String targetVar;
    private VariableDef targetVarDef;


    public EntityPosController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ActionCommandPos cmd) {
        this.app=app;
        this.thread=thread;
        this.cmd=cmd;
        this.prg=prg;

        targetVarDef = cmd.varDef;

    }

    public boolean run(float tpf) {
        if (forceStop) return true;

        Double valX=null;
        Double valY=null;
        Double valZ=null;
        RunTimeVarDef entityForPos=null;
        Vector3f calculatedPosition=null;

        if(cmd.posStatement!=null) {
            RunTimeVarDef lookatVar = app.findVarRuntime(prg,thread,cmd.posStatement.startEntity);
            Spatial sp = app.getEntitySpatial(lookatVar.varName,lookatVar.varDef.varType);

            if(sp==null) {
                // error - probably user typed wrong object name
                return true;
            }

            calculatedPosition = sp.getWorldTranslation().clone();
            Quaternion locRot = sp.getLocalRotation();
            Util.calcPositionStatementVerbs(this.thread, cmd.posStatement,locRot,calculatedPosition);

        } else if(cmd.x!=null) {
            valX = (Double) new ActionLogicalExpression(cmd.x, thread).evaluate();
            valY = (Double) new ActionLogicalExpression(cmd.y, thread).evaluate();
            valZ = (Double) new ActionLogicalExpression(cmd.z, thread).evaluate();
        } else if(cmd.entityPos!=null) {
            entityForPos = app.findVarRuntime(prg,thread,cmd.entityPos);
        }

        if(cmd.varDef.varType== VariableDef.VAR_TYPE_SPHERE || cmd.varDef.varType== VariableDef.VAR_TYPE_BOX) {
            int threadId = app.getEntityThreadId(thread, cmd.varName ,cmd.varDef.varType);
            this.targetVar = cmd.varDef.varName + "@" + threadId;
        } else if(cmd.varDef.varType== VariableDef.VAR_TYPE_OBJECT) {
            EntityInstBase obj = (EntityInstBase) thread.getFuncScopeParam(cmd.varDef.varName);

            if(obj==null) {
                app.handleRuntimeError("Function argument '"+cmd.varDef.varName+"' is undefined");
                return true;
            }

            this.targetVar = obj.varDef.varName + "@" + obj.thread.threadId;
            targetVarDef=new VariableDef();// in order to avoid overriding varType
            targetVarDef.varType = obj.varDef.varType;
        } else if(cmd.varDef.varType!= VariableDef.VAR_TYPE_CAMERA) {
            int threadId = app.getEntityThreadId(thread, cmd.varName);
            this.targetVar = cmd.varDef.varName + "@" + threadId;
        }

        this.enableEntity(targetVar);// enable this entity
        if(StopModelController.forceStopCommands.get(targetVar)!=null) {
            return true;
        }


        if(targetVarDef.varType== ProgramDef.VAR_TYPE_3D){
            app.posModel(targetVar,valX,valY,valZ, entityForPos, calculatedPosition);
        } else if(targetVarDef.varType== ProgramDef.VAR_TYPE_2D){
            app.posSprite(targetVar,valX,valY,valZ,entityForPos, calculatedPosition);
        } else if(targetVarDef.varType== ProgramDef.VAR_TYPE_CAMERA){
            app.posCamera(valX,valY,valZ,entityForPos, calculatedPosition);
        } else if(targetVarDef.varType== ProgramDef.VAR_TYPE_SPHERE){
            app.posSphere(targetVar,valX,valY,valZ,entityForPos, calculatedPosition);
        } else if(targetVarDef.varType== VariableDef.VAR_TYPE_BOX){
            app.posBox(targetVar,valX,valY,valZ,entityForPos, calculatedPosition);
        }

        return true;
    }


}
