package com.scenemaxeng.projector;

import java.util.HashMap;

public class SceneMaxThread  {

    public static final int THREAD_TYPE_LOOPER = 10;
    public static final int THREAD_TYPE_RETURN_POINT = 20;


    public SceneMaxThread parent = null;
    private static int threadSeq=0;
    public final int threadId;
    public CompositeController mainController = null;
    public HashMap<String, VarInst> vars_index = new HashMap<String, VarInst>();
    public SceneMaxThread sequenceCreatorThread;
    private HashMap<String, Object> csharpRegisters = new HashMap<>();
    public HashMap<String, ModelInst> models = new HashMap<>();
    public HashMap<String, SpriteInst> sprites = new HashMap<>();
    public HashMap<String, SphereInst> spheres = new HashMap<>();
    public HashMap<String, BoxInst> boxes = new HashMap<>();
    public HashMap<String, GroupInst> groups = new HashMap<>();

    //
    public HashMap<String, Object> funcScopeParams;
    public int type;
    private SceneMaxBaseController _creatorController;
    public boolean isReturnPoint=false; // mark thread as possible return point when using "return" command
    public boolean isSecondLevelReturnPoint = false;

    public SceneMaxThread() {
        mainController = new CompositeController();
        this.threadId=++threadSeq;
    }

    public void add(SceneMaxBaseController c) {
        if(c!=null) {
            mainController.add(c);
        }
    }

    public void setCSharpRegister(String targetRegister, Object val) {
        csharpRegisters.put(targetRegister, val);
    }

    public Object getCSharpRegisterValue(String registerName) {
        Object val =  csharpRegisters.get(registerName);
        if(val==null) {
            if(parent==null) {
                return null;
            }

            return parent.getCSharpRegisterValue(registerName);
        } else {
            return val;
        }
    }

    public VarInst getVar(String targetVar) {

        if(funcScopeParams!=null) {
            Object val = funcScopeParams.get(targetVar);
            if (val != null && val instanceof VarInst) {
                return (VarInst) val;
            }
        }

        VarInst def = vars_index.get(targetVar);
        if(def==null){
            if(parent==null) {
                return null;
            }

            return parent.getVar(targetVar);
        } else {
            return def;
        }


    }

    public BoxInst getBox(String varName) {

        if(funcScopeParams!=null) {
            Object val = funcScopeParams.get(varName);
            if (val != null && val instanceof BoxInst) {
                return (BoxInst) val;
            }
        }


        BoxInst def = boxes.get(varName);
        if(def==null){
            if(parent==null) {
                return null;
            }

            return parent.getBox(varName);
        } else {
            return def;
        }

    }

    public GroupInst getGroup(String varName) {

        if(funcScopeParams!=null) {
            Object val = funcScopeParams.get(varName);
            if (val != null && val instanceof GroupInst) {
                return (GroupInst) val;
            }
        }


        GroupInst def = groups.get(varName);
        if(def==null){
            if(parent==null) {
                return null;
            }

            return parent.getGroup(varName);
        } else {
            return def;
        }

    }

    public SphereInst getSphere(String varName) {

        if(funcScopeParams!=null) {
            Object val = funcScopeParams.get(varName);
            if (val != null && val instanceof SphereInst) {
                return (SphereInst) val;
            }
        }


        SphereInst def = spheres.get(varName);
        if(def==null){
            if(parent==null) {
                return null;
            }

            return parent.getSphere(varName);
        } else {
            return def;
        }


    }

    public ModelInst getModel(String varName) {

        if(funcScopeParams!=null) {
            Object val = funcScopeParams.get(varName);
            if (val != null && val instanceof ModelInst) {
                return (ModelInst) val;
            }
        }


        ModelInst def = models.get(varName);
        if(def==null){

            if(sequenceCreatorThread!=null) {
                return sequenceCreatorThread.getModel(varName);
            }

            if(parent==null) {
                return null;
            }

            return parent.getModel(varName);
        } else {
            return def;
        }


    }

    public SpriteInst getSprite(String varName) {

        if(funcScopeParams!=null) {
            Object val = funcScopeParams.get(varName);
            if (val != null && val instanceof SpriteInst) {
                return (SpriteInst) val;
            }
        }

        SpriteInst def = sprites.get(varName);
        if(def==null){
            if(parent==null) {
                return null;
            }

            return parent.getSprite(varName);
        } else {
            return def;
        }
    }


    public Object getFuncScopeParam(String varName) {
        Object var = null;
        if(funcScopeParams!=null) {
            var = funcScopeParams.get(varName);
        }
        if(var==null) {
            if(parent==null) {
                return null;
            }

            return parent.getFuncScopeParam(varName);
        } else {
            return var;
        }
    }

    public EntityInstBase getEntityInst(String var) {

//        Object vi = this.funcScopeParams.get(var);
//        if(vi!=null && vi instanceof EntityInstBase) {
//            return (EntityInstBase)vi;
//        }

        ModelInst mi = this.getModel(var);
        if(mi==null) {
            SpriteInst si = this.getSprite(var);
            if(si==null) {
                SphereInst sphi = this.getSphere(var);
                if(sphi==null) {

                    BoxInst bxi = this.getBox(var);
                    if(bxi==null) {

                        return null;

                    } else {
                        return bxi;
                    }

                } else {
                    return sphi;
                }
            } else {
                return si;
            }

        } else {
            return mi;
        }

    }

    public int getVariableThreadId(String var) {

        EntityInstBase eb = getEntityInst(var);
        if(eb!=null) {
            return eb.thread.threadId;
        }

        GroupInst gri = this.getGroup(var);
        if(gri==null) {
            return 0;// should throw error instance not found
        } else {
            return gri.thread.threadId;
        }

    }

    public SceneMaxThread getSecondLevelReturnPointThread() {
        if(this.isSecondLevelReturnPoint) {
            return this;
        } else if(this.parent!=null) {
            return this.parent.getSecondLevelReturnPointThread();
        } else {
            return null;
        }
    }

    public SceneMaxThread getFirstReturnPointThread() {
        if(this.isReturnPoint) {
            return this;
        } else if(this.parent!=null) {
            return this.parent.getFirstReturnPointThread();
        } else {
            return null;
        }
    }

    public SceneMaxThread getFirstLooperThread() {
        if(this.type==SceneMaxThread.THREAD_TYPE_LOOPER) {
            return this;
        } else if(this.parent!=null) {
            return this.parent.getFirstLooperThread();
        } else {
            return null;
        }
    }

    public void forceStop() {
        if(mainController!=null) {
            mainController.forceStop();
        }
    }

    public SceneMaxBaseController getCreatorController() {
        return _creatorController;
    }

    public void setCreatorController(SceneMaxBaseController c) {
        _creatorController=c;
    }

}
