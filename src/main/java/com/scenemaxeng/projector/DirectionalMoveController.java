package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.DirectionalMoveCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class DirectionalMoveController extends SceneMaxBaseController{

    private boolean targetCalculated=false;
    private float targetTime = -1;
    private boolean paused = false;
    private Double dist;

    public DirectionalMoveController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, DirectionalMoveCommand cmd) {
        super(app, prg, thread, cmd);
        this.adhereToPauseStatus=false;
    }

    public boolean run(float tpf) {
        if (forceStop) return true;
        if (app.scenePaused && targetCalculated) {
            this.app.moveDirectional(this.targetVar, DirectionalMoveCommand.FORWARD, 0.0);
            this.paused = true;
            return false;
        }

        if (this.paused && !app.scenePaused) {
            this.paused = false;
            this.app.moveDirectional(this.targetVar, DirectionalMoveCommand.FORWARD, dist);
            return false;
        }

        if (!targetCalculated) {

            DirectionalMoveCommand cmd = (DirectionalMoveCommand) this.cmd;

            targetCalculated = true;
            findTargetVar();

            //Double dist = null;

            if (cmd.distanceExpr != null) {
                dist = (Double) new ActionLogicalExpression(cmd.distanceExpr, this.thread).evaluate();
            }
            this.app.moveDirectional(this.targetVar, cmd.direction, dist);

            if (cmd.timeExpr != null) {
                this.targetTime = ((Double) new ActionLogicalExpression(cmd.timeExpr, this.thread).evaluate()).floatValue();
                return false;
            } else {
                return true;
            }

        }

        this.targetTime -= tpf;
        boolean stop = (this.targetTime <= 0);
        if (stop) {
            this.app.moveDirectional(this.targetVar, DirectionalMoveCommand.FORWARD, 0.0);
        }

        return stop;


    }
}
