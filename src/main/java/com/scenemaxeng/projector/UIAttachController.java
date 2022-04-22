package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ChannelDrawCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class UIAttachController extends SceneMaxBaseController {

    public UIAttachController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ChannelDrawCommand cmd) {
        super(app, prg, thread, cmd);
    }

    public boolean run(float tpf) {

        if (forceStop) return true;

        if (!targetCalculated) {
            targetCalculated = true;
            //findTargetVar();

            ChannelDrawCommand cmd = (ChannelDrawCommand)this.cmd;
            if(cmd.posXExpr!=null) {
                cmd.posXVal = ((Double)new ActionLogicalExpression(cmd.posXExpr,this.thread).evaluate()).floatValue();
                cmd.posYVal = ((Double)new ActionLogicalExpression(cmd.posYExpr,this.thread).evaluate()).floatValue();

            }

            if(cmd.widthExpr!=null) {
                cmd.widthVal = ((Double)new ActionLogicalExpression(cmd.widthExpr,this.thread).evaluate()).intValue();
                cmd.heightVal = ((Double)new ActionLogicalExpression(cmd.heightExpr,this.thread).evaluate()).intValue();
            }

            if(cmd.frameNumExpr!=null) {
                cmd.frameNumVal = ((Double)new ActionLogicalExpression(cmd.frameNumExpr,this.thread).evaluate()).intValue();
            }

            app.channelDraw(cmd);

        }


        return true;
    }

}
