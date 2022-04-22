package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.SkyBoxCommand;

public class SkyBoxActionController extends SceneMaxBaseController{

    private final SceneMaxApp app;
    private final SceneMaxThread thread;
    private final SkyBoxCommand cmd;
    private final ProgramDef prg;

    public SkyBoxActionController(SceneMaxApp app, ProgramDef prg, SkyBoxCommand cmd, SceneMaxThread thread) {
        this.app=app;
        this.thread=thread;
        this.cmd=cmd;
        this.prg=prg;
    }

    public boolean run(float tpf)
    {
        if(cmd.isShow) {
            if(!cmd.isShowSolarSystem) {
                String skyboxMaterial = cmd.showExpr;//new ActionLogicalExpression(cmd.showExpr, thread).evaluate().toString();
                app.showSkyBox(skyboxMaterial);
            } else {
                evalSetupVars(cmd);
                app.showSolarSystemSkyBox(cmd);
            }
        } else if(cmd.isSetup) {
            evalSetupVars(cmd);
            app.setupSkyControl(cmd);
        }
        return true;
    }

    private void evalSetupVars(SkyBoxCommand cmd) {
        if(cmd.cloudinessExpr!=null) {
            cmd.cloudinessVal = (Double)new ActionLogicalExpression(cmd.cloudinessExpr,thread).evaluate();
        }

        if(cmd.cloudFlatteningExpr!=null) {
            cmd.cloudFlatteningVal = (Double)new ActionLogicalExpression(cmd.cloudFlatteningExpr,thread).evaluate();
        }

        if(cmd.hourOfDayExpr!=null) {
            cmd.hourOfDayVal = (Double)new ActionLogicalExpression(cmd.hourOfDayExpr,thread).evaluate();

        }


    }


}
