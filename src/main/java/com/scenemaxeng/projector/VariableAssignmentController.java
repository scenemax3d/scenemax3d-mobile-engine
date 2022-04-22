package com.scenemaxeng.projector;

import com.abware.scenemaxlang.parser.SceneMaxParser;
import com.scenemaxeng.compiler.ProgramDef;
import com.scenemaxeng.compiler.VariableAssignmentCommand;
import com.scenemaxeng.compiler.VariableDef;

import java.util.ArrayList;
import java.util.List;

public class VariableAssignmentController extends CompositeController {

    private VariableAssignmentCommand varAssignmentCommand;

    public VariableAssignmentController(SceneMaxApp app, SceneMaxThread thread, ProgramDef prg,VariableAssignmentCommand cmd) {
        super(app,prg,thread,cmd);
        this.varAssignmentCommand=cmd;

    }

    @Override
    public boolean run(float tpf) {

        // evaluate expression once
        if(this.size()==0) {

            if(varAssignmentCommand.array!=null) {
                return assignArray();
            } else {
                return assignSingleVar();
            }

        }

        // run until finish
        return super.run(tpf);

    }

    private boolean assignArray() {

        VarInst var = addVarIndex();
        if(var==null) {
            return true;
        }

        var.values = new ArrayList<>();
        for(SceneMaxParser.Logical_expressionContext logExp: varAssignmentCommand.array) {
            Object retval = new ActionLogicalExpression(logExp,thread).evaluate();
            var.values.add(retval);
        }

        var.varType=VariableDef.VAR_TYPE_ARRAY;

        return true;

    }

    protected boolean assignSingleVar() {

        VarInst var = addVarIndex();
        if(var==null) {
            return true;
        }


        Object retval = new ActionLogicalExpression(varAssignmentCommand.expression,thread).evaluate();

        if(retval instanceof List) {
            var.values= (List<Object>) retval;
            var.varType=VariableDef.VAR_TYPE_ARRAY;
            return true;
        } else {
            var.value = retval;
        }

        if(var.varDef.varType==0 || var.varDef.varType==VariableDef.VAR_TYPE_OBJECT) {
            if(retval instanceof String) {
                var.varType=VariableDef.VAR_TYPE_STRING;
            } else if(retval instanceof Double) {
                var.varType=VariableDef.VAR_TYPE_NUMBER;
            } else if(retval instanceof EntityInstBase) {
                var.varReference=((EntityInstBase) retval).varDef;
            }
        }

        return true;
    }

    private VarInst addVarIndex() {
        VarInst var = thread.getVar(varAssignmentCommand.var.varName);
        if(var==null) {
            VariableDef vd = prg.getVar(varAssignmentCommand.var.varName);
            if(vd==null) {
                app.handleRuntimeError("Variable '"+varAssignmentCommand.var.varName+"' is not defined");
                return null;
            }
            var =new VarInst(vd,thread);
            thread.vars_index.put(varAssignmentCommand.var.varName,var);

        }

        return var;
    }

}
