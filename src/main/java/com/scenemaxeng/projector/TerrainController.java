package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.TerrainCommand;

public class TerrainController extends SceneMaxBaseController {

    private final TerrainCommand cmd;
    private SceneMaxApp app;
    private ProgramDef prg;
    private SceneMaxThread thread;

    public TerrainController(SceneMaxApp app, ProgramDef prg, SceneMaxThread thread, TerrainCommand cmd) {
        this.app=app;
        this.prg=prg;
        this.thread=thread;
        this.cmd=cmd;

    }

    public boolean run(float tpf) {
        if (forceStop) return true;
        ActionLogicalExpression exp = new ActionLogicalExpression(cmd.terrainNameExprCtx,this.thread);
        String asset = exp.evaluate().toString();
        app.loadTerrain(asset);

        return true;
    }


}
