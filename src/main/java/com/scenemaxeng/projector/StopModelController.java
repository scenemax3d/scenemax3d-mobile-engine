package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ActionCommandStop;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableDef;

import java.util.HashMap;

class StopModelController extends SceneMaxBaseController{

    private final SceneMaxApp app;
    private final ProgramDef prg;
    private final ActionCommandStop cmd;
    private SceneMaxThread thread;
    private VariableDef targetVarDef;
    public static HashMap<String,Boolean> forceStopCommands = new HashMap<>();

    public StopModelController(SceneMaxApp app, ProgramDef prg, ActionCommandStop cmd, SceneMaxThread thread) {
        this.app=app;
        this.prg=prg;
        this.cmd=cmd;
        this.thread=thread;
        this.targetVarDef=cmd.varDef;
    }

    @Override
    public boolean run(float tpf) {

        int threadId=-1;
        String targetVar = null;

        if(cmd.varDef.varType== VariableDef.VAR_TYPE_OBJECT) {
            EntityInstBase obj = (EntityInstBase) thread.getFuncScopeParam(cmd.varDef.varName);
            if(obj==null) {
                app.handleRuntimeError("Function argument '"+cmd.varDef.varName+"' is undefined");
                return true;
            }
            targetVar = obj.varDef.varName + "@" + obj.thread.threadId;
            targetVarDef = new VariableDef();// in order to avoid overriding varType
            targetVarDef.varType = obj.varDef.varType;
        } else if(cmd.varDef.varType==VariableDef.VAR_TYPE_SPHERE) {
            threadId = app.getEntityThreadId(thread, cmd.targetVar,cmd.varDef.varType);
            targetVar = cmd.varDef.varName + "@" + threadId;
        } else if(cmd.varDef.varType!= VariableDef.VAR_TYPE_CAMERA) {
            threadId=app.getEntityThreadId(thread,cmd.targetVar);
            targetVar = cmd.varDef.varName + "@" + threadId;
        }

        forceStopCommands.put(targetVar,true);

        return true;

    }
}
