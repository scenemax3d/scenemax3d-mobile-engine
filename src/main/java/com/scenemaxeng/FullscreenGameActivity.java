package com.scenemaxeng;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;
import com.scenemaxeng.projector.SceneMaxApp;
import com.scenemaxeng.projector.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class FullscreenGameActivity extends Activity {

    public final static int FORWARD=1;
    public final static int REVERSE=2;
    public final static int LEFT=4;
    public final static int RIGHT=8;

    private String targetScriptPath;
    private int joystickState = 0;

    private JmeProjectorFragment jmeFragment;
    private String resourcesHash;

    Joystick joystick;
    private SceneMaxApp app;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        destryed=true;
        if(app!=null) {
            app.getAudioRenderer().cleanup();
            app.stop(false);
        }
    }

    private boolean destryed = false;

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!destryed) {
                    startGame();
                }
            }
        },1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.scenemaxeng.Util.setContext(this);
        importProgram();

        setContentView(R.layout.activity_fullscreen_game);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int orientation = this.getIntent().getExtras().getInt("is_landscape");
        if(orientation==1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //showJmeProjectorFragment();

        joystick = findViewById(R.id.joystick);
        int joystickPos = this.getIntent().getExtras().getInt("joystick");

        if(joystickPos==0) {
            joystick.setVisibility(View.GONE);
        } else if(joystickPos==3) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) joystick.getLayoutParams();
            params.endToEnd=R.id.fullscreen_content;
            joystick.requestLayout();
        } else if(joystickPos==2) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) joystick.getLayoutParams();
            params.endToEnd=R.id.fullscreen_content;
            params.startToStart=ConstraintLayout.LayoutParams.UNSET;
            joystick.requestLayout();
        }

        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                app.observePress();
            }

            @Override
            public void onDrag(float degrees, float offset) {

                int state = 0;

                if (offset > 0.3) {
                    if (degrees < -25 && degrees > -165) {
                        state |= REVERSE;
                    } else if (degrees < 155 && degrees > 25) {
                        state |= FORWARD;
                    }

                    if (degrees < 60 && degrees > -60) {
                        state |= RIGHT;
                    } else if ((degrees < -115 && degrees > -180) || (degrees < 180 && degrees > 115)) {
                        state |= LEFT;
                    }

                }

                if(state!=joystickState) {
                    joystickState=state;
                    app.observeState((state|LEFT)==state,(state|RIGHT)==state,
                            (state|FORWARD)==state,(state|REVERSE)==state);
                    //app.viObserveDrag(degrees, offset);
                }

            }

            @Override
            public void onUp() {
                joystickState=0;
                app.observeRelease();
            }
        });

    }

    private void importProgram() {
        InputStream is = this.getResources().openRawResource(R.raw.code);
        File code = new File(this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"code.zip");
        com.scenemaxeng.Util.copyInputStreamToFile(is, code);
        new ImportProgramZipFileTask(code.getAbsolutePath(), new Callback() {

            @Override
            public void done(Object res) {
                if(res!=null){

                    JSONObject obj = (JSONObject)res;
                    try {
                        FullscreenGameActivity.this.targetScriptPath = obj.getString("targetScriptPath");
                        FullscreenGameActivity.this.resourcesHash = obj.getString("resourcesHash");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).run();

    }

    @Override
    public void onBackPressed() {

        jmeFragment.clearScene();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
        FullscreenGameActivity.this.finish();
//            }
//        },500);
    }

    private void startGame() {
        if(this.targetScriptPath==null) {
            return;
        }

        showJmeProjectorFragment();
        //String targetScriptPath = this.getIntent().getExtras().getString("targetFile");
        File targetFile = new File(this.targetScriptPath);
        if(targetFile.exists()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String code = Util.readFile(targetFile);
                    code=code.replaceFirst("^canvas\\.size\\s+((?<val1>\\d+),(?<val2>\\d+))","");;

                    String workingFolder = targetFile.getParentFile().getAbsolutePath();
                    getJmeFragment().runScript(code, workingFolder);
                    app = (SceneMaxApp) getJmeFragment().getJmeApplication();
                }
            },500);

        }
    }

    private JmeProjectorFragment getJmeFragment() {
        return jmeFragment;
    }

    private void showJmeProjectorFragment() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<Fragment> fragments = getFragmentManager().getFragments();

                jmeFragment = new JmeProjectorFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.frame, jmeFragment);
                fragmentTransaction.commitAllowingStateLoss();

            }
        });

    }


    public long download(String url, File targetFolder, String title) {

        try {
            Uri u = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(u);
            request.setTitle(title);

            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            String fileName = u.getLastPathSegment();
            File target = new File(targetFolder, fileName);
            request.setDestinationUri(Uri.fromFile(target));
            // get download service and enqueue file
            DownloadManager manager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            return manager.enqueue(request);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return 0;

    }

}