package com.scenemaxeng;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {

    private static Context ctx;
    private static String activeProject = "default";

    public static void setContext(Context host) {
        ctx=host;
    }

    public static void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    public static String readFile(File f) {

        String text = "";
        try {
            text = FileUtils.readFileToString(f, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;

    }

    public static boolean writeFile(String path, String text) {

        BufferedWriter writer = null;
        try {
            File target = new File(path);
            writer = new BufferedWriter(new FileWriter(target));
            writer.write(text);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }

//    public static void unzip(File zipFile, File targetDirectory) throws IOException {
//        ZipInputStream zis = new ZipInputStream(
//                new BufferedInputStream(new FileInputStream(zipFile)));
//        try {
//            ZipEntry ze;
//            int count;
//            byte[] buffer = new byte[8192];
//            while ((ze = zis.getNextEntry()) != null) {
//
//                File file = new File(targetDirectory, ze.getName());
//                System.out.println("extracting: " + file.getAbsolutePath());
//                File dir = ze.isDirectory() ? file : file.getParentFile();
//
//                if (!dir.isDirectory() && !dir.mkdirs()) {
//                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
//                }
//
//                if (ze.isDirectory())
//                    continue;
//                FileOutputStream fout = new FileOutputStream(file);
//                try {
//                    while ((count = zis.read(buffer)) != -1)
//                        fout.write(buffer, 0, count);
//                } finally {
//                    fout.close();
//                }
//
//            }
//        } finally {
//            zis.close();
//        }
//    }

    private static final String TAG = "ZipExtractor";

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();

                Log.d(TAG, "Processing entry: " + ze.getName());

                if (!dir.exists() && !dir.mkdirs()) {
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    Log.d(TAG, "Created directory: " + file.getAbsolutePath());
                    continue;
                }

                Log.d(TAG, "Creating file: " + file.getAbsolutePath());

                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }

                Log.d(TAG, "Extracted file: " + file.getAbsolutePath());

            }
        } finally {
            zis.close();
        }
    }

    public static void createProjectFolder(String projName) {
        File basePath = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File project = new File(basePath,"projects/"+projName);
        if(!project.exists()) {
            project.mkdirs();
            createResourcesFolderStruct(project);

            File scripts = new File(project,"scripts");
            scripts.mkdir();
        }

    }

    public static void createCommonStorage() {
        File basePath = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File common = new File(basePath,"common");
        if(!common.exists()) {
            common.mkdir();
            createResourcesFolderStruct(common);
        }
    }

    private static void createResourcesFolderStruct(File target) {

        try {
            File resources = new File(target, "resources");
            resources.mkdir();
            File media = new File(resources, "Models");
            media.mkdir();
            File json = new File(media, "models.json");
            FileUtils.write(json, "{\"models\":[]}");

            media = new File(resources, "sprites");
            media.mkdir();
            json = new File(media, "sprites.json");
            FileUtils.write(json, "{\"sprites\":[]}");

            media = new File(resources, "audio");
            media.mkdir();
            json = new File(media, "audio.json");
            FileUtils.write(json, "{\"sounds\":[]}");

            media = new File(resources, "skyboxes");
            media.mkdir();
            json = new File(media, "skyboxes.json");
            FileUtils.write(json, "{\"skyboxes\":[]}");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getDefaultResourcesFolder() {
        File basePath = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File common = new File(basePath,"common");
        common =  new File(common,"resources");
        if(!common.exists()) {
            common.mkdirs();
        }

        return common.getAbsolutePath();
    }

    public static String getResourcesFolder() {

        File basePath = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File projFolder = new File(basePath,"projects/"+activeProject);
        return new File(projFolder,"resources").getAbsolutePath();
    }

    public static String getScriptsFolder() {
        File basePath = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File projFolder = new File(basePath,"projects/"+activeProject);
        return new File(projFolder,"scripts").getAbsolutePath();
    }

    public static String readFileFromAssets(AssetManager assetManager, String resPath) {
        StringBuilder content = new StringBuilder();
        try {
            InputStream inputStream = assetManager.open(resPath);//"myfiles/" + file

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            inputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return content.toString();
    }

}
