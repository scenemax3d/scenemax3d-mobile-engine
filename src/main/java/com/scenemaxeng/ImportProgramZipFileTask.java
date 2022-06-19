package com.scenemaxeng;

import android.os.Handler;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ImportProgramZipFileTask {

    private final Callback finish;
    private String filePath;
    public String error;
    private String targetScriptPath;
    private String resourcesHash="";
    private boolean importOnlyMetaData = true;// native Android already has all resources in assets folder

    public ImportProgramZipFileTask(String filePath, Callback finish) {

        this.finish = finish;
        this.filePath = filePath;

    }


    private File extractProgramZip(String zipFile) {

        File f = new File(zipFile);

        File folder = new File(f.getParentFile().getAbsolutePath()+"/"+f.getName().toLowerCase().replace(".zip",""));
        if(folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
                e.printStackTrace();
                this.error = e.getMessage();
            }
        }

        try {
            Util.unzip(new File(zipFile),new File(folder.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return folder;
    }

    private JSONObject getResourcesFolderIndex(String path) {

        try {
            File f = new File(path);
            if (!f.exists()) return null;

            String s = Util.readFile(f);
            if (s == null || s.length() == 0) return null;
            return new JSONObject(s);

        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private int skyboxNameExists(JSONArray skyboxes, String name) {

        try {
            for (int i = 0; i < skyboxes.length(); ++i) {
                JSONObject m = skyboxes.getJSONObject(i);
                if (m.getString("name").equalsIgnoreCase(name)) {
                    return i;
                }
            }

            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int spriteNameExists(JSONArray sprites, String name) {

        try {
            for (int i = 0; i < sprites.length(); ++i) {
                JSONObject m = sprites.getJSONObject(i);
                if (m.getString("name").equalsIgnoreCase(name)) {
                    return i;
                }
            }

            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int modelNameExists(JSONArray models, String name) {

        try {
            for (int i = 0; i < models.length(); ++i) {
                JSONObject m = models.getJSONObject(i);
                if (m.getString("name").equalsIgnoreCase(name)) {
                    return i;
                }
            }
        } catch (JSONException e) {

        }
        return -1;
    }

    private int audioNameExists(JSONArray sounds, String name) {

        try {
            for (int i = 0; i < sounds.length(); ++i) {
                JSONObject m = sounds.getJSONObject(i);
                if (m.getString("name").equalsIgnoreCase(name)) {
                    return i;
                }
            }

            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void copyResourcesIndex(File srcFolder, File srcResFile) {

        try {
            File resFolder = new File(Util.getDefaultResourcesFolder());
            File media = new File(resFolder,"Models");
            if(!media.exists()) {
                media.mkdir();
            }
            media = new File(resFolder,"sprites");
            if(!media.exists()) {
                media.mkdir();
            }
            media = new File(resFolder,"audio");
            if(!media.exists()) {
                media.mkdir();
            }
            media = new File(resFolder,"skyboxes");
            if(!media.exists()) {
                media.mkdir();
            }


            String text = new String(Files.readAllBytes(srcResFile.toPath()));
            JSONObject json = new JSONObject(text);

            // Update models index
            JSONObject builtInRes = getResourcesFolderIndex((Util.getDefaultResourcesFolder()+"/Models/models.json"));

            if(builtInRes==null) {
                builtInRes = new JSONObject("{\"models\":[]}");
            }


            JSONArray currBuiltInModels = builtInRes.getJSONArray("models");
            JSONObject res = getResourcesFolderIndex((Util.getResourcesFolder()+"/Models/models-ext.json"));
            if(res==null) {
                res=new JSONObject("{\"models\":[]}");
            }
            JSONArray currModels = res.getJSONArray("models");

            boolean changed = false;

            if(json.has("models")) {
                JSONArray models = json.getJSONArray("models");

                for (int i = 0; i < models.length(); ++i) {
                    JSONObject model = models.getJSONObject(i);
                    String name = model.getString("name");
                    if (modelNameExists(currBuiltInModels, name)==-1) {
                        int index=modelNameExists(currModels, name);
                        if(index!=-1) {
                            currModels.remove(index);
                        }

                        String modelPath = model.getString("path");
                        if(!this.importOnlyMetaData) {
                            File srcDir = new File(srcFolder.getAbsolutePath() + "/export_res/" + modelPath).getParentFile();
                            File destDir = new File(Util.getResourcesFolder() + "/Models/" + srcDir.getName());
                            destDir.mkdirs();
                            FileUtils.copyDirectory(srcDir, destDir);
                        }
                        currModels.put(model);
                        changed = true;
                    }

                }

                if (changed) {
                    Util.writeFile(Util.getResourcesFolder()+"/Models/models-ext.json", res.toString(2));
                }
            }

            // Update sprites index
            if(json.has("sprites")) {
                builtInRes = getResourcesFolderIndex((Util.getDefaultResourcesFolder()+"/sprites/sprites.json"));
                if(builtInRes==null) {
                    builtInRes = new JSONObject("{\"sprites\":[]}");
                }

                JSONArray currBuiltInSprites = builtInRes.getJSONArray("sprites");
                res = getResourcesFolderIndex((Util.getResourcesFolder()+"/sprites/sprites-ext.json"));
                if(res==null) {
                    res=new JSONObject("{\"sprites\":[]}");
                }

                JSONArray currSprites = res.getJSONArray("sprites");
                JSONArray sprites = json.getJSONArray("sprites");
                changed = false;
                for (int i = 0; i < sprites.length(); ++i) {
                    JSONObject sprite = sprites.getJSONObject(i);
                    String name = sprite.getString("name");
                    if (spriteNameExists(currBuiltInSprites,name)==-1) {

                        int index = spriteNameExists(currSprites, name);
                        if(index!=-1) {
                            currSprites.remove(index);
                        }

                        if(!this.importOnlyMetaData) {
                            String path = sprite.getString("path");
                            File src = new File(srcFolder.getAbsolutePath() + "/export_res/" + path);
                            File dest = new File(Util.getResourcesFolder() + "/" + path);
                            FileUtils.copyFile(src, dest);
                        }

                        currSprites.put(sprite);
                        changed = true;
                    }

                }

                if (changed) {
                    Util.writeFile(Util.getResourcesFolder()+"/sprites/sprites-ext.json", res.toString(2));
                }
            }



            if(json.has("sounds")) {
                builtInRes = getResourcesFolderIndex((Util.getDefaultResourcesFolder()+"/audio/audio.json"));
                if(builtInRes==null) {
                    builtInRes = new JSONObject("{\"sounds\":[]}");
                }
                JSONArray currBuiltInAudio = builtInRes.getJSONArray("sounds");
                res = getResourcesFolderIndex((Util.getResourcesFolder()+"/audio/audio-ext.json"));
                if(res==null) {
                    res=new JSONObject("{\"sounds\":[]}");
                }

                JSONArray currAudio = res.getJSONArray("sounds");

                JSONArray sounds = json.getJSONArray("sounds");
                changed = false;
                for (int i = 0; i < sounds.length(); ++i) {
                    JSONObject sound = sounds.getJSONObject(i);
                    String name = sound.getString("name");
                    if (audioNameExists(currBuiltInAudio, name)==-1) {

                        int index = audioNameExists(currAudio, name);
                        if(index!=-1) {
                            currAudio.remove(index);
                        }

                        if(!this.importOnlyMetaData) {
                            String path = sound.getString("path");
                            File src = new File(srcFolder.getAbsolutePath() + "/export_res/" + path);
                            File dest = new File(Util.getResourcesFolder() + "/" + path);
                            FileUtils.copyFile(src, dest);
                        }

                        currAudio.put(sound);
                        changed = true;
                    }

                }

                if (changed) {
                    Util.writeFile(Util.getResourcesFolder()+"/audio/audio-ext.json", res.toString(2));
                }

            }

            // Update skyboxes index
            if(json.has("skyboxes")) {
                builtInRes = getResourcesFolderIndex((Util.getDefaultResourcesFolder()+"/skyboxes/skyboxes.json"));
                if(builtInRes==null) {
                    builtInRes = new JSONObject("{\"skyboxes\":[]}");
                }
                JSONArray currBuiltInSkyBoxes = builtInRes.getJSONArray("skyboxes");
                res = getResourcesFolderIndex((Util.getResourcesFolder()+"/skyboxes/skyboxes-ext.json"));
                if(res==null) {
                    res=new JSONObject("{\"skyboxes\":[]}");
                }
                JSONArray currSkyboxes = res.getJSONArray("skyboxes");

                JSONArray skyboxes = json.getJSONArray("skyboxes");
                changed = false;
                for (int i = 0; i < skyboxes.length(); ++i) {
                    JSONObject skybox = skyboxes.getJSONObject(i);
                    String name = skybox.getString("name");
                    if (skyboxNameExists(currBuiltInSkyBoxes, name)==-1) {

                        int index = skyboxNameExists(currSkyboxes, name);
                        if(index!=-1) {
                            currSkyboxes.remove(index);
                        }

                        if(!this.importOnlyMetaData) {
                            String path = skybox.getString("back");
                            File srcDir = new File(srcFolder.getAbsolutePath() + "/export_res/" + path).getParentFile();
                            File destDir = new File(Util.getResourcesFolder() + "/skyboxes/" + srcDir.getName());
                            destDir.mkdirs();
                            FileUtils.copyDirectory(srcDir, destDir);
                        }

                        currSkyboxes.put(skybox);
                        changed = true;
                    }

                }

                if (changed) {
                    Util.writeFile(Util.getResourcesFolder()+"/skyboxes/skyboxes-ext.json", res.toString(2));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyResourceFileToDefaultStorage(File srcFolder, String resPath) {

        File src = new File(srcFolder.getAbsolutePath()+"/export_res/"+resPath);
        File dest = new File(Util.getDefaultResourcesFolder()+"/"+resPath);
        if(src.exists() && !dest.exists()) {
            try {
                FileUtils.copyFile(src, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    protected void done() {
        JSONObject res = new JSONObject();
        try {
            res.put("targetScriptPath",targetScriptPath);
            res.put("resourcesHash",resourcesHash);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.finish.done(res);
    }

    public void run() {
        try {
            File src = extractProgramZip(filePath);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Integer res = doInBackground(src);
                        done();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            },500);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected Integer doInBackground(File src) throws Exception {

        // ****
        String targetFolderName = src.getName();
        String targetScriptFile = "";
        boolean importResourcesOnly = false;

        for (File f:src.listFiles()) {
            if(f.isFile()) {
                if (f.getName().equals("extract_config.json")) {
                    String json = Util.readFile(f);
                    JSONObject config = new JSONObject(json);
                    if(config.has("targetFolder")) {
                        targetFolderName=config.getString("targetFolder");
                    }

                    if (config.has("scriptFile")) {
                        targetScriptFile=config.getString("scriptFile");
                    }

                    if (config.has("resOnly")) {
                        importResourcesOnly=config.getBoolean("resOnly");
                    }

                    if (config.has("resourcesHash")) {
                        resourcesHash=config.getString("resourcesHash");
                    }

                }

            }
        }

        File scriptFolder = null;
        // create parent folder if needed
        if(!importResourcesOnly) {
            scriptFolder = new File(Util.getScriptsFolder()+"/" + targetFolderName);
            if (!scriptFolder.exists()) {
                scriptFolder.mkdir();
            }
        }


        // copy script files + cs files
        for (File f:src.listFiles()) {
            if(f.isFile()) {
                if(f.getName().equals("extract_config.json")) {
                    // not copying this file
                } else if(f.getName().equals("resources.json")) {
                    copyResourcesIndex(src,f);
                } else {

                    if(!importResourcesOnly) {
                        File destFile = new File(scriptFolder.getAbsolutePath() + "/" + f.getName());
                        // delete existing file if exists
                        if (destFile.exists()) {
                            // backup existing
                            File backupFile = new File(scriptFolder.getAbsolutePath() + "/" + f.getName() + ".bkup");
                            FileUtils.copyFile(destFile, backupFile);

                            destFile.delete();
                        }

                        FileUtils.copyFileToDirectory(f, scriptFolder);

                        // allow main script file to be auto selected
                        if (f.getName().equals(targetScriptFile)) {
                            targetScriptPath = destFile.getAbsolutePath();
                        }
                    }

                }
            } else if(f.getName().equals("export_res")) {
                for(File resFile:f.listFiles()) {
                    if(resFile.isDirectory()) {
                        if(resFile.getName().equals("macro")) {
                            File macroFolder = new File("macro");
                            for(File macroFile:resFile.listFiles()) {
                                if(macroFile.isFile()) {
                                    FileUtils.copyFileToDirectory(macroFile,macroFolder);
                                }
                            }
                        }

                    }
                }
            }
        }

        return 0;
    }


}
