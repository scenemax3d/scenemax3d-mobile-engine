package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.LighsActionCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class LightsActionController extends SceneMaxBaseController {

    public LightsActionController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, LighsActionCommand cmd) {
        super(app, prg, thread, cmd);
    }

    public boolean run(float tpf) {

        if (forceStop) return true;

        LighsActionCommand cmd=(LighsActionCommand)this.cmd;

        float x=0;
        float y=0;
        float z=0;

        if(cmd.xExpr!=null) {
            x = ((Double) new ActionLogicalExpression(cmd.xExpr, thread).evaluate()).floatValue();
            y = ((Double) new ActionLogicalExpression(cmd.yExpr, thread).evaluate()).floatValue();
            z = ((Double) new ActionLogicalExpression(cmd.zExpr, thread).evaluate()).floatValue();
        }

        app.addLightProbe(cmd.name,x,y,z);

        return true;
    }

}
