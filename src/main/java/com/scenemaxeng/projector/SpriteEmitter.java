package com.scenemaxeng.projector;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

public class SpriteEmitter {

    private final Geometry _geom;
    public EntityInstBase entityInst;
    String _name = "";
    private int _startFrame=0;
    private int _endFrame=2;
    private float _time=0;
    private float _targetTime=1;
    private transient Vector3f temp = new Vector3f();;
    private Material mat = null;


    public SpriteEmitter(String name, int rows, int cols, AssetManager assetManager, Texture texture) {

        _name=name;

        this.setStartFrame(0);
        this.setEndFrame(0);

        Box b = new Box(1f, 1f, 0.01f); // create cube shape
        Geometry geom = new Geometry("Box", b);  // create cube geometry from the shape
        mat = new Material(assetManager, "MatDefs/sprite_sheet.j3md");
        mat.setTexture("ColorMap", texture);
        mat.setFloat("SizeX", cols);
        mat.setFloat("SizeY", rows);
        mat.setFloat("Position", 0f);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(mat);
        
        _geom = geom;

    }

    public void setStartFrame(int start) {
        _startFrame=start;
    }

    public void setEndFrame(int end) {
        _endFrame=end;
    }

    public void setFrame(float frame) {
        mat.setFloat("Position", frame);
    }

    public Geometry getGeometry() {
        return _geom;
    }
}
