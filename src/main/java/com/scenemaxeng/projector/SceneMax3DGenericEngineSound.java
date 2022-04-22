package com.scenemaxeng.projector;

import com.jayfella.jme.vehicle.Sound;

import java.util.ArrayList;
import java.util.List;

public class SceneMax3DGenericEngineSound extends Sound {

    List<String> soundFiles = new ArrayList<>();

    public SceneMax3DGenericEngineSound(int soundType) {

        switch(soundType) {
            case 1:
                setEngineSound1();
                break;
            case 2:
                setEngineSound2();
                break;

            case 4:
                setEngineSound4();
                break;

            case 5:
                setEngineSound5();
                break;

        }
    }

    public void setEngineSound1() {
        addAssetPath("audio/engine-1d2.ogg", 13.4f);
        addAssetPath("audio/engine-1.ogg", 26.75f);
        addAssetPath("audio/engine-1x2.ogg", 53.5f);
        addAssetPath("audio/engine-1x4.ogg", 107f);
    }

    public void setEngineSound2() {
        addAssetPath("audio/engine-2d2.ogg", 12.6f);
        addAssetPath("audio/engine-2.ogg", 25.25f);
        addAssetPath("audio/engine-2x2.ogg", 50.5f);
        addAssetPath("audio/engine-2x4.ogg", 101f);
    }

    public void setEngineSound4() {
        addAssetPath("audio/engine-4d8.ogg", 10.7f);
        addAssetPath("audio/engine-4d4.ogg", 21.4f);
        addAssetPath("audio/engine-4d2.ogg", 42.9f);
        addAssetPath("audio/engine-4.ogg", 85.75f);
        addAssetPath("audio/engine-4x2.ogg", 171.5f);
    }

    public void setEngineSound5() {
        addAssetPath("audio/engine-5d4.ogg", 14.4f);
        addAssetPath("audio/engine-5d2.ogg", 28.9f);
        addAssetPath("audio/engine-5.ogg", 57.75f);
        addAssetPath("audio/engine-5x2.ogg", 115.5f);
    }

    public void addAssetPath(String path, float pitch) {
        soundFiles.add(path);
        super.addAssetPath(path, pitch);
    }
    public List<String> getSoundFiles() {
        return soundFiles;
    }



}
