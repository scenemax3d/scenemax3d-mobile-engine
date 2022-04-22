package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ActionCommandPlay;
import com.scenemaxeng.compiler.VariableDef;

class SpritePlayFramesController extends SceneMaxBaseController{

    private final SceneMaxApp app;
    private int fromFrame = 0;
    private int toFrame = 0;
    private float passedTime = 0;
    private float targetTime=0;
    private float passedDuration=0;
    private float targetDuration=0;

    private ActionCommandPlay cmd = null;
    private int loopCounter;
    private int targetLoop = -1;
    private boolean targetCalculated=false;
    private SceneMaxThread thread;
    private String targetVar;


    public SpritePlayFramesController(SceneMaxApp app, SceneMaxThread thread, ActionCommandPlay cmd) {
        this.cmd=cmd;
        this.thread=thread;
        this.app=app;
    }


    @Override
    public void init() {

        if(cmd.durationStrategy==2) {//loop
            loopCounter = 0;
            targetLoop = Integer.parseInt(cmd.loopTimes);
        }

    }


    @Override
    public boolean run(float tpf) {

        if(!targetCalculated) {
            if(cmd.durationStrategy==1) {//play for amount of time
                targetDuration = cmd.forTimeExpr==null?1.0f:
                        Float.parseFloat(new ActionLogicalExpression(cmd.forTimeExpr,thread).evaluate().toString());//     Float.parseFloat(this.forTime);
            }

            targetTime = cmd.speedExpr==null?1.0f:Float.parseFloat(new ActionLogicalExpression(cmd.speedExpr,thread).evaluate().toString());

            fromFrame = (int)Float.parseFloat(new ActionLogicalExpression(this.cmd.fromFrameExpr,thread).evaluate().toString());//
            toFrame = (int)Float.parseFloat(new ActionLogicalExpression(this.cmd.toFrameExpr,thread).evaluate().toString());//


            if(cmd.varDef.varType== VariableDef.VAR_TYPE_OBJECT) {
                EntityInstBase obj = (EntityInstBase) thread.getFuncScopeParam(cmd.varDef.varName);
                if(obj==null) {
                    app.handleRuntimeError("Function argument '"+cmd.varDef.varName+"' is undefined");
                    return true;
                }
                targetVar = obj.varDef.varName + "@" + obj.thread.threadId;

            } else {
                int threadId = app.getEntityThreadId(thread, cmd.targetVar);
                this.targetVar = cmd.varDef.varName + "@" + threadId;//cmd.targetVar;
            }

            targetCalculated=true;
        }

        float ratio=1;
        passedTime+=tpf;

        if(passedTime>=targetTime){

            passedTime=0;

            if(cmd.durationStrategy==0) {//Once
                return true;// finish animation
            } else if(cmd.durationStrategy==2) {//loop
                loopCounter++;
                if(targetLoop!=-1 && loopCounter>=targetLoop){
                    return true;
                }
            }


        } else {
            ratio=passedTime/targetTime;
        }

        float frame = fromFrame+ratio*(toFrame+1-fromFrame);
        if(frame>toFrame){
            frame=toFrame;
        }

        app.spritePlayFrames(this.targetVar, (int) frame, thread);

        if(cmd.durationStrategy==1) {//time
            passedDuration+=tpf;
            if (passedDuration >= targetDuration) {
                return true;
            }
        }

        return false;//continue to play frames

    }
}
