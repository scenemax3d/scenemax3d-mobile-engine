package com.scenemaxeng.projector;

import com.abware.scenemaxlang.parser.SceneMaxParser;
import com.scenemaxeng.compiler.VariableDef;

class FunctionInvoker {

    private SceneMaxParser.Function_valueContext ctx;
    private SceneMaxApp app;
    private SceneMaxThread thread;

    public Object retval;
    public int retvalType = VariableDef.VAR_TYPE_NUMBER;
    public String runtimeError;

    public FunctionInvoker(SceneMaxParser.Function_valueContext ctx, SceneMaxApp app, SceneMaxThread thread) {
        this.ctx=ctx;
        this.app=app;
        this.thread=thread;
    }

    public boolean invoke() {
        String funcName = ctx.java_func_name().getText().toLowerCase();
        if(funcName.equals("rnd")) {
            return invokeRnd();
        } else if(funcName.equals("floor")) {
            return invokeFloor();
        } else if(funcName.equals("ceil")) {
            return invokeCeiling();
        } else if(funcName.equals("abs")) {
            return invokeAbs();
        } else if(funcName.equals("round")) {
            return invokeRound();
        } else if(funcName.equals("sin")) {
            return invokeSin();
        } else if(funcName.equals("cos")) {
            return invokeCos();
        }

        this.runtimeError = "Function '"+funcName+"' is not supported";
        return false;
    }

    private boolean invokeCos() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();
            retval = Math.cos((double) val);
            return true;
        } else {
            this.runtimeError = "Function '"+ctx.java_func_name().getText()+"' expecting argument";
            return false;
        }
    }

    private boolean invokeSin() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();
            retval = Math.sin((double) val);
            return true;
        } else {
            this.runtimeError = "Function '"+ctx.java_func_name().getText()+"' expecting argument";
            return false;
        }
    }

    private boolean invokeRound() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();
            retval = Math.round((double) val);
            return true;
        } else {
            this.runtimeError = "Function '"+ctx.java_func_name().getText()+"' expecting argument";
            return false;
        }
    }

    private boolean invokeAbs() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();
            retval = Math.abs((double) val);
            return true;
        } else {
            this.runtimeError = "Function '"+ctx.java_func_name().getText()+"' expecting argument";
            return false;
        }
    }

    private boolean invokeCeiling() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();
            retval = Math.ceil((double) val);
            return true;
        } else {
            this.runtimeError = "Function '"+ctx.java_func_name().getText()+"' expecting argument";
            return false;
        }
    }

    private boolean invokeFloor() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();
            retval = Math.floor((double) val);
            return true;
        } else {
            this.runtimeError = "Function '"+ctx.java_func_name().getText()+"' expecting argument";
            return false;
        }
    }


    private boolean invokeRnd() {
        if(ctx.logical_expression().size()>0) {
            ActionLogicalExpression exp = new ActionLogicalExpression(ctx.logical_expression(0), thread);
            Object val = exp.evaluate();

            Double d = (Math.random() * (double) val);
            retval = Math.floor(d);
            return true;
        } else {
            retval = Math.random();
            return true;
        }

    }

}
