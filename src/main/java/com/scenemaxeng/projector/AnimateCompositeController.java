package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ActionCommandAnimate;

public class AnimateCompositeController extends CompositeController{

    private boolean loopAnimations;

    public AnimateCompositeController(ActionCommandAnimate cmd) {
        loopAnimations = cmd.loop;
        this.adhereToPauseStatus=false;
    }

    @Override
    public boolean run(float tpf) {

        if(_controllers.size()==0) return true;

        SceneMaxBaseController ctl = _controllers.get(runningControllerIndex);
        boolean finished = false;
        boolean async = ctl.async;
        if(async) {
            this.app.registerController(ctl);
        } else {
            finished = ctl.run(tpf);
        }

        if(finished || async) {

            runningControllerIndex++;
            if(runningControllerIndex < _controllers.size()) {
                return false;
            } else {
                if(loopAnimations) {

                    for(SceneMaxBaseController c: _controllers) {
                        ((ModelAnimateController)c).reuse();
                    }
                    runningControllerIndex=0;
                    return false;
                }
                return true; // no more controllers to run
            }

        }

        return false; // current controller not finished

    }

    public void stopAnimationLoop() {
        _controllers.clear();
    }

}
