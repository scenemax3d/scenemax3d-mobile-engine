package com.scenemaxeng.projector;

import com.scenemaxeng.compiler.ModelDef;
import com.scenemaxeng.compiler.VariableDef;

public class ModelInst extends EntityInstBase{

    public ModelDef modelDef;

    public ActionLogicalExpression scaleExpr;
    public ActionLogicalExpression massExpr;
    public ActionLogicalExpression xExpr;
    public ActionLogicalExpression yExpr;
    public ActionLogicalExpression zExpr;
    public ActionLogicalExpression rxExpr;
    public ActionLogicalExpression ryExpr;
    public ActionLogicalExpression rzExpr;
    public RunTimeVarDef entityForPos;
    public RunTimeVarDef entityForRot;

    public ModelInst(ModelDef md, VariableDef varDef, SceneMaxThread thread) {
        this.modelDef=md;
        this.thread=thread;
        this.varDef=varDef;

        if(varDef.scaleExpr!=null) {
            this.scaleExpr = new ActionLogicalExpression(varDef.scaleExpr,thread);
        }

        if(varDef.massExpr!=null) {
            this.massExpr = new ActionLogicalExpression(varDef.massExpr,thread);
        }

        if(varDef.xExpr!=null) {
            this.xExpr=new ActionLogicalExpression(varDef.xExpr,thread);
            this.yExpr=new ActionLogicalExpression(varDef.yExpr,thread);
            this.zExpr=new ActionLogicalExpression(varDef.zExpr,thread);

        }

        if(varDef.useVerbalTurn) {
            if(varDef.rxExpr!=null) {
                this.rxExpr=new ActionLogicalExpression(varDef.rxExpr,thread);
            } else if(varDef.ryExpr!=null) {
                this.ryExpr=new ActionLogicalExpression(varDef.ryExpr,thread);
            } else if(varDef.rzExpr!=null) {
                this.rzExpr=new ActionLogicalExpression(varDef.rzExpr,thread);
            }
        } else if(varDef.rxExpr!=null) {
            this.rxExpr=new ActionLogicalExpression(varDef.rxExpr,thread);
            this.ryExpr=new ActionLogicalExpression(varDef.ryExpr,thread);
            this.rzExpr=new ActionLogicalExpression(varDef.rzExpr,thread);
        }

    }

}
