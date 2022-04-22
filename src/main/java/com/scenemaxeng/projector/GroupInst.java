package com.scenemaxeng.projector;

import com.jme3.scene.Node;
import com.scenemaxeng.compiler.GroupDef;

public class GroupInst {

    public final SceneMaxThread thread;
    private final GroupDef def;
    public Node node;
    public EntityInstBase lastClosestRayCheck;

    public GroupInst(GroupDef gd, SceneMaxThread thread, Node n) {
        this.def = gd;
        this.node=n;
        this.thread=thread;
        this.thread.groups.put(gd.name,this);

    }


}
