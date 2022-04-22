package com.scenemaxeng.projector;

import com.jme3.anim.AnimComposer;
import com.scenemaxeng.compiler.AnimateOptionsCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class AnimateOptionsController extends SceneMaxBaseController{

    private Double forTimeVal;
    private Double aboveFramesVal;
    private boolean startCounting = false;
    private AppModel targetModel;

    public AnimateOptionsController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, AnimateOptionsCommand cmd) {
        super(app, prg, thread, cmd);
    }

    public boolean run(float tpf) {

        if (forceStop) return true;

        if(forTimeVal==null) {
            findTargetVar();
            AnimateOptionsCommand cmd = (AnimateOptionsCommand) this.cmd;
            cmd.speedVal = (Double) new ActionLogicalExpression(cmd.speedExpr, this.thread).evaluate();

            if (cmd.forTimeExpr != null) {
                forTimeVal = (Double) new ActionLogicalExpression(cmd.forTimeExpr, this.thread).evaluate();
            } else {
                this.app.applyAnimationOptions(this.targetVar, cmd);
                return true;

            }

            if(cmd.aboveFramesExpr!=null) {
                aboveFramesVal = (Double) new ActionLogicalExpression(cmd.aboveFramesExpr, this.thread).evaluate();
                targetModel = this.app.getAppModel(this.targetVar);
                if(targetModel==null) {
                    return true;
                }
            } else {
                this.app.applyAnimationOptions(this.targetVar, cmd);
                startCounting=true;
            }

        }

        if(aboveFramesVal!=null && !startCounting) {

            AnimComposer ac = targetModel.getAnimComposer();
            if(targetModel.currentAction!=null) {
                double percent = ac.getTime("Default") / targetModel.currentAction.getLength() * 100;
                if (percent>aboveFramesVal) {
                    this.app.applyAnimationOptions(this.targetVar, (AnimateOptionsCommand) this.cmd);
                    startCounting = true;
                }
            }

        }

        if(startCounting) {
            forTimeVal-=tpf;
        } else {
            return false;
        }

        if(forTimeVal<=0) {
            this.app.resetAnimationOptions(this.targetVar);
            return true;
        }

        return false;

    }

}
