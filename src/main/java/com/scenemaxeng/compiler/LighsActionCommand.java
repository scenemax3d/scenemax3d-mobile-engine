package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

public class LighsActionCommand extends ActionStatementBase {
    public String name;

    public SceneMaxParser.Logical_expressionContext xExpr;
    public SceneMaxParser.Logical_expressionContext yExpr;
    public SceneMaxParser.Logical_expressionContext zExpr;

    public String entityPos;

}
