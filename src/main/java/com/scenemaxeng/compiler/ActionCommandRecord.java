package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

public class ActionCommandRecord extends VariableActionStatement {


    public static final int RECORD_TYPE_TRANSITIONS = 10;
    public static final int RECORD_TYPE_COMMANDS = 20;
    public static final int RECORD_TYPE_STOP = 30;
    public static final int RECORD_TYPE_SAVE = 40;

    public SceneMaxParser.Logical_expressionContext everyTimeExpr;
    public int recordType;
    public String savePath;

    @Override
    public boolean validate(ProgramDef prg) {

        this.varDef = prg.getVar(targetVar);
        return (this.varDef!=null);

    }

}
