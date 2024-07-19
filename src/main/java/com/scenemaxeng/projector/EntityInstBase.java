package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.VariableDef;

public class EntityInstBase {

    public VariableDef varDef;
    public SceneMaxThread thread;
    public String entityKey;
    public float thresholdX=100;

    public String getVarRunTimeName() {
        return this.varDef.varName + "@" + this.thread.threadId;
    }
}
