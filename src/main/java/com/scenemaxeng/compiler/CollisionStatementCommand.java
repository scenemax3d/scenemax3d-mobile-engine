package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

public class CollisionStatementCommand extends ActionStatementBase {

    public DoBlockCommand doBlock;

    public VariableDef varDef1;
    public VariableDef varDef2;
    public String part1="";
    public String part2="";
    public SceneMaxParser.Logical_expressionContext goExpr;
}
