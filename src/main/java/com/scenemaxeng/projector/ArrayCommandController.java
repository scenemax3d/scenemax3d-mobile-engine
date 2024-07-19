package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.ArrayCommand;

public class ArrayCommandController extends SceneMaxBaseController {

    private ArrayCommand cmd;

    public ArrayCommandController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ArrayCommand cmd) {
        super(app, prg, thread, cmd);
        this.cmd = cmd;
    }

    public boolean run(float tpf) {
        this.findTargetVar();
        VarInst var = thread.getVar(this.cmd.varName);
        if (var == null) {
            return false;
        }

        switch (this.cmd.action) {
            case Push:
                Object obj = new ActionLogicalExpression(this.cmd.expr, this.thread).evaluate();
                var.values.add(obj);
                break;
            case Pop:
                if(var.values.size() > 0) {
                    var.values.remove(var.values.size() - 1);
                }
                break;
            case Clear:
                var.values.clear();
                break;
        }

        return true;
    }

}
