
package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxParser;

public class SpriteDef extends StatementDef {
    public String name = null;
    public String varName = null;

    public int rows = 0;
    public int cols=0;

    public SceneMaxParser.Logical_expressionContext nameExpr;
    public SceneMaxParser.Logical_expressionContext xExpr;
    public SceneMaxParser.Logical_expressionContext yExpr;
    public SceneMaxParser.Logical_expressionContext zExpr;
    public EntityPos entityPos;
    public SceneMaxParser.Logical_expressionContext scaleExpr;

    public boolean isBillboard;

}
