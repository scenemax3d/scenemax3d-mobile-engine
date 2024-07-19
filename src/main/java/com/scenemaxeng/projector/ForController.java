package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ForCommand;
import com.scenemaxeng.compiler.FunctionInvocationCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class ForController extends CompositeController {

    private ForCommand cmd;
    private DoBlockController doBlock;

    public ForController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, ForCommand cmd) {
        super(app, prg, thread, cmd);
        this.cmd = cmd;
    }

    public boolean run(float tpf) {

        if (forceStop) return true;
        if (!targetCalculated) {
            targetCalculated = true;

            VariableAssignmentController ctl = new VariableAssignmentController(this.app,thread,prg,this.cmd.declareIndexCommand);
            ctl.run(tpf);

            this.doBlock = new DoBlockController(this.app, this.thread, this.cmd.doBlock);
            this.doBlock.async = this.cmd.isAsync;
            this.add(this.doBlock);
        }

        Boolean continueLoop = (Boolean) new ActionLogicalExpression(this.cmd.stopConditionExpr, thread).evaluate();
        if (!continueLoop) {
            return true; // finish loop
        }

        boolean bodyFinished = super.run(tpf);
        if (bodyFinished) {
            // increment loop index
            VariableAssignmentController ctl = new VariableAssignmentController(this.app, thread, prg, this.cmd.incrementIndexCommand);
            ctl.run(tpf);

            this.doBlock.reset();
            this.runningControllerIndex = 0;
        }

        return false;
    }

}
