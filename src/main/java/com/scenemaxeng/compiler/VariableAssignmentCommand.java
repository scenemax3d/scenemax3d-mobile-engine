package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

public class VariableAssignmentCommand extends ActionStatementBase {

    public ParserRuleContext expression;
    public VariableDef var;
    public List<SceneMaxParser.Logical_expressionContext> array;

}
