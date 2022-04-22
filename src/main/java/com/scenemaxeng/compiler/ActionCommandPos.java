package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

public class ActionCommandPos extends ActionStatementBase {

    public VariableDef varDef;
    public String varName;
    public SceneMaxParser.Logical_expressionContext x;
    public SceneMaxParser.Logical_expressionContext y;
    public SceneMaxParser.Logical_expressionContext z;
    public String entityPos;
    public PositionStatement posStatement;

    @Override
    public boolean validate(ProgramDef prg) {

        this.varDef = prg.getVar(varName);
        return (this.varDef!=null);

    }

}
