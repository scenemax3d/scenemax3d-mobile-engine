package com.scenemaxeng.common.types;

import android.content.Context;

import com.jme3.audio.AudioData;
import com.jme3.math.Vector3f;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

//import netscape.javascript.JSObject;

public class AssetsMapping {

    private final Context ctx;
    private HashMap<String, ResourceFont> _fontNodesRes = new HashMap<>();
    private HashMap<String, ResourceAudio> _audioNodesRes = new HashMap<>();
    private HashMap<String, ResourceSetup> _resources = new HashMap<>();
    private HashMap<String, ResourceSetup2D> _resources2D = new HashMap<>();
    private HashMap<String,TerrainResource> _terrains=new HashMap<>();
    private HashMap<String,SkyBoxResource> _skyboxes=new HashMap<>();

    private JSONObject getResourcesIndex() {
        String json = "";
        InputStream script = AssetsMapping.class.getClassLoader().getResourceAsStream("resources.json");
        if(script==null) {
            return null;
        }

        try {
            json = new String(Util.toByteArray(script));

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

    public AssetsMapping(Context context, String extPath) {
        this(context);

        JSONObject res = getResourcesFolderIndex(extPath+"/Models/models-ext.json");
        loadModelsFromJson(res);

        res = getResourcesFolderIndex(extPath+"/sprites/sprites-ext.json");
        loadSpritesFromJson(res);

        res = getResourcesFolderIndex(extPath+"/audio/audio-ext.json");
        loadAudioFromJson(res);

        res = getResourcesFolderIndex(extPath+"/skyboxes/skyboxes-ext.json");
        loadSkyBoxesFromJson(res);

    }

    public String loadFileFromAssets(String inFile) {

        String data = "";

        try {
            InputStream stream = ctx.getAssets().open(inFile);
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            data = new String(buffer);
        } catch (IOException e) {
            // Handle exceptions here
        }

        return data;

    }

    public AssetsMapping(Context context) {

        this.ctx=context;
        ///////////// LOAD MODELS ///////////
        JSONObject res = getAssetsResourcesFolderIndex( "Models/models.json");
        loadModelsFromJson(res);

        ///////////// LOAD SPRITES //////////
//        res = getResourcesFolderIndex("./resources/sprites/sprites.json");
//        loadSpritesFromJson(res);
//
//        ///////////// LOAD TERRAIN //////////
//        res = getResourcesFolderIndex("./resources/terrain/terrains.json");
//        loadTerrainsFromJson(res);
//
        ///////////// LOAD SOUNDS ///////////
        res = getAssetsResourcesFolderIndex("audio/audio.json");
        loadAudioFromJson(res);
//
//        ///////////// LOAD SOUNDS ///////////
//        res = getResourcesFolderIndex("./resources/fonts/fonts.json");
//        loadFontsFromJson(res);
//
//        ///////////// LOAD SkyBoxes ///////////
//        res = getResourcesFolderIndex("./resources/skyboxes/skyboxes.json");
//        loadSkyBoxesFromJson(res);
//
//        /////////////////////////////// READ SELF - CONTAINED ASSETS /////////////////////////////
//        // self contained exec will read from embedded class-path resource file
//        res = getResourcesIndex();
//        if(res!=null) {
//            loadSpritesFromJson(res);
//            loadModelsFromJson(res);
//            loadTerrainsFromJson(res);
//            loadAudioFromJson(res);
//            loadFontsFromJson(res);
//            loadSkyBoxesFromJson(res);
//        }
    }

    private void loadModelsFromJson(JSONObject res) {

        try {

            if (res == null || !res.has("models")) return;
            JSONArray models = res.getJSONArray("models");
            for (int i = 0; i < models.length(); ++i) {
                JSONObjectExt spr = null;

                spr = new JSONObjectExt(models.getJSONObject(i));

                String name = spr.getString("name");
                String path = spr.getString("path");
                float scaleX = spr.getFloat("scaleX");
                float scaleY = spr.getFloat("scaleY");
                float scaleZ = spr.getFloat("scaleZ");
                float transX = spr.getFloat("transX");
                float transY = spr.getFloat("transY");
                float transZ = spr.getFloat("transZ");
                float rotateY = spr.getFloat("rotateY");

                ResourceSetup res3D = new ResourceSetup(name, path, scaleX, scaleY, scaleZ, transX, transY, transZ, rotateY);
                res3D.setJsonBuffer(spr.toString());

                if (spr.has("physics")) {
                    JSONObject physics = spr.getJSONObject("physics");
                    if (physics.has("character")) {
                        JSONObjectExt character = new JSONObjectExt(physics.getJSONObject("character"));

                        float ratio = 1.0f / scaleX;
                        res3D.calibrateX = character.getFloat("calibrateX") * ratio;
                        res3D.calibrateY = character.getFloat("calibrateY") * ratio;
                        res3D.calibrateZ = character.getFloat("calibrateZ") * ratio;
                        //res3D.calibrateRatio = 1.0f/scaleX;

                        res3D.capsuleRadius = character.getFloat("capsuleRadius");
                        res3D.capsuleHeight = character.getFloat("capsuleHeight");
                        res3D.stepHeight = character.getFloat("stepHeight");
                    }

                    if (physics.has("vehicle")) {
                        res3D.isVehicle = true;
                        JSONObjectExt vehicle = new JSONObjectExt(physics.getJSONObject("vehicle"));

                        if (vehicle.has("chassisMaterial")) {
                            res3D.chassisMaterial = vehicle.getString("chassisMaterial");
                        }

                        if (vehicle.has("localScale")) {
                            res3D.localScale = vehicle.getFloat("localScale");
                        }

                        res3D.wheelModel = vehicle.getString("wheelModel");
                        if (vehicle.has("rearWheelModel")) {
                            res3D.rearWheelModel = vehicle.getString("rearWheelModel");
                        } else {
                            res3D.rearWheelModel = res3D.wheelModel;
                        }

                        if (vehicle.has("wheelMaterial")) {
                            res3D.wheelMaterial = vehicle.getString("wheelMaterial");
                        }

                        res3D.frontWheel = loadWheel(vehicle.getJSONObject("frontWheel"));
                        res3D.backWheel = loadWheel(vehicle.getJSONObject("backWheel"));

                        res3D.gearBox = loadGearBox(vehicle.getJSONObject("gearBox"));
                        res3D.engine = loadEngine(vehicle.getJSONObject("engine"));

                        res3D.mass = vehicle.getFloat("mass");
                        res3D.horn = vehicle.getString("horn");

                    }

                }

                name = name.toLowerCase();
                _resources.put(name, res3D);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private SceneMaxWheel loadWheel(JSONObject d) {
        JSONObjectExt data = null;
        try {
            data = new JSONObjectExt(d);

            SceneMaxWheel wheel = new SceneMaxWheel();
            wheel.scale = data.getFloat("scale");

            JSONObjectExt offset = new JSONObjectExt(data.getJSONObject("offset"));
            wheel.offset = new Vector3f(offset.getFloat("x"), offset.getFloat("y"), offset.getFloat("z"));
            wheel.steering = data.getBoolean("steering");
            wheel.brake = data.getFloat("brake");
            wheel.friction = data.getFloat("friction");
            wheel.diameter = data.getFloat("diameter");

            JSONObjectExt suspension = new JSONObjectExt(data.getJSONObject("suspension"));
            wheel.suspension.stiffness = suspension.getFloat("stiffness");
            wheel.suspension.compression = suspension.getFloat("compression");
            wheel.suspension.damping = suspension.getFloat("damping");
            wheel.suspension.length = suspension.getFloat("length");
            wheel.suspension.maxForce = suspension.getFloat("maxForce");

            wheel.accelerationForce = data.getFloat("accelerationForce");

            return wheel;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private SceneMaxGearBox loadGearBox(JSONObject data) {

        SceneMaxGearBox gb = new SceneMaxGearBox();
        JSONArray gears = null;
        try {
            gears = data.getJSONArray("gears");

            for (int i = 0; i < gears.length(); ++i) {
                JSONObjectExt gear = new JSONObjectExt(gears.getJSONObject(i));
                gb.gears.add(new SceneMaxGearBox.SceneMaxGear(gear.getFloat("start"), gear.getFloat("end")));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return gb;

    }

    private SceneMaxEngine loadEngine(JSONObject d) {

        JSONObjectExt data = null;
        SceneMaxEngine en = new SceneMaxEngine();

        try {
            data = new JSONObjectExt(d);

            en.name = data.getString("name");
            en.audio = data.getString("audio");
            en.power = data.getFloat("power");
            en.maxRevs = data.getFloat("maxRevs");
            en.braking = data.getFloat("braking");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return en;
    }

    private void loadFontsFromJson(JSONObject res) {
        try {

            if (res == null || !res.has("fonts")) return;
            JSONArray fonts = res.getJSONArray("fonts");
            for (int i = 0; i < fonts.length(); ++i) {
                JSONObject snd = fonts.getJSONObject(i);
                String fontName = snd.getString("name");
                String path = snd.getString("path");
                _fontNodesRes.put(fontName.toLowerCase(), new ResourceFont(fontName, path));//
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void loadAudioFromJson(JSONObject res) {

        try {
            if (res == null || !res.has("sounds")) return;
            JSONArray sounds = res.getJSONArray("sounds");
            for (int i = 0; i < sounds.length(); ++i) {
                JSONObject snd = sounds.getJSONObject(i);
                String soundName = snd.getString("name");
                String path = snd.getString("path");
                _audioNodesRes.put(soundName.toLowerCase(), new ResourceAudio(soundName, path, AudioData.DataType.Buffer));//
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void loadSpritesFromJson(JSONObject res) {

        try {
            if (res == null || !res.has("sprites")) return;

            JSONArray sprites = res.getJSONArray("sprites");
            for (int i = 0; i < sprites.length(); ++i) {
                JSONObject spr = sprites.getJSONObject(i);
                String spriteName = spr.getString("name");
                String path = spr.getString("path");
                int rows = spr.getInt("rows");
                int cols = spr.getInt("cols");
                _resources2D.put(spriteName.toLowerCase(), new ResourceSetup2D(spriteName, path, rows, cols));//
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadSkyBoxesFromJson(JSONObject res) {

        try {
            if (res == null || !res.has("skyboxes")) return;

            JSONArray skyboxes = res.getJSONArray("skyboxes");
            for (int i = 0; i < skyboxes.length(); ++i) {
                JSONObject skybox = skyboxes.getJSONObject(i);
                String name = skybox.getString("name");
                String up = skybox.getString("up");
                String down = skybox.getString("down");
                String left = skybox.getString("left");
                String right = skybox.getString("right");
                String front = skybox.getString("front");
                String back = skybox.getString("back");

                SkyBoxResource sr = new SkyBoxResource(name, up, down, left, right, front, back);
                sr.buff = skybox.toString();
                _skyboxes.put(name.toLowerCase(), sr);//
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadTerrainsFromJson(JSONObject res) {

        try {
            if (res == null || !res.has("terrains")) return;

            JSONArray terrains = res.getJSONArray("terrains");
            for (int i = 0; i < terrains.length(); ++i) {
                JSONObject terr = terrains.getJSONObject(i);
                String name = terr.getString("name");
                String alphaMap = terr.getString("Alpha");
                String redTex = terr.getString("Red");
                String greenTex = terr.getString("Green");
                String blueTex = terr.getString("Blue");
                String heightMap = terr.getString("HeightMap");
                JSONObject pos = terr.getJSONObject("pos");
                JSONObject scale = terr.getJSONObject("scale");

                TerrainResource tr = new TerrainResource(name, alphaMap, redTex, greenTex, blueTex, heightMap, pos, scale);
                tr.buff = terr.toString();
                _terrains.put(name.toLowerCase(), tr);//
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, SkyBoxResource> getSkyboxesIndex () { return _skyboxes; }
    public HashMap<String, TerrainResource> getTerrainsIndex () {
        return _terrains;
    }

    public HashMap<String, ResourceSetup> get3DModelsIndex () {
        return _resources;
    }

    public HashMap<String, ResourceSetup2D> getSpriteSheetsIndex () {
        return _resources2D;
    }

    public HashMap<String, ResourceAudio> getAudioIndex() {
        return _audioNodesRes;
    }

    public HashMap<String, ResourceFont> getFontsIndex() {
        return _fontNodesRes;
    }

    //
    private JSONObject getAssetsResourcesFolderIndex(String path) {

        try {
            String data = loadFileFromAssets(path);
            if(data != null && data.length() > 0) {
                return new JSONObject(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private JSONObject getResourcesFolderIndex(String path) {

        try {
            File f = new File(path);
            if(f.exists()) {
                String data = Util.readFile(f);
                if(data != null && data.length() > 0) {
                    return new JSONObject(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}