package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.CreateSpriteCommand;
import com.scenemaxeng.compiler.ProgramDef;

public class CreateSpriteController extends SceneMaxBaseController {

    public CreateSpriteController(SceneMaxApp app, ProgramDef prg, CreateSpriteCommand cmd, SceneMaxThread thread) {
        this.app = app;
        this.prg = prg;
        this.thread = thread;
        this.cmd = cmd;
    }

    @Override
    public boolean run(float tpf) {

        CreateSpriteCommand cmd = (CreateSpriteCommand) this.cmd;
//        String spriteResName = null;
//        SpriteDef def;
//
        if (cmd.spriteDef.nameExpr != null) {
            cmd.varDef.resName = new ActionLogicalExpression(cmd.spriteDef.nameExpr, thread).evaluate().toString();
        }

        SpriteInst inst = app.createSpriteInst(thread, cmd);
        thread.sprites.put(cmd.varDef.varName, inst);
        app.loadSprite(inst);

        return true;
    }

}
