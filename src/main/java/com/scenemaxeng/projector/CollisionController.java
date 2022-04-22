package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.CollisionStatementCommand;
import com.scenemaxeng.compiler.DoBlockCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class CollisionController extends CompositeController {

    private String targetVar1;
    private String targetVar2;

    public CollisionController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, CollisionStatementCommand cmd) {
        super(app,prg,thread,cmd);
    }

    public boolean run(float tpf)
    {
        if(forceStop) return true;

        CollisionStatementCommand collCmd = (CollisionStatementCommand)this.cmd;
        RunTimeVarDef rt1 = findTargetVar(collCmd.varDef1.varName);
        RunTimeVarDef rt2 = findTargetVar(collCmd.varDef2.varName);
        this.targetVar1 = rt1.varName;
        this.targetVar2 = rt2.varName;

        DoBlockCommand cmd = collCmd.doBlock;
        DoBlockController c = new DoBlockController(app, thread, cmd);

        c.app = app;
        c.async = cmd.isAsync;

        app.addCollisionHandler(this.targetVar1,this.targetVar2,c,
                thread.getEntityInst(collCmd.varDef1.varName),
                thread.getEntityInst(collCmd.varDef2.varName),
                collCmd.part1, collCmd.part2, collCmd.goExpr);

        return true;

    }




}
