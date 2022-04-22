package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.GraphicEntityCreationCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class InstantiateGraphicEntityController extends SceneMaxBaseController {

    private GraphicEntityCreationCommand cmd;

    public InstantiateGraphicEntityController(SceneMaxApp app, ProgramDef prg, GraphicEntityCreationCommand cmd, SceneMaxThread thread) {
        this.app=app;
        this.prg=prg;
        this.cmd=cmd;
        this.thread=thread;
    }

    @Override
    public boolean run(float tpf) {
        app.instantiateVariable(prg,cmd.varDef,thread);
        return true;
    }

}
