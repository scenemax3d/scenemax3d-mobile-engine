package com.scenemaxeng.projector;

import com.jme3.scene.Spatial;
import com.scenemaxeng.compiler.GraphicEntityCreationCommand;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableDef;

import java.util.concurrent.*;

public class InstantiateGraphicEntityController extends SceneMaxBaseController {

    private GraphicEntityCreationCommand cmd;
    private int status = 0;

    public InstantiateGraphicEntityController(SceneMaxApp app, ProgramDef prg, GraphicEntityCreationCommand cmd, SceneMaxThread thread) {
        this.app=app;
        this.prg=prg;
        this.cmd=cmd;
        this.thread=thread;
    }

    @Override
    public boolean run(float tpf) {
        if (status == 0) {
            if (this.isSharedEntityExists()) {
                return true;
            }
            if (cmd.varDef.varType == VariableDef.VAR_TYPE_3D && cmd.isAsync) {
                status = 1; // loading
                load3DModelAsync();
            } else {
                app.instantiateVariable(prg, cmd.varDef, thread);
                return true;
            }
        }

        return status == 2; // 2 == Java thread finished
    }

    private boolean isSharedEntityExists() {
        VariableDef vd = this.prg.vars_index.get(cmd.varDef.varName);
        if (vd !=null && vd.isShared) {
            if (vd.varType == VariableDef.VAR_TYPE_3D) {
                return this.thread.models.containsKey(cmd.varDef.varName);
            } if (vd.varType == VariableDef.VAR_TYPE_BOX) {
                return this.thread.boxes.containsKey(cmd.varDef.varName);
            } if (vd.varType == VariableDef.VAR_TYPE_SPHERE) {
                return this.thread.spheres.containsKey(cmd.varDef.varName);
            }
        }

        return false;
    }

    private void load3DModelAsync() {

        final ModelInst[] inst = {null};
        final Spatial[] node = {null};
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            inst[0] = app.instantiate3DModelAsync(prg, cmd.varDef, thread);
            node[0] = app.loadModelSpatial(cmd.varDef.varName, inst[0].modelDef.name, inst[0]);
            return true;
        }, app.getExecutorService());

        future.thenAccept(result -> {
            app.enqueue(() -> {
                app.attachModelSpatial(node[0], inst[0]);
                this.status = 2;
            });
        });

        //
    }

}
