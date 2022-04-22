package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ActionCommandAnimate;

public class ModelAnimateController extends SceneMaxBaseController {

    public String speed;
    private boolean animationStarted = false;
    private AppModelAnimationController controller;
    private ActionLogicalExpression speedExpr;
    private boolean reused;
    private ActionCommandAnimate cmdAnim = null;

    public ModelAnimateController(SceneMaxApp app, ActionCommandAnimate cmd, SceneMaxThread thread) {
        this.app=app;
        this.cmd=cmd;
        this.cmdAnim = (ActionCommandAnimate)this.cmd;
        this.thread=thread;
        speedExpr = cmd.speedExpr==null?null:new ActionLogicalExpression(cmd.speedExpr,thread);
        targetVarDef=cmd.varDef;

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
            speed=speedExpr==null?"1":speedExpr.evaluate().toString();

            if(cmd.varDef==null) {
                app.handleRuntimeError("Line: "+cmd.varLineNum+".  variable '"+cmd.targetVar+"' is undefined");
                return true;
            }

            findTargetVar();

            app.animateModel(this.targetVar, ((ActionCommandAnimate)this.cmd).animationName, speed, controller);

        } else {
//            if(!checkGoExpr()) {
//                return true;
//            }
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
