package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.SphereVariableDef;

public class SphereInst extends ModelInst {

    public ActionLogicalExpression radiusExpr;
    public ActionLogicalExpression materialExpr;

    public SphereInst(SphereVariableDef varDef, SceneMaxThread thread) {
        super(null,varDef,thread);

        if(varDef.radiusExpr!=null) {
            this.radiusExpr=new ActionLogicalExpression(varDef.radiusExpr,thread);
        }

        if(varDef.materialExpr!=null) {
            this.materialExpr=new ActionLogicalExpression(varDef.materialExpr,thread);
        }

    }
}
