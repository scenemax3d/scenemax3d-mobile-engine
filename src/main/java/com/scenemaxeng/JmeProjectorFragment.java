package com.scenemaxeng;

import android.os.Environment;

import com.jme3.app.AndroidHarnessFragment;
import com.scenemaxeng.projector.AssetsMapping;
import com.scenemaxeng.projector.SceneMaxApp;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JmeProjectorFragment extends AndroidHarnessFragment {

    //private IProjectorListener projListener = null;

    public JmeProjectorFragment() {
        appClass=SceneMaxApp.class.getName();
    }

//    public void setProjectorListener(IProjectorListener listener) {
//        this.projListener=listener;
//    }

    @Override
    public void onStart() {
        super.onStart();

        final SceneMaxApp app=(SceneMaxApp)this.getJmeApplication();

    }

    public void clearScene() {
        SceneMaxApp app=(SceneMaxApp)this.getJmeApplication();
        if(app!=null) {
            app.clearScene();
        }
    }

    public void runScript(String script, String workingFolder) {

        // remove canvas size & window mode commands for Android execution
        script=setCanvasSize(script);
        script=setWindowMode(script);

        SceneMaxApp app=(SceneMaxApp)this.getJmeApplication();
        if(app!=null) {
            File wf = new File(workingFolder);
            File projFolder = wf.getParentFile().getParentFile();
            File resourcesFolder = new File(projFolder,"resources");
            AssetsMapping am = new AssetsMapping(this.getContext(),resourcesFolder.getAbsolutePath());
            app.setAssetsMapping(am);
            app.setWorkingFolder(workingFolder);

            File cf = new File(this.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"common");
            app.setCommonFolder(cf.getAbsolutePath());

            app.run(script);
        }
    }

    public void stopScript() {
        SceneMaxApp app=(SceneMaxApp)this.getJmeApplication();
        if(app!=null) {
            app.stopScript();
        }
    }

    private String setCanvasSize(String prg) {

        String pat="^canvas\\.size\\s+((?<val1>\\d+),(?<val2>\\d+))";
        Pattern p = Pattern.compile(pat,Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(prg);
        int width = 1024;
        int height = 768;
        if(m.find()) {

            String w = m.group("val1");
            String h = m.group("val2");
            width=Integer.parseInt(w);
            height=Integer.parseInt(h);

            prg = m.replaceFirst("");
        }

        return prg;
    }

    private String setWindowMode(String prg) {

        String pat="^screen\\.mode\\s+full";
        Pattern p = Pattern.compile(pat,Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(prg);

        prg=prg.replaceFirst(pat,"");

        return prg;

    }



}
