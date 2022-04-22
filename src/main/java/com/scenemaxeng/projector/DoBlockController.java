package com.scenemaxeng.projector;


import com.abware.scenemaxlang.parser.SceneMaxParser;
import com.scenemaxeng.compiler.ActionStatementBase;
import com.scenemaxeng.compiler.DoBlockCommand;
import com.scenemaxeng.compiler.FunctionBlockDef;
import com.scenemaxeng.compiler.StatementDef;
import com.scenemaxeng.compiler.VariableDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DoBlockController extends SceneMaxBaseController {

    public final SceneMaxThread parentThread; // the thread which runs this sequence

    public SceneMaxParser.Logical_expressionContext goExpr;
    private DoBlockCommand cmd = null;
    //public SceneMaxApp app = null;

    private ArrayList<SceneMaxBaseController> _controllers = new ArrayList<>();
    private SceneMaxThread thread = null;
    private double count = 0;
    private double target = -1;
    public HashMap<String,Object> funcScopeParams;
    private List<String> paramsNames;
    private List<SceneMaxParser.Logical_expressionContext> paramsExp;
    public ActionLogicalExpression intervalExpr;
    private Double interval=-1.0;
    //private double timerIntervalTarget = 0;
    private double timerIntervalCount=0;
    private boolean timerTicked;
    public int type = 0;
    private boolean checkGoExpr = true;


    public DoBlockController(SceneMaxApp app, SceneMaxThread thread, DoBlockCommand cmd) {
        this.app=app;
        this.parentThread=thread;
        this.cmd=cmd;

        this.adhereToPauseStatus=false; // do block works even when the scene is paused
    }


    @Override
    public void init() {

        if(cmd.prg==null) {
            app.handleRuntimeError("Check 3D/2D resources scope");
        }

    }

    @Override
    public boolean run(float tpf) {

        if(forceStop) {
            return true;
        }

        boolean goCondition=true;

        if(this.interval==-1 && this.intervalExpr!=null) {
            Object interval = this.intervalExpr.evaluate();
            if(interval instanceof Double) {
                this.interval=(Double)interval;
            }
        }

        if(checkGoExpr && goExpr!=null) {
            checkGoExpr=false;
            Object cond = new ActionLogicalExpression(goExpr,parentThread).evaluate();
            if(cond instanceof Boolean) {
                goCondition=(Boolean)cond;
                if(!goCondition) { // no-go & regular procedure, no timer - stop
                    if(this.interval==-1) {
                        return true;
                    } else {
                        // set a void thread to prevent the controller from working while waiting for the timer tick event
                        thread = new SceneMaxThread();
                    }
                }
            }

        }

        if(this.funcScopeParams==null) {
            evalFunctionScopeParams();
        }

        // run actions
        if(thread==null && goCondition) {
            thread = new SceneMaxThread();
            thread.mainController.app=app;
            thread.isReturnPoint=cmd.isReturnPoint;
            thread.isSecondLevelReturnPoint=cmd.isSecondLevelReturnPoint;
            thread.mainController.adhereToPauseStatus=false; // thread main controller never pauses
            thread.funcScopeParams=this.funcScopeParams;
            thread.parent=parentThread;

            thread.sequenceCreatorThread=cmd.creatorThread;

            if(this.interval!=-1) {
                thread.isReturnPoint=true;
                thread.type=SceneMaxThread.THREAD_TYPE_LOOPER;
                thread.setCreatorController(this);
            }

            for(StatementDef st:cmd.prg.requireResourceActions){
                app.loadResource(st);
            }

            for(FunctionBlockDef f: cmd.prg.functions.values()) {
                f.doBlock.creatorThread = thread;
            }

            for (StatementDef action : cmd.prg.actions) {
                app.runAction(cmd.prg,(ActionStatementBase) action, thread);
            }

            registerController(thread.mainController);
        }

        if(this.target==-1 && goCondition) {
            if(this.cmd.amountExpr==null){
                this.target=0;
            } else {
                this.target = Double.parseDouble(new ActionLogicalExpression(this.cmd.amountExpr,thread).evaluate().toString());
                thread.type=SceneMaxThread.THREAD_TYPE_LOOPER;
                thread.isReturnPoint=true;
                thread.setCreatorController(this);
            }
        }


        //////////////  RUN //////////////
        boolean loopFinished = _controllers.size()==0;

        for(int i=_controllers.size()-1;i>=0;--i) {

            SceneMaxBaseController ctl =  _controllers.get(i);

            // check whether this controller should be paused
            if(app.scenePaused) {
                if(ctl.adhereToPauseStatus) {
                    continue;
                }
            }

            boolean finished = ctl.run(tpf);
            if(finished) {
                ctl.isRunning=false;
                _controllers.remove(i);
            }

            if(_controllers.size()==0) {
                count++;

                if(this.interval==-1) {
                    thread=null; // trigger re-run all actions
                }

                if(count>=target) {
                    loopFinished= true;
                } else {
                    thread=null; // trigger re-run all actions
                }
            }

        }


        if(this.interval!=-1) {

            // timer doesn't work when scene is paused
            if(app.scenePaused) {
                return false;
            }

            timerIntervalCount+=tpf;
            if(timerIntervalCount>=this.interval) {
                timerTicked=true;
                timerIntervalCount=0;
            }

            if(loopFinished && timerTicked) {
                checkGoExpr=true;  // in the next run, recheck the go expression
                timerTicked=false; // enable next ticking
                count = 0; // reset loop counter
                thread = null;
            }
            return false; // since we have a timer, we cannot end the controller even if the loop is finished
        }

        return loopFinished;
    }

    private int registerController(SceneMaxBaseController c) {

        c.setUIProxy(this.app); // this probably should be changed -
                                //  - we need to have LoopBlockController to implement its own IUiProxy
        c.init();
        _controllers.add(c);
        return 0;
    }

    public void setFuncScopeParams(HashMap<String,Object> params) {
        this.funcScopeParams=params;
    }

    public void setFunctionScopeParams(List<String> paramsNames, List<SceneMaxParser.Logical_expressionContext> paramsExp) {

        this.paramsNames=paramsNames;
        this.paramsExp=paramsExp;

    }

    public void setFunctionScopeParam(List<String> paramsNames, EntityInstBase param) {
        funcScopeParams=new HashMap<>();
        this.funcScopeParams.put(paramsNames.get(0), param);
    }

        private void evalFunctionScopeParams() {
        if(paramsNames!=null && paramsNames.size()>0) {
            funcScopeParams=new HashMap<>();
            int index = 0;
            for (SceneMaxParser.Logical_expressionContext ctx : paramsExp) {
                ActionLogicalExpression exp = new ActionLogicalExpression(ctx, this.parentThread);
                Object obj = exp.evaluate();

                if(obj instanceof String) {
                    VariableDef vd = new VariableDef();
                    vd.varType=VariableDef.VAR_TYPE_STRING;
                    VarInst vi = new VarInst(vd,null);
                    vi.varType=VariableDef.VAR_TYPE_STRING;
                    vi.value=obj;
                    this.funcScopeParams.put(paramsNames.get(index), vi);
                } else if(obj instanceof Double) {
                    VariableDef vd = new VariableDef();
                    vd.varType=VariableDef.VAR_TYPE_NUMBER;
                    VarInst vi = new VarInst(vd,null);
                    vi.varType=VariableDef.VAR_TYPE_NUMBER;
                    vi.value=obj;
                    this.funcScopeParams.put(paramsNames.get(index), vi);
                } else {
                    this.funcScopeParams.put(paramsNames.get(index), obj);
                }
                index++;
                if(index>=paramsNames.size()) {
                    break;
                }
            }

        }

    }
}
