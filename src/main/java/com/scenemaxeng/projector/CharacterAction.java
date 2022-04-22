package com.scenemaxeng.projector;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.action.BaseAction;

public class CharacterAction extends BaseAction {

    private AppModelAnimationController controller;
    AnimComposer ac;

    public CharacterAction(AppModelAnimationController controller, Tween delegate, AnimComposer ac) {
        super(delegate);
        this.ac = ac;
        this.controller=controller;

    }

    @Override
    public boolean interpolate(double t) {
        boolean running = super.interpolate(t);
        if (!running) {
            //ac.removeCurrentAction(AnimComposer.DEFAULT_LAYER);
            this.setSpeed(0);
            this.controller.animationFinished = true;
            this.controller.appModel.currentAction=null;
        }
        return running;
    }

    public void finishAnimation() {
        this.controller.animationFinished = true;
    }

    public void setController(AppModelAnimationController ctl) {
        this.controller=ctl;
    }

    public SceneMaxBaseController getHostController() {
        return this.controller.hostController;
    }

}