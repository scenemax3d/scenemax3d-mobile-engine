package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

import java.util.List;

public class VariableDeclarationCommand extends StatementDef {

    public String varName;
    public SceneMaxParser.Logical_expressionContext valExpr;
    public List<VariableDeclarationCommand> siblings;

    public List<SceneMaxParser.Logical_expressionContext> array;

}
