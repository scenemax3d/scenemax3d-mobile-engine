package com.scenemaxeng.projector;

import com.abware.scenemaxlang.parser.SceneMaxBaseVisitor;
import com.abware.scenemaxlang.parser.SceneMaxParser;
import com.scenemaxeng.compiler.ActionStatementBase;
import com.scenemaxeng.compiler.VariableDef;

import org.antlr.v4.runtime.ParserRuleContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ActionLogicalExpression extends ActionStatementBase {

    SceneMaxThread thread;
    ParserRuleContext ctx;
    static SceneMaxApp app;


    public ActionLogicalExpression(ParserRuleContext ctx, SceneMaxThread thread) {
        this.thread=thread;
        this.ctx=ctx;
    }

    public static void setApp(SceneMaxApp app) {
        ActionLogicalExpression.app=app;
    }

    public Object evaluate() {
        LogicalExpressionVisitor v = new LogicalExpressionVisitor(this.thread);
        ctx.accept(v);
        return v.getEvalResult();

    }

    private static class LogicalExpressionVisitor extends SceneMaxBaseVisitor<Object> {
        private static final String NEW_LINE_STRING = "newline";

        private final SceneMaxThread thread;
        private Object res;
        private Double retval = 0.0;
        private String retvalStr="";
        private Object retvalObj=null;
        private boolean isResBool=false;
        private Boolean retvalBool;
        private boolean isResString=false;
        private boolean isObject=false;
        private boolean hasRuntimeError=false;
        private boolean hasNumericRetval=false;

        public LogicalExpressionVisitor(SceneMaxThread thread) {
            this.thread=thread;
        }

        public Object getEvalResult() {
            return res;
        }

        public Object visitLogical_expression(SceneMaxParser.Logical_expressionContext ctx) {

            boolean retval=false;

            for(int i=0;i<ctx.getChildCount();++i) {
                ctx.getChild(i).accept(this);

                if(res instanceof Boolean) {
                    retval = (retval || (Boolean) res);
                }

                if(isObject) {
                    break;
                }

                // if even just one expression is true then OK and no need to continue checking
                if(retval) {
                    break;
                }
            }

            return retval;
        }

        public Object visitBooleanAndExpression(SceneMaxParser.BooleanAndExpressionContext ctx) {

            boolean retval=true;

            for(int i=0;i<ctx.getChildCount();++i) {
                ctx.getChild(i).accept(this);

                if(res instanceof Boolean) {
                    retval = (retval && (Boolean) res);
                } else if(isObject) {
                    break;
                }

                // if even just one expression is not true then OK and no need to continue checking
                if(!retval) {
                    break;
                }
            }

            return retval;
        }

        public Object visitRelationalExpression(SceneMaxParser.RelationalExpressionContext ctx) {

            Double left = 0.0;
            Boolean retval = true;
            String leftStr="";
            String sign = "";

            for(int i=0;i<ctx.getChildCount();++i) {
                if(i%2==0) {
                    ctx.getChild(i).accept(this);

                    if(isObject) {
                        return true;
                    }

                    if(sign.equals("==")) {
                        if(isResString) {
                            retval = leftStr.equals(res.toString());
                        } else {
                            retval = left.equals(Double.parseDouble(res.toString()));
                        }
                    } else if(sign.equals("!=") || sign.equals("<>")) {
                        if(isResString) {
                            retval = !leftStr.equals(res.toString());
                        } else {
                            retval = !left.equals(Double.parseDouble(res.toString()));
                        }
                    } else if(sign.equals(">")) {
                        if(isResString) {

                        } else {
                            retval = left > Double.parseDouble(res.toString());
                        }
                    } else if(sign.equals(">=")) {
                        if(isResString) {

                        } else {
                            retval = left >= Double.parseDouble(res.toString());
                        }
                    } else if(sign.equals("<")) {
                        if(isResString) {

                        } else {
                            retval = left < Double.parseDouble(res.toString());
                        }
                    } else if(sign.equals("<=")) {
                        if(isResString) {

                        } else {
                            retval = left <= Double.parseDouble(res.toString());
                        }
                    } else {
                        if(isResString) {
                            leftStr = res.toString();
                        } else {
                            if(res instanceof Boolean) {
                                retval=(Boolean)res;
                            } else {
                                left = Double.parseDouble(res.toString());
                            }
                        }
                    }
                } else {
                    sign=ctx.getChild(i).getText();
                }
            }

            // only if there was a equality sign involve return boolean otherwise return the result number itself
            if(sign.length()>0) {
                isResBool=true;
                isResString=false;
                res = retval;
            }

            return retval;

        }

        public Object visitMultiplicativeExpression(SceneMaxParser.MultiplicativeExpressionContext ctx) {

            Double retval = 0.0;
            String sign = "";

            for(int i=0;i<ctx.getChildCount();++i) {
                if(i%2==0) {
                    ctx.getChild(i).accept(this);
                    if(sign.equals("*")) {
                        retval*=Double.parseDouble(res.toString());
                    } else if(sign.equals("/")) {
                        retval/=Double.parseDouble(res.toString());
                    } else if(sign.equals("%")) {
                        retval=retval%Double.parseDouble(res.toString());
                    } else {
                        if(res!=null) {
                            if(res instanceof Double) {
                                retval = (Double)res;// Double.parseDouble(res.toString());
                            } else if(res instanceof String) {
                                return res.toString();
                            } else {
                                return res;
                            }


                        } else {
                            retval=0.0;
                        }
                    }
                } else {
                    sign=ctx.getChild(i).getText();
                }
            }

            res=retval;
            return retval;

        }

        public Object visitAdditiveExpression(SceneMaxParser.AdditiveExpressionContext ctx) {

            retval = 0.0;
            retvalStr="";

            String sign = "";

            for(int i=0;i<ctx.getChildCount();++i) {
                if(i%2==0) {
                    ctx.getChild(i).accept(this);
                    if(sign.equals("+")) {
                        if(!hasRuntimeError) {
                            if (isResString) {
                                String addStr = res.toString();
                                if(addStr.endsWith(".0")) {
                                    addStr=addStr.replace(".0","");
                                }
                                retvalStr = retvalStr.concat(addStr);
                            } else {
                                retval += Double.parseDouble(res.toString());
                            }
                        }
                    } else if(sign.equals("-")) {
                        if(!hasRuntimeError) {
                            if (isResString) {
                                app.handleRuntimeError("Line: " + ctx.start.getLine() + ", Invalid '-' operator in String expression");
                            } else {
                                retval -= Double.parseDouble(res.toString());
                            }
                        }
                    } else {
                        if(isResString) {
                            retvalStr = res.toString();
                        } else if(isObject) {
                            retvalObj = res;
                        } else if(res instanceof Boolean) {
                            retvalBool = (Boolean)res;
                        } else if(!hasRuntimeError) {
                            hasNumericRetval = true;
                            retval = Double.parseDouble(res.toString());
                        }
                    }
                } else {
                    sign=ctx.getChild(i).getText();
                }
            }

            if(isResString) {
                res=retvalStr;
                return retvalStr;
            } else if(isObject) {
                return retvalObj;
            } else if(isResBool) {
                return retvalBool;
            }  else {
                res = retval;
                return retval;
            }

        }

        public Object visitUnaryExpression(SceneMaxParser.UnaryExpressionContext ctx) {

            ctx.primaryExpression().accept(this);

            if(ctx.NOT()!=null) {
                res=!(Boolean)res;
            }

            return res;
        }

        public Object visitValue(SceneMaxParser.ValueContext ctx) {

            res=ctx.getText();
            if(ctx.BOOLEAN()!=null) {
                res = Boolean.parseBoolean(res.toString());
            } else if(ctx.number_expr()!=null) {
                res = Double.parseDouble(res.toString());
            } else if(ctx.QUOTED_STRING()!=null) {
                String tmp = ctx.QUOTED_STRING().getText();
                if(tmp.length()>=3) {
                    tmp = tmp.substring(1, tmp.length() - 1);
                } else {
                    tmp="";
                }
                res=tmp;
                turnOnIsString();
            } else if(ctx.variable_data_field()!=null) {
                SceneMaxParser.Variable_data_fieldContext dataFieldCtx = ctx.variable_data_field();
                String var = dataFieldCtx.var_decl().getText();
                RunTimeVarDef vd1 = app.findVarRuntime(null,thread,var);
                if(vd1==null) {
                    res=null;
                } else {
                    String fieldName = dataFieldCtx.field_name().getText();
                    res = app.getUserDataFieldValue(vd1.varName, fieldName);

                    if (res instanceof String) {
                        turnOnIsString();
                    }
                }

            } else if(ctx.variable_field()!=null) {

                String var = ctx.variable_field().var_decl().getText();
                RunTimeVarDef vd1 = app.findVarRuntime(null,thread,var);
                if(vd1==null) {
                    GroupInst ginst = thread.getGroup(var);
                    if(ginst!=null) {
                        String field = ctx.variable_field().var_field().getText();
                        if(field.equalsIgnoreCase("hit")) {
                            res=ginst.lastClosestRayCheck;
                            isObject=true;
                            return res;
                        }
                    } else {
                        return null;
                    }
                }

                String field = ctx.variable_field().var_field().getText();
                res=app.getFieldValue(vd1.varName,field);
                if(res instanceof EntityInstBase ) { //continue here
                    isObject=true;
                }

            } else if(ctx.function_value()!=null) {
                FunctionInvoker fi = new FunctionInvoker(ctx.function_value(),app,thread);
                if (fi.invoke()) {
                    res = fi.retval;
                    if (fi.retvalType == VariableDef.VAR_TYPE_STRING) {
                        turnOnIsString();
                    }
                }else {
                    // report run time error
                    hasRuntimeError=true;
                    app.handleRuntimeError("Line: "+ctx.start.getLine()+", "+fi.runtimeError);
                }
            } else if(ctx.csharp_register()!=null) {
                res = thread.getCSharpRegisterValue(ctx.csharp_register().res_var_decl().get(0).getText());
                if(res instanceof String) {
                    turnOnIsString();
                }
            } else if(ctx.fetch_array_value()!=null) {
                String varName = ctx.fetch_array_value().var_decl().getText();
                VarInst vi = thread.getVar(varName);
                if(vi==null) {
                    hasRuntimeError=true;
                    app.handleRuntimeError("Line "+ctx.start.getLine()+": Array variable '"+varName+"' not found");
                    return null;
                }
                Object indexObj = new ActionLogicalExpression(ctx.fetch_array_value().logical_expression(),thread).evaluate();
                int index = ((Double)indexObj).intValue();
                if(vi.values==null || vi.values.size()<=index || index<0) {
                    hasRuntimeError=true;
                    app.handleRuntimeError("Line "+ctx.start.getLine()+": Array '"+varName+"' index out of bound");
                    return null;
                }

                res = vi.values.get(index);
                if(res instanceof String) {
                    turnOnIsString();
                } else if(res instanceof List) {
                    isObject=true;
                } else if(res instanceof EntityInstBase) {
                    isObject=true;
                }
            } else if(ctx.get_array_length()!=null) {
                String varName = ctx.get_array_length().var_decl().getText();
                VarInst vi = thread.getVar(varName);
                if(vi==null) {
                    hasRuntimeError=true;
                    app.handleRuntimeError("Line "+ctx.start.getLine()+": Array variable '"+varName+"' not found");
                    return null;
                }

                if(vi.values==null) {
                    hasRuntimeError=true;
                    app.handleRuntimeError("Line "+ctx.start.getLine()+": Array '"+varName+"' is not initialized");
                    return null;
                }

                res = vi.values.size();

            } else if(ctx.calc_distance_value()!=null) {

                String obj1 = ctx.calc_distance_value().first_object().getText();
                String obj2 = ctx.calc_distance_value().second_object().getText();

                RunTimeVarDef vd1 = app.findVarRuntime(null,thread,obj1);
                if(vd1==null) {
                    return null;
                }
                RunTimeVarDef vd2 = app.findVarRuntime(null,thread,obj2);
                if(vd2==null) {
                    return null;
                }

                res = app.calcDistance(vd1.varName,vd2.varName);
            } else if(ctx.calc_angle_value()!=null) {

                String obj1 = ctx.calc_angle_value().first_object().getText();
                String obj2 = ctx.calc_angle_value().second_object().getText();

                RunTimeVarDef vd1 = app.findVarRuntime(null,thread,obj1);
                if(vd1==null) {
                    return null;
                }

                RunTimeVarDef vd2 = app.findVarRuntime(null,thread,obj2);
                if(vd2==null) {
                    return null;
                }

                res = app.calcAngle(vd1.varName,vd2.varName);
            } else if(ctx.get_json_value()!=null) {
                String varName = ctx.get_json_value().var_decl().getText();
                VarInst vi = thread.getVar(varName);
                if(vi==null) {
                    hasRuntimeError=true;
                    app.handleRuntimeError("Line "+ctx.start.getLine()+": Array variable '"+varName+"' not found");
                    return null;
                }

                JSONObject obj = null;
                JSONArray objArr = null;

                if(vi.value instanceof JSONObject) {
                    obj = (JSONObject) vi.value;
                } else if(vi.value instanceof String) {
                    String buff = vi.value.toString();
                    //buff=buff.replace("\\\"","\"");
                    try {
                        obj = new JSONObject(buff);
                    } catch (JSONException e) {
                         // not a JSON object , try JSONArray
                        try {
                            objArr = new JSONArray(buff);
                        } catch (JSONException err) {
                            err.printStackTrace();
                        }
                    }
                }

                if(obj!=null || objArr!=null) {

                    for(SceneMaxParser.Json_element_acceessorContext elem : ctx.get_json_value().json_accessor_expression().json_element_acceessor()) {
                        if(elem.json_field_accessor()!=null) {
                            if(obj==null) {
                                return null;
                            }

                            String fieldName = elem.json_field_accessor().var_decl().getText();
                            Object fieldVal = null;
                            try {
                                fieldVal = obj.get(fieldName);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(fieldVal instanceof JSONObject) {
                                obj = (JSONObject)fieldVal;
                                objArr=null;
                            } else if(fieldVal instanceof JSONArray) {
                                objArr = (JSONArray)fieldVal;
                                obj=null;
                            } else {
                                res=fieldVal;
                                if(res instanceof String) {
                                    turnOnIsString();
                                }
                                return fieldVal;
                            }
                        } else if(elem.json_array_item_accessor()!=null) {
                            if(objArr==null) {
                                return null;
                            }

                            Double index = (Double)new ActionLogicalExpression(elem.json_array_item_accessor().logical_expression(),thread).evaluate();
                            Object fieldVal = null;
                            try {
                                fieldVal = objArr.get(index.intValue());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(fieldVal instanceof JSONObject) {
                                obj = (JSONObject)fieldVal;
                                objArr=null;
                            } else if(fieldVal instanceof JSONArray) {
                                objArr = (JSONArray)fieldVal;
                                obj=null;
                            } else {
                                res=fieldVal;
                                if(res instanceof String) {
                                    turnOnIsString();
                                }
                                return fieldVal;
                            }
                        }
                    }
                }

                res=null;
                return null;

            } else {

                if(res.toString().equalsIgnoreCase(NEW_LINE_STRING)) {
                    res="\n";
                    turnOnIsString();
                } else {

                    VarInst varInst = this.thread.getVar(res.toString());

                    if(varInst!=null) {
                        if(varInst.varType==VariableDef.VAR_TYPE_ARRAY) {
                            isObject=true;
                            res = varInst.values;
                        } else {
                            res = varInst.value;
                            if(varInst.varType==VariableDef.VAR_TYPE_STRING) {
                                turnOnIsString();
                            } else if(res instanceof EntityInstBase) {
                                isObject=true;
                            }
                        }

                    } else {

                        decideWhichObjectIsRes(ctx);

                    }
                }


            }

            return res;
        }

        private void decideWhichObjectIsRes(SceneMaxParser.ValueContext ctx) {

            isObject=true;
            ModelInst mi = this.thread.getModel(res.toString());
            if(mi!=null) {
                res=mi;
            } else {
                SpriteInst si = this.thread.getSprite(res.toString());
                if(si!=null) {
                    res=si;
                } else {
                    SphereInst sphi = this.thread.getSphere(res.toString());
                    if(sphi!=null) {
                        res=sphi;
                    } else {
                        BoxInst bxinst = this.thread.getBox(res.toString());
                        if(bxinst!=null) {
                            res=bxinst;
                        } else {
                            isObject = false;
                            hasRuntimeError = true;
                            app.handleRuntimeError("Line: " + ctx.start.getLine() + ", '" + res.toString() + "' is not a valid number or variable");
                        }
                    }
                }
            }

        }

        private void turnOnIsString() {
            isResString = true;
            // if we already have some numeric value - convert it to string
            if(hasNumericRetval) {
                retvalStr = retval.toString();
                hasNumericRetval=false;

            }
        }


    }
}
