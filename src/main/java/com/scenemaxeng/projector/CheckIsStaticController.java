package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.CheckIsStaticCommand;
import com.scenemaxeng.compiler.DoBlockCommand;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableDef;

public class CheckIsStaticController extends SceneMaxBaseController {

    private final SceneMaxApp app;
    private final ProgramDef prg;
    private final SceneMaxThread thread;
    private final CheckIsStaticCommand cmd;
    private VariableDef targetVarDef;
    private boolean targetCalculated=false;
    private String targetVar;
    private double targetTime;


    public CheckIsStaticController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, CheckIsStaticCommand cmd) {
        this.app=app;
        this.prg=prg;
        this.thread=thread;
        this.cmd=cmd;
        this.targetVarDef=cmd.varDef;
    }

    public boolean run(float tpf) {
        if (forceStop) return true;

        if (!targetCalculated) {
            targetCalculated=true;

            if(cmd.varDef.varType== VariableDef.VAR_TYPE_SPHERE || cmd.varDef.varType==VariableDef.VAR_TYPE_BOX) {
                int threadId = app.getEntityThreadId(thread, cmd.varDef.varName,cmd.varDef.varType);
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
                int threadId = app.getEntityThreadId(thread, cmd.varDef.varName);
                this.targetVar = cmd.varDef.varName + "@" + threadId;
            }
        }

        if(this.targetTime==0) {
            if (app.isEntityStatic(targetVar, targetVarDef.varType)) {
                double delay = Double.parseDouble(new ActionLogicalExpression(this.cmd.timeExpr,thread).evaluate().toString());
                this.targetTime = delay;
            }
        } else {
            this.targetTime-=tpf;
            if(this.targetTime<=0) {
                DoBlockCommand doCmd = this.cmd.doBlock;
                DoBlockController c = new DoBlockController(app, thread, doCmd);
                //c.cmd = doCmd;
                c.app = app;
                c.async = doCmd.isAsync;

                this.app.registerController(c);
                return true;
            }
        }

        return false;

    }



}
