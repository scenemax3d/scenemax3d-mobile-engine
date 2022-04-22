package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.WaitStatementCommand;

public class WaitStatementController extends SceneMaxBaseController {

    private final SceneMaxApp app;
    private final ProgramDef prg;
    private final WaitStatementCommand cmd;
    private boolean targetCalculated=false;
    private float targetTime;
    private float passedTime = 0;
    private SceneMaxThread thread;

    public WaitStatementController(SceneMaxApp app, ProgramDef prg, WaitStatementCommand cmd, SceneMaxThread thread) {
        this.app=app;
        this.prg=prg;
        this.cmd=cmd;
        this.thread=thread;

        this.adhereToPauseStatus=false; // allow timer even when scene is paused

    }

    public boolean run(float tpf)
    {
        if(!targetCalculated) {

            if(cmd.explicitWaitTime>0) {
                targetTime=cmd.explicitWaitTime;
            } else {
                targetTime = cmd.waitExpr == null ? 1.0f : Float.parseFloat(new ActionLogicalExpression(cmd.waitExpr, thread).evaluate().toString());
            }

            targetCalculated=true;
        }

        boolean finished = false;
        passedTime+=tpf;
        if(passedTime>=targetTime) {
            finished=true;
        }

        return finished;

    }

}
