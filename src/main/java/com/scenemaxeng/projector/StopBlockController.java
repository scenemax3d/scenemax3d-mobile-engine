package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.StopBlockCommand;

class StopBlockController extends SceneMaxBaseController{

    private final SceneMaxApp app;
    private final SceneMaxThread thread;
    private final ProgramDef prg;
    private final StopBlockCommand cmd;

    public StopBlockController(SceneMaxApp app, ProgramDef prg, StopBlockCommand cmd, SceneMaxThread thread) {
        this.app=app;
        this.thread=thread;
        this.prg=prg;
        this.cmd=cmd;
    }

    @Override
    public boolean run(float tpf) {

        // return action just return from the parent do-end do scope
        if(cmd.returnAction) {
            SceneMaxThread t = thread.getFirstReturnPointThread();
            if(t!=null) {
                t.forceStop();
            } else {
                // didn't find any hosting procedure so stop this one
                t = thread.getSecondLevelReturnPointThread();
                if(t!=null) {
                    t.forceStop();
                } else {
                    thread.forceStop();
                }
            }
            return true;
        }


        // stop - kills the repetition timer
        SceneMaxThread t = thread.getFirstLooperThread();
        SceneMaxBaseController ctl = t.getCreatorController();
        if(ctl instanceof CompositeController) {
            ((CompositeController)ctl).forceStop();
        } else {
            ctl.forceStop = true;
        }



//        SceneMaxThread t = thread.getFirstLooperThread();
//        if(t!=null) {
//
//            // return action just return from the parent do-end do scope
//            if(cmd.returnAction) {
//                t.forceStop();
//                return true;
//            }
//
//            // stop - kills the repetition timer
//            SceneMaxBaseController ctl = t.getCreatorController();
//            if(ctl instanceof CompositeController) {
//                ((CompositeController)ctl).forceStop();
//            } else {
//                ctl.forceStop = true;
//            }
//
//        }
        return true;
    }

}
