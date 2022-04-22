package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;
import com.scenemaxeng.projector.EntityInstBase;

import java.util.ArrayList;
import java.util.List;

public class FunctionInvocationCommand extends ActionStatementBase {

    public List<SceneMaxParser.Logical_expressionContext> params = new ArrayList<>();
    public String funcName;
    public SceneMaxParser.Logical_expressionContext intervalExpr;
    public EntityInstBase funcParam;
}
