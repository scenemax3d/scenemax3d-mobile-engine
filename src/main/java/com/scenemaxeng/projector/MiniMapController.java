package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.MiniMapCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class MiniMapController extends SceneMaxBaseController {

    public MiniMapController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, MiniMapCommand cmd) {
        super(app, prg, thread, cmd);
    }

    public boolean run(float tpf) {

        if (forceStop) return true;

        if(cmd.targetVar!=null) {
            findTargetVar();
        }

        MiniMapCommand cmd = (MiniMapCommand)this.cmd;
        if(cmd.sizeExpr!=null) {
            cmd.sizeVal = ((Double)new ActionLogicalExpression(cmd.sizeExpr,this.thread).evaluate()).intValue();
        }

        if(cmd.heightExpr!=null) {
            cmd.heightVal = ((Double)new ActionLogicalExpression(cmd.heightExpr,this.thread).evaluate()).floatValue();
        }

        app.ShowHideMiniMap(cmd,this.targetVar, this.targetVarDef);
        return true;
    }

}
