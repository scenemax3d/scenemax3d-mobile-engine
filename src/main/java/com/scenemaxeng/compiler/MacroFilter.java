package com.scenemaxeng.compiler;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroFilter {

    private HashMap<String, JSONObject> macroRules;

    static class LexicographicComparator implements Comparator<Object> {
        @Override
        public int compare(Object a, Object b) {
            JSONObject a1 = (JSONObject) a;
            JSONObject b1 = (JSONObject) b;
            int len = 0;
            try {
                len = a1.getString("pat").length() - b1.getString("pat").length();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (len > 0) {
                return -1;
            } else if (len < 0) {
                return 1;
            }

            return 0;
        }
    }

    public boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public void setMacroRules(HashMap<String, JSONObject> macroRules) {
        this.macroRules = macroRules;
    }

    public void loadMacroRulesFromMacroFolder(File macroFolder) {

        if(!macroFolder.exists()) {
            return;
        }

        this.macroRules = new HashMap<>();

        for (File macro : macroFolder.listFiles()) {
            String macroFileName = macro.getName();
            String macroFile = null;
            JSONObject rules = null;

            try {
                macroFile = FileUtils.readFileToString(macro, String.valueOf(StandardCharsets.UTF_8));
                rules = new JSONObject(macroFile);
            } catch (Exception e) {
                // something wrong with the JSON - continue to next file
            }

            if (!rules.has("patterns")) {
                continue;
            }

            this.macroRules.put(macroFileName, rules);

        }

    }

    public ApplyMacroResults apply(String prg) {

        ApplyMacroResults mr = new ApplyMacroResults();
        mr.finalPrg = prg;
        mr.finalPrg = mr.finalPrg.replaceAll("\r", "");

        try {

            for (String rulesKey : macroRules.keySet()) {

                JSONObject rules = macroRules.get(rulesKey);
                JSONArray patterns = null;

                patterns = rules.getJSONArray("patterns");

                List<Object> patternsSorted = new ArrayList<>();// patterns.toList();
                for (int i = 0; i < patterns.length(); i++) {
                    patternsSorted.add(patterns.getJSONObject(i));
                }
                Collections.sort(patternsSorted, new LexicographicComparator());

                for (int i = 0; i < patternsSorted.size(); ++i) {

                    JSONObject item = (JSONObject) patternsSorted.get(i);

                    String pat = item.getString("pat");
                    if (pat.length() == 0) {
                        continue;
                    }

                    String[] words = pat.split("\\s+");
                    pat = "";
                    int valNum = 0;
                    for (int j = 0; j < words.length; ++j) {
                        String w = words[j].trim();
                        if (isNumeric(w)) {
                            valNum++;
                            pat += "(?<val" + valNum + ">\\d+(\\.\\d+)?)";

                        } else {
                            pat += w;
                        }

                        if (j < words.length - 1) {
                            pat += "\\s+";
                        }
                    }

                    String program = item.getString("prg");

                    Pattern p = Pattern.compile(pat);
                    Matcher m = p.matcher(mr.finalPrg);
                    int groupCnt = m.groupCount();

                    while (m.find()) {
                        // replace all numeric values in program with actual values
                        if (groupCnt > 0) {
                            for (int j = 1; j <= valNum; ++j) {//skip group 0
                                String actualNum = m.group("val" + j);// get group by name
                                program = program.replaceFirst("\\d+", actualNum);
                            }
                        }

                        mr.finalPrg = m.replaceFirst(program);
                        if (!mr.macroFilesUsed.contains(rulesKey)) {
                            mr.macroFilesUsed.add(rulesKey);
                        }
                        m = p.matcher(mr.finalPrg);
                    }

                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mr;

    }
}
