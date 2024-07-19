package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ActionCommandAnimate;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableDef;

public class ModelAnimateController extends SceneMaxBaseController {

    public String speed;
    private boolean animationStarted = false;
    private AppModelAnimationController controller;
    private ActionLogicalExpression speedExpr;
    private boolean reused;
    private ActionCommandAnimate cmdAnim = null;

    public ModelAnimateController(SceneMaxApp app, ProgramDef prg, ActionCommandAnimate cmd, SceneMaxThread thread) {
        super(app, prg, thread, cmd);
        this.cmdAnim = (ActionCommandAnimate)this.cmd;
        speedExpr = cmd.speedExpr==null?null:new ActionLogicalExpression(cmd.speedExpr,thread);
        this.adhereToPauseStatus=false;
    }


    @Override
    public boolean run(float tpf) {

        if(controller!=null && controller.isPaused()) {
            if (!app.scenePaused) {
                controller.resume();
                return false;
            } else {
                return false;
            }
        }

        if(animationStarted && app.scenePaused) {
            controller.pause();
            return false;
        }


        if(!animationStarted ) {

            if(!checkGoExpr()) {
                return true;
            }

            animationStarted=true;
            controller=new AppModelAnimationController(this);
            controller.isProtected = this.cmdAnim.isProtected;
            speed=speedExpr==null?"1":speedExpr.evaluate().toString();

            if(cmd.varDef==null) {
                app.handleRuntimeError("Line: "+cmd.varLineNum+".  variable '"+cmd.targetVar+"' is undefined");
                return true;
            }

            findTargetVar();

            app.animateModel(this.targetVar, ((ActionCommandAnimate)this.cmd).animationName, speed, controller);

        } else {
            if(reused) {
                this.reused=false;
                app.animateModel(this.targetVar, ((ActionCommandAnimate)this.cmd).animationName, speed, controller);
            }
        }

        return controller.animationFinished;
    }

    public boolean checkGoExpr() {

        if(cmdAnim.goExpr!=null) {
            Object cond = new ActionLogicalExpression(cmdAnim.goExpr,this.thread).evaluate();
            if(cond instanceof Boolean) {
                return (Boolean)cond;

            }
        }

        return true;

    }

    public void reuse() {
        if(controller!=null) {
            controller.animationFinished = false;
            this.reused = true;
        }

    }

}
