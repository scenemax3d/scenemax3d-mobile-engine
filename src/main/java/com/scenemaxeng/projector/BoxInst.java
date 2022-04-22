package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.BoxVariableDef;

public class BoxInst extends ModelInst{

    public ActionLogicalExpression materialExpr;

    public BoxInst(BoxVariableDef varDef, SceneMaxThread thread) {
        super(null,varDef,thread);

    }
}
