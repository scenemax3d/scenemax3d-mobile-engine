package com.scenemaxeng.compiler;

import com.abware.scenemaxlang.parser.SceneMaxBaseVisitor;
import com.abware.scenemaxlang.parser.SceneMaxLexer;
import com.abware.scenemaxlang.parser.SceneMaxParser;
import com.abware.scenemaxlang.parser.SceneMaxParser.StatementContext;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SceneMaxLanguageParser implements IParser {

    public static MacroFilter macroFilter;
    public static boolean parseUsingResource = false;
    public static List<String> skyboxUsed = new ArrayList<>();
    public static List<String> terrainsUsed = new ArrayList<>();
    public static List<String> spriteSheetUsed = new ArrayList<>();
    public static List<String> modelsUsed = new ArrayList<>();
    public static List<String> audioUsed = new ArrayList<>();
    public static List<String> fontsUsed = new ArrayList<>();
    public static List<String> filesUsed = new ArrayList<>();
    public static List<String> macroFilesUsed = new ArrayList<>();

    ProgramDef prg = null;
    String codePath="";

    private boolean isChildParser=false;
    private String _sourceFileName="";
    private static int foreachCounter=0; // implicit foreach function counter

    private static String trimQuotedString(String s) {
        if(s.length()>2) {
            s=s.substring(1,s.length()-1);
        }

        return s;
    }

    public static String readFile(File f) {

        String text = "";

        try {
            text = FileUtils.readFileToString(f, String.valueOf(StandardCharsets.UTF_8));

        }catch(Exception ex) {

        }

        return text;
    }

    public SceneMaxLanguageParser(ProgramDef prg, String codePath) {
        super();
        this.prg = prg;
        this.codePath=codePath;
    }

    public SceneMaxLanguageParser(ProgramDef prg) {
        super();
        this.prg = prg;
    }


    public SceneMaxLanguageParser() {
        // temporary hard-code insert the default terrain
        SceneMaxLanguageParser.terrainsUsed.clear();
        SceneMaxLanguageParser.terrainsUsed.add("terrain1");

    }

    public void enableChildParserMode(boolean enable) {
        this.isChildParser=enable;
    }

    public ProgramDef parse(String code) {

        // Only main parser cleans the collections
        if(!isChildParser) {
            macroFilesUsed.clear();
            filesUsed.clear();
            fontsUsed.clear();
            audioUsed.clear();
            modelsUsed.clear();
            spriteSheetUsed.clear();

        }

        final List<String> errors = new ArrayList<>();

        if(macroFilter!=null) {
            ApplyMacroResults mr = macroFilter.apply(code);
            code=mr.finalPrg;

            for(String fileName:mr.macroFilesUsed) {
                if(!macroFilesUsed.contains(fileName)) {
                    macroFilesUsed.add(fileName);
                }
            }
        }

        CharStream charStream = new ANTLRInputStream(code);
        SceneMaxLexer lexer = new SceneMaxLexer(charStream);
        TokenStream tokens = new CommonTokenStream(lexer);
        SceneMaxParser parser = new SceneMaxParser(tokens);

        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(final Recognizer<?,?> recognizer, final Object offendingSymbol, final int line,
                                    final int charPositionInLine, final String msg, final RecognitionException e) {

                String err = "line: " + line + ", offset: " + charPositionInLine +
                        " " + msg;
                if(_sourceFileName.length()>0) {
                    err="File: " +_sourceFileName+", "+err;
                }
                errors.add(err);
            }
        });


        ProgramVisitor v = new ProgramVisitor(this.prg, this.codePath);
        ProgramDef prg = v.visit(parser.prog());

        if(errors.size()>0) {
            if(prg==null){
                prg=new ProgramDef();
            }
            prg.syntaxErrors.addAll(errors);
        }


        return prg;
    }

    public static void mergeExternalCode(ProgramDef prg, ProgramDef extPrg) {

        prg.syntaxErrors.addAll(extPrg.syntaxErrors);
        prg.groups.putAll(extPrg.groups);

        if(prg.inParams!=null && extPrg.inParams!=null) {
            prg.inParams.addAll(extPrg.inParams);
        }
        prg.vars.addAll(extPrg.vars);
        prg.vars_index.putAll(extPrg.vars_index);
        prg.functions.putAll(extPrg.functions);
        prg.models.putAll(extPrg.models);
        prg.sprites.putAll(extPrg.sprites);
        prg.actions.addAll(extPrg.actions);
    }

    public static void setMacroFilter(MacroFilter mf) {
        macroFilter=mf;
    }

    public static MacroFilter getMacroFilter() {
        return macroFilter;
    }

    private class ProgramVisitor extends SceneMaxBaseVisitor<ProgramDef> {

        private ProgramDef prg;
        private String codePath="";


        public ProgramVisitor(ProgramDef prg, String codePath) {
            this.prg = prg;
            if(codePath!=null) {
                this.codePath=codePath;
            }
        }

        @Override
        public ProgramDef visitProg(SceneMaxParser.ProgContext ctx) {

            if(this.prg!=null) {
                ProgramDef prg = new ProgramDef();
                prg.parent = this.prg;

                ProgramStatementsVisitor prgStatementsVisitor = new ProgramStatementsVisitor(prg, this.codePath);
                return prgStatementsVisitor.visit(ctx.program_statements());
            }


            ProgramDef prg = null;

            ProgramStatementsVisitor prgStatementsVisitor = new ProgramStatementsVisitor(null,this.codePath);
            prg = prgStatementsVisitor.visit(ctx.program_statements());

            return prg;

        }

        private class ProgramStatementsVisitor extends SceneMaxBaseVisitor<ProgramDef> {

            private ProgramDef prg;
            private String codePath="";

            public ProgramStatementsVisitor(ProgramDef prg, String codePath) {
                this.prg = prg;
                if(codePath!=null) {
                    this.codePath=codePath;
                }
            }

            @Override
            public ProgramDef visitProgram_statements(SceneMaxParser.Program_statementsContext ctx) {

                ProgramDef prg = new ProgramDef();
                prg.parent = this.prg;

                // root program should have a camera variable
                if(prg.parent==null) {
                    prg.addCameraVariableDef();
                }


                DefineStatementVisitor defineStatementVisitor = new DefineStatementVisitor(prg,this.codePath);

                List<StatementContext> statements = ctx.statement();
                for (StatementContext st : statements) {

                    StatementDef stDef = st.accept(defineStatementVisitor);
                    if(stDef==null) {

                        //return null;
                        continue;
                    }

                    if(stDef instanceof ForEachCommand) {
                        ForEachCommand fec = (ForEachCommand)stDef;
                        prg.functions.put(fec.funcDef.name,fec.funcDef);
                        prg.actions.add(fec);
                    } else if(stDef instanceof FunctionBlockDef) {
                        FunctionBlockDef fd = (FunctionBlockDef)stDef;
                        prg.functions.put(fd.name,fd);
                        //System.out.println("function def=" + fd.name);

                    } else if(stDef instanceof ModelDef) {
                        ModelDef md = (ModelDef)stDef;
                        prg.models.put(md.name, md);

                    } else if(stDef instanceof SpriteDef) {
                        SpriteDef def = (SpriteDef)stDef;

                        if(def.varName!=null) {

                            if(def.name!=null) {
                                prg.sprites.put(def.name, def);
                            }

                            VariableDef var = new VariableDef();
                            var.varType=VariableDef.VAR_TYPE_2D;
                            var.resName=def.name;
                            var.resNameExpr = def.nameExpr;
                            var.varName = def.varName;
                            var.xExpr=def.xExpr;
                            var.yExpr=def.yExpr;
                            var.zExpr=def.zExpr;
                            var.entityPos=def.entityPos;

                            prg.vars.add(var);
                            prg.vars_index.put(var.varName, var);

                            CreateSpriteCommand cmd = new CreateSpriteCommand(def,var);
                            prg.actions.add(cmd);

                        }


                    } else if(stDef instanceof VariableDef) {
                        VariableDef var = (VariableDef)stDef;
                        GraphicEntityCreationCommand cmd = new GraphicEntityCreationCommand(var);

                        if(!var.validate(prg)) {
                            // assume implicit declaration of a 3d model
                            var.varType=VariableDef.VAR_TYPE_3D;

                            if(var.resName!=null) {
                                ModelDef md = new ModelDef();
                                md.name = var.resName;
                                md.from = "";
                                md.isVehicle = var.isVehicle;
                                prg.models.put(md.name, md);

                                if (!modelsUsed.contains(md.name)) {
                                    modelsUsed.add(md.name);
                                }
                            }

                        }

                        prg.vars.add(var);
                        prg.vars_index.put(var.varName, var);
                        prg.actions.add(cmd);


                    } else if(stDef instanceof VariableDeclarationCommand) {

                        VariableDeclarationCommand varDecl = (VariableDeclarationCommand)stDef;
                        for (VariableDeclarationCommand var : varDecl.siblings) {
                            if(!prg.vars_index.containsKey(var.varName)) {
                                VariableDef vd = new VariableDef();
                                vd.resName = "var";
                                vd.varName = var.varName;

                                VariableAssignmentCommand vac = new VariableAssignmentCommand();
                                vac.var = vd;

                                if(var.array!=null) {
                                    vac.array = var.array;
                                } else {
                                    vac.expression = var.valExpr;
                                }

                                vac.expression = var.valExpr;
                                prg.actions.add(vac);

                                prg.vars.add(vd);
                                prg.vars_index.put(vd.varName, vd);
                            }
                        }

                    } else {
                        ActionStatementBase base = (ActionStatementBase)stDef;
                        if(base!=null && base.validate(prg)) {
                            prg.actions.add(stDef);
                            if(stDef.requireResource) {
                                prg.requireResourceActions.add(stDef);
                            }
                        } else {
                            String err = "";
                            if(_sourceFileName.length()>0) {
                                err="Error: at file: "+_sourceFileName+": ";
                            }
                            prg.syntaxErrors.add(err+base.lastError+" at line: "+ctx.start.getLine());
                            //return null;
                        }
                    }

                }

                if(prg.parent!=null && prg.syntaxErrors.size()>0) {
                    prg.parent.syntaxErrors.addAll(prg.syntaxErrors);
                }
                return prg;

            }
        }


        private class DefineStatementVisitor extends SceneMaxBaseVisitor<StatementDef> {

            private final ProgramDef prg;
            private String codePath="";

            public DefineStatementVisitor(ProgramDef prg, String codePath) {
                this.prg=prg;
                if(codePath!=null) {
                    this.codePath=codePath;
                }
            }

            public ActionStatementBase visitHttpStatement(SceneMaxParser.HttpStatementContext ctx) {

                HttpCommand cmd = new HttpCommand();
                SceneMaxParser.Http_getContext getCtx = ctx.http_statement().http_action().http_get();
                if(getCtx!=null) {
                    cmd.verb = HttpCommand.VERB_TYPE_GET;
                    cmd.addressExp = getCtx.http_address().logical_expression();
                    cmd.callbackProcName = getCtx.res_var_decl().getText();
                    return cmd;
                }

                SceneMaxParser.Http_postContext postCtx = ctx.http_statement().http_action().http_post();
                if(postCtx!=null) {
                    cmd.verb = HttpCommand.VERB_TYPE_POST;
                    cmd.addressExp = postCtx.http_address().logical_expression();
                    cmd.bodyExp = postCtx.http_body().logical_expression();
                    cmd.callbackProcName = postCtx.res_var_decl().getText();
                    return cmd;
                }

                SceneMaxParser.Http_putContext putCtx = ctx.http_statement().http_action().http_put();
                if(putCtx!=null) {
                    cmd.verb = HttpCommand.VERB_TYPE_PUT;
                    cmd.addressExp = putCtx.http_address().logical_expression();
                    cmd.bodyExp = putCtx.http_body().logical_expression();
                    cmd.callbackProcName = putCtx.res_var_decl().getText();
                    return cmd;
                }

                return null;

            }

            public ActionStatementBase visitForEachStatement(SceneMaxParser.ForEachStatementContext ctx) {

                ForEachCommand cmd = new ForEachCommand();
                String functionParamName = ctx.for_each_statement().var_decl().getText();
                List<String> params = new ArrayList<>();
                params.add(functionParamName);
                DoBlockCommand doBlock = new DoBlockVisitor(prg, params).visit(ctx.for_each_statement().do_block());

                FunctionBlockDef fdef = new FunctionBlockDef();
                fdef.doBlock = doBlock;
                fdef.name="foreach_"+ ++foreachCounter;
                cmd.funcDef = fdef;

                if(ctx.for_each_statement().entity_type()!=null) {
                    String type = ctx.for_each_statement().entity_type().getText().toLowerCase();
                    if(type.equals("model")) {
                        cmd.entityType = VariableDef.VAR_TYPE_3D;
                    } else if(type.equals("sprite")) {
                        cmd.entityType = VariableDef.VAR_TYPE_2D;
                    } else if(type.equals("box")) {
                        cmd.entityType = VariableDef.VAR_TYPE_BOX;
                    } else if(type.equals("sphere")) {
                        cmd.entityType = VariableDef.VAR_TYPE_SPHERE;
                    }
                }

                if(ctx.for_each_statement().for_each_having_expr()!=null) {
                    for(SceneMaxParser.For_each_having_attrContext attr : ctx.for_each_statement().for_each_having_expr().for_each_having_attr()) {
                        if(attr.for_each_name_attr()!=null) {
                            cmd.name = attr.for_each_name_attr().QUOTED_STRING().getText();
                            if(cmd.name.length()>2) {
                                cmd.name = cmd.name.substring(1, cmd.name.length() - 1);
                            }

                            if(attr.for_each_name_attr().string_comparators()!=null) {
                                cmd.nameComparator = attr.for_each_name_attr().string_comparators().getText().toLowerCase();
                            }
                        }
                    }
                }

                return cmd;
            }

            public ActionStatementBase visitMiniMapActions(SceneMaxParser.MiniMapActionsContext ctx) {

                MiniMapCommand cmd = new MiniMapCommand();
                cmd.show = ctx.mini_map_actions().show_or_hide().Show()!=null;

                if(ctx.mini_map_actions().minimap_options()!=null) {
                    for (SceneMaxParser.Minimap_optionContext attr : ctx.mini_map_actions().minimap_options().minimap_option()) {
                        if (attr.height_attr() != null) {
                            cmd.heightExpr = attr.height_attr().logical_expression();
                        } else if (attr.unisize_attr() != null) {
                            cmd.sizeExpr = attr.unisize_attr().logical_expression();
                        } else if (attr.follow_entity()!=null) {
                            cmd.targetVar = attr.follow_entity().var_decl().getText();
                        }
                    }

                }
                return cmd;
            }

            public ActionStatementBase visitChannelDraw(SceneMaxParser.ChannelDrawContext ctx) {

                ChannelDrawCommand cmd = new ChannelDrawCommand();

                cmd.channelName = ctx.channel_draw_statement().res_var_decl().getText();//
                cmd.resourceName = ctx.channel_draw_statement().sprite_name().getText();

                if(ctx.channel_draw_statement().channel_draw_attrs()!=null) {
                    for(SceneMaxParser.Channel_draw_attrContext attr : ctx.channel_draw_statement().channel_draw_attrs().channel_draw_attr()) {
                        if(attr.frame_attr()!=null) {
                            cmd.frameNumExpr = attr.frame_attr().logical_expression();
                        } else if(attr.pos_2d_attr()!=null) {
                            cmd.posXExpr =attr.pos_2d_attr().pos_axes_2d().print_pos_x().logical_expression();
                            cmd.posYExpr =attr.pos_2d_attr().pos_axes_2d().print_pos_y().logical_expression();
                        } else if(attr.size_2d_attr()!=null) {
                            cmd.widthExpr = attr.size_2d_attr().width_size().logical_expression();
                            cmd.heightExpr = attr.size_2d_attr().height_size().logical_expression();
                        }
                    }
                }

                if(cmd.resourceName!=null && !cmd.resourceName.equalsIgnoreCase("clear")) {
                    if (!spriteSheetUsed.contains(cmd.resourceName)) {
                        spriteSheetUsed.add(cmd.resourceName);
                    }
                }

                return cmd;


            }

            public ActionStatementBase visitAddExternalCode(SceneMaxParser.AddExternalCodeContext ctx) {

                List<String> files = new ArrayList<>();
                for(SceneMaxParser.File_nameContext file:ctx.add_external_code().file_name()) {
                    String name = file.QUOTED_STRING().getText();
                    if(name.length()>0) {
                        name=name.substring(1,name.length()-1);
                    }

                    files.add(name);
                }

                for (String file: files) {

                    if(filesUsed.contains(file)) {
                        continue; // prevent cyclic dependency
                    }

                    // get code from file or resource
                    String code = getExternalCode(file);
                    if(code!=null) {
                        SceneMaxLanguageParser parser = new SceneMaxLanguageParser(this.prg);
                        parser.setParserSourceFileName(file);
                        parser.enableChildParserMode(true);
                        //parser.setMacroFilter(SceneMaxLanguageParser.getMacroFilter());
                        ProgramDef prg = parser.parse(code);
                        filesUsed.add(file);

                        SceneMaxLanguageParser.mergeExternalCode(this.prg,prg);
                    }

                }

                return null;

            }

            private String getExternalCode(String file) {

                String code = null;
                File f = new File(this.codePath+"/"+file);
                if(f.exists()) {
                    code = readFile(f);
                } else {

                    InputStream script = SceneMaxLanguageParser.class.getClassLoader().getResourceAsStream(file);
                    try {
                        if(script!=null) {
                            code = new String(Utils.toByteArray(script));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                return code;

            }


            public StatementDef visitDeclareVariable(SceneMaxParser.DeclareVariableContext ctx) {

                VariableDeclarationCommand cmd = new VariableDeclarationCommand();
                cmd.siblings = new ArrayList<>();

                for(SceneMaxParser.Variable_name_and_assignemtContext v : ctx.declare_variable().variable_name_and_assignemt()) {

                    VariableDeclarationCommand var = new VariableDeclarationCommand();
                    var.varName = v.res_var_decl().getText();

                    if(v.var_value_option().array_value()!=null) {
                        var.array = new ArrayList<>();
                        var.array.addAll(v.var_value_option().array_value().logical_expression());
                    } else {
                        var.valExpr = v.var_value_option().single_value_option().logical_expression();
                    }

                    cmd.siblings.add(var);
                }

                return cmd;
            }

            public ActionStatementBase visitDefineGroup(SceneMaxParser.DefineGroupContext ctx) {

                AddEntityToGroupCommand cmd = new AddEntityToGroupCommand();
                cmd.targetVar = ctx.define_group().res_var_decl().getText();
                cmd.targetGroup = ctx.define_group().group_name().getText();
                if(!prg.groups.containsKey(cmd.targetGroup)) {
                    prg.groups.put(cmd.targetGroup,new GroupDef(cmd.targetGroup));
                }
                return cmd;
            }

            public ActionStatementBase visitDebugStatement(SceneMaxParser.DebugStatementContext ctx) {
                ChangeDebugMode cmd = new ChangeDebugMode();
                if(ctx.debug_statement().debug_actions().debug_on()!=null) {
                    cmd.debugOn=true;
                } else {
                    cmd.debugOff=true;
                }

                return cmd;
            }

            public StatementDef visitAttachCameraActions(SceneMaxParser.AttachCameraActionsContext ctx) {
                FpsCameraCommand cmd = new FpsCameraCommand();

                cmd.command = ctx.attach_camera_actions().attach_camera_action().attach_camera_action_start() != null ?
                        FpsCameraCommand.START : FpsCameraCommand.STOP;

                String varName = null;

                if(cmd.command==FpsCameraCommand.START) {
                    SceneMaxParser.Attach_camera_action_startContext startCtx = ctx.attach_camera_actions().attach_camera_action().attach_camera_action_start();
                    varName = startCtx.var_decl().getText();

                    VariableDef vd = prg.getVar(varName);
                    if (vd == null) {
                        prg.syntaxErrors.add("Variable '" + varName + "' not exists");
                        return null;
                    }
                    cmd.varDef = vd;
                    cmd.targetVar = varName;

                    for(SceneMaxParser.Attach_camera_having_optionContext attr:startCtx.attach_camera_having_expr().attach_camera_having_options().attach_camera_having_option()) {
                        if(attr.camera_type_attr()!=null) {
                            cmd.cameraType = attr.camera_type_attr().camera_type().getText();
                        }  if(attr.damping_attr()!=null) {
                            cmd.dampingExpr = attr.damping_attr().logical_expression();
                        } else if(attr.replay_attr_offset()!=null) {
                            String axis = attr.replay_attr_offset().all_axes_names().getText();
                            if(axis.equalsIgnoreCase("x")) {
                                cmd.offsetXExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("y")) {
                                cmd.offsetYExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("z")) {
                                cmd.offsetZExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("rx")) {
                                cmd.offsetRXExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("ry")) {
                                cmd.offsetRYExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("rz")) {
                                cmd.offsetRZExpr = attr.replay_attr_offset().logical_expression();
                            }
                        }
                    }

                }

                return cmd;
            }

            public StatementDef visitChaseCameraActions(SceneMaxParser.ChaseCameraActionsContext ctx) {
                ChaseCameraCommand cmd = new ChaseCameraCommand();
                SceneMaxParser.Chase_camera_actionContext actionCtx = ctx.chase_camera_actions().chase_camera_action();
                SceneMaxParser.Chase_camera_action_chaseContext ctxChase = actionCtx.chase_camera_action_chase();

                if(ctxChase!=null) {

                    String varName = ctxChase.var_decl().getText();
                    VariableDef vd = prg.getVar(varName);
                    if(vd==null) {
                        prg.syntaxErrors.add("Variable '"+varName+"' not exists");
                        return null;
                    }

                    cmd.varDef=vd;
                    cmd.targetVar = vd.varName;
                    cmd.command = ChaseCameraCommand.CHASE;

                    SceneMaxParser.Chase_cam_having_exprContext having = ctxChase.chase_cam_having_expr();
                    if(having!=null) {
                        cmd.havingAttributesExists=true;
                        SceneMaxParser.Chase_cam_optionsContext options = having.chase_cam_options();
                        for(SceneMaxParser.Chase_cam_optionContext opt: options.chase_cam_option()) {
                            if(opt.chase_cam_option_trailing()!=null) {
                                if(opt.chase_cam_option_trailing().False()!=null) {
                                    cmd.trailing = false;
                                }
                            } else if(opt.chase_cam_option_rotation_speed()!=null) {
                                cmd.rotationSpeedExpr = opt.chase_cam_option_rotation_speed().logical_expression();
                            } else if(opt.chase_cam_option_vertical_rotation()!=null) {
                                cmd.verticalRotationExpr = opt.chase_cam_option_vertical_rotation().logical_expression();
                            } else if(opt.chase_cam_option_horizontal_rotation()!=null) {
                                cmd.horizontalRotationExpr = opt.chase_cam_option_horizontal_rotation().logical_expression();
                            } else if(opt.chase_cam_option_min_distance()!=null) {
                                cmd.minDistanceExpr = opt.chase_cam_option_min_distance().logical_expression();
                            } else if(opt.chase_cam_option_max_distance()!=null) {
                                cmd.maxDistanceExpr = opt.chase_cam_option_max_distance().logical_expression();
                            }
                        }
                    }

                    return cmd;
                }

                if(actionCtx.chase_camera_action_stop()!=null) {
                    cmd.command = ChaseCameraCommand.STOP;
                    return cmd;
                }


                return null;
            }

            public StatementDef visitTerrainActions(SceneMaxParser.TerrainActionsContext ctx) {

                TerrainCommand cmd = new TerrainCommand();
                SceneMaxParser.Terrain_actionContext ac = ctx.terrain_actions().terrain_action();
                if(ac.terrain_action_show()!=null) {
                    cmd.action = TerrainCommand.ACTION_SHOW;
                    cmd.terrainNameExprCtx = ac.terrain_action_show().logical_expression();
                } else if(ac.terrain_action_hide()!=null) {
                    cmd.action = TerrainCommand.ACTION_HIDE;
                }

                return cmd;
            }

            public StatementDef visitDefBox(SceneMaxParser.DefBoxContext ctx) {

                String varName = ctx.define_box().res_var_decl().getText();
                BoxVariableDef varDef = new BoxVariableDef();
                varDef.varName = varName;
                varDef.resName="box";
                varDef.isStatic=ctx.define_box().Static()!=null;
                varDef.isCollider = ctx.define_box().Collider()!=null;

                if(ctx.define_box().box_having_expr()!=null) {
                    for(SceneMaxParser.Box_attrContext attr:ctx.define_box().box_having_expr().box_attributes().box_attr()) {

                        if(attr.model_attr()!=null) {
                            if(attr.model_attr().print_pos_attr()!=null) {
                                if(attr.model_attr().print_pos_attr().pos_axes()!=null) {

                                    if(attr.model_attr().print_pos_attr().pos_axes().exception!=null) {
                                        return null;
                                    }

                                    varDef.xExpr = attr.model_attr().print_pos_attr().pos_axes().print_pos_x().logical_expression();
                                    varDef.yExpr = attr.model_attr().print_pos_attr().pos_axes().print_pos_y().logical_expression();
                                    varDef.zExpr = attr.model_attr().print_pos_attr().pos_axes().print_pos_z().logical_expression();
                                } else {
                                    varDef.entityPos=new EntityPos();
                                    setEntityPos(varDef.entityPos,attr.model_attr().print_pos_attr().pos_entity());
                                }
                            } else if (attr.model_attr().init_rotate_attr()!=null) {
                                SceneMaxParser.Rot_axesContext rotAxes = attr.model_attr().init_rotate_attr().rot_axes();
                                if(rotAxes!=null) {
                                    varDef.rxExpr = rotAxes.rotate_x().logical_expression();//new ActionLogicalExpression(attr.init_rotate_attr().rotate_x().logical_expression(),prg);
                                    varDef.ryExpr = rotAxes.rotate_y().logical_expression();
                                    varDef.rzExpr = rotAxes.rotate_z().logical_expression();
                                } else {
                                    varDef.entityRot = attr.model_attr().init_rotate_attr().rot_entity().getText();
                                }
                            } else if(attr.model_attr().init_mass_attr()!=null) {
                                varDef.massExpr = attr.model_attr().init_mass_attr().logical_expression();
                            } else if(attr.model_attr().init_static_attr()!=null) {
                                varDef.isStatic=true;
                            } else if(attr.model_attr().init_hidden_attr()!=null) {
                                varDef.visible=false;
                            } else if(attr.model_attr().shadow_mode_attr()!=null) {
                                SceneMaxParser.Shadow_mode_optionsContext shadow_opts = attr.model_attr().shadow_mode_attr().shadow_mode_options();
                                if(shadow_opts.Cast()!=null) {
                                    varDef.shadowMode = 1;
                                } else if(shadow_opts.Receive()!=null) {
                                    varDef.shadowMode = 2;
                                } else {
                                    varDef.shadowMode = 3;
                                }
                            }
                        }


                        if(attr.box_specific_attr()!=null) {
                            if(attr.box_specific_attr().volume_size_attr()!=null) {
                                varDef.sizeX = attr.box_specific_attr().volume_size_attr().size_x().logical_expression();
                                varDef.sizeY = attr.box_specific_attr().volume_size_attr().size_y().logical_expression();
                                varDef.sizeZ = attr.box_specific_attr().volume_size_attr().size_z().logical_expression();
                            }

                            if(attr.box_specific_attr().material_attr()!=null) {
                                varDef.materialExpr = attr.box_specific_attr().material_attr().logical_expression();
                            }
                        }
                    }
                }

                return varDef;

            }

            public StatementDef visitSceneActions(SceneMaxParser.SceneActionsContext ctx) {
                SceneActionCommand cmd = new SceneActionCommand();
                if(ctx.scene_actions().exception!=null) {
                    return null;
                }
                if (ctx.scene_actions().scene_action().scene_action_pause()!=null) {
                    cmd.pause=true;
                } else {
                    cmd.resume=true;
                }
                return cmd;
            }

            public StatementDef visitScreenActions(SceneMaxParser.ScreenActionsContext ctx) {
                ScreenActionCommand cmd = new ScreenActionCommand();
                cmd.actionFullWindow = ctx.screen_actions().screen_action().mode_full()!=null;

                return cmd;
            }

            public StatementDef visitLightActions(SceneMaxParser.LightActionsContext ctx) {

                LighsActionCommand cmd = new LighsActionCommand();
                SceneMaxParser.Light_probeContext probe = ctx.light_actions().light_options().light_probe();
                if(probe!=null) {
                    cmd.name = probe.QUOTED_STRING().getText();
                    cmd.name=cmd.name.substring(1,cmd.name.length()-1);
                    if(probe.print_pos_attr()!=null) {
                        if( probe.print_pos_attr().pos_axes()!=null) {

                            if(probe.print_pos_attr().pos_axes().exception!=null) {
                                return null;
                            }

                            cmd.xExpr = probe.print_pos_attr().pos_axes().print_pos_x().logical_expression();
                            cmd.yExpr = probe.print_pos_attr().pos_axes().print_pos_y().logical_expression();
                            cmd.zExpr = probe.print_pos_attr().pos_axes().print_pos_z().logical_expression();
                        } else {
                            cmd.entityPos=probe.print_pos_attr().pos_entity().getText();
                        }
                    }
                }

                return cmd;

            }

            public StatementDef visitAudioStop(SceneMaxParser.AudioStopContext ctx) {
                PlayStopSoundCommand cmd = new PlayStopSoundCommand();
                cmd.sound = ctx.audio_stop().string_expr().getText();
                if(cmd.sound.length()>2) {
                    cmd.sound=cmd.sound.substring(1,cmd.sound.length()-1);
                } else {
                    cmd.sound="";
                }

                cmd.stop = true;
                return cmd;
            }

            public StatementDef visitAudioPlay(SceneMaxParser.AudioPlayContext ctx) {
                PlayStopSoundCommand cmd = new PlayStopSoundCommand();
                if(ctx.audio_play().string_or_logical_expr().string_expr()!=null) {
                    cmd.sound = ctx.audio_play().string_or_logical_expr().string_expr().getText();
                    cmd.sound = stripQutes(cmd.sound);

                } else {
                    cmd.soundExpr = ctx.audio_play().string_or_logical_expr().logical_expression();
                }


                cmd.loop = ctx.audio_play().Loop()!=null;
                if(cmd.sound!=null && !audioUsed.contains(cmd.sound)) {
                    audioUsed.add(cmd.sound);
                }
                cmd.requireResource = true;

                if(ctx.audio_play().audio_play_options()!=null) {
                    for(SceneMaxParser.Audio_play_optionContext opt : ctx.audio_play().audio_play_options().audio_play_option()) {
                        if(opt.Volume()!=null) {
                            cmd.volumeExpr = opt.logical_expression();
                        }
                    }
                }

                return cmd;
            }

            public StatementDef visitPlaySound(SceneMaxParser.PlaySoundContext ctx) {
                PlayStopSoundCommand cmd = new PlayStopSoundCommand();
                cmd.sound = ctx.play_sound().res_var_decl().getText();
                cmd.loop = ctx.play_sound().Loop()!=null;
                if(!audioUsed.contains(cmd.sound)) {
                    audioUsed.add(cmd.sound);
                }
                cmd.requireResource = true;
                return cmd;
            }

            public StatementDef visitParticleSystemActions(SceneMaxParser.ParticleSystemActionsContext ctx) {

                ParticleSystemCommand cmd = new ParticleSystemCommand();
                cmd.isAsync = ctx.particle_system_actions().async_expr()!=null;

                SceneMaxParser.Particle_system_effectContext effectCtx = ctx.particle_system_actions().particle_system_effect();
                if (effectCtx.Debris()!=null) {
                    cmd.type=ParticleSystemCommand.DEBRIS;
                } else if(effectCtx.Explosion()!=null) {
                    cmd.type=ParticleSystemCommand.EXPLOSION;
                } else if(effectCtx.Flash()!=null) {
                    cmd.type=ParticleSystemCommand.FLASH;
                    cmd.startSizeVal = .1f;
                    cmd.endSizeVal = 3.0f;
                } else if(effectCtx.ShockWave()!=null) {
                    cmd.type=ParticleSystemCommand.SHOCK_WAVE;
                } else if(effectCtx.SmokeTrail()!=null) {
                    cmd.type=ParticleSystemCommand.SMOKE_TRAIL;
                } else if(effectCtx.Spark()!=null) {
                    cmd.type=ParticleSystemCommand.SPARK;
                } else if(effectCtx.TimeOrbit()!=null) {
                    cmd.type = ParticleSystemCommand.TIME_ORBIT ;
                } else if(effectCtx.Flame()!=null) {
                    cmd.type = ParticleSystemCommand.FLAME ;
                }


                SceneMaxParser.Particle_system_action_showContext showCtx = ctx.particle_system_actions().particle_system_action().particle_system_action_show();
                SceneMaxParser.Particle_system_having_exprContext havingCtx = showCtx.particle_system_having_expr();
                if(havingCtx!=null) {
                    for(SceneMaxParser.Particle_system_attrContext attr : havingCtx.particle_system_attributes().particle_system_attr()) {

                        ParserRuleContext attrCtx = attr.print_pos_attr();
                        if(attrCtx!=null) {

                            if(attr.print_pos_attr().pos_entity()!=null) {
                                cmd.entityPos=new EntityPos();
                                setEntityPos(cmd.entityPos, attr.print_pos_attr().pos_entity());
                            } else {
                                cmd.pos = (SceneMaxParser.Print_pos_attrContext) attrCtx;
                            }

                            continue;
                        }

                        attrCtx = attr.psys_attr_gravity();
                        if(attrCtx!=null) {
                            cmd.gravity = (SceneMaxParser.Psys_attr_gravityContext) attrCtx;
                            continue;
                        }

                        attrCtx = attr.psys_attr_start_size();
                        if(attrCtx!=null) {
                            cmd.startSize = (SceneMaxParser.Psys_attr_start_sizeContext) attrCtx;
                            continue;
                        }

                        attrCtx = attr.psys_attr_end_size();
                        if(attrCtx!=null) {
                            cmd.endSize = (SceneMaxParser.Psys_attr_end_sizeContext) attrCtx;
                            continue;
                        }

                        attrCtx = attr.psys_attr_duration();
                        if(attrCtx!=null) {
                            cmd.time = (SceneMaxParser.Psys_attr_durationContext) attrCtx;
                            continue;
                        }

                        if(attr.psys_attr_radius()!=null) {
                            cmd.radiusValExpr = attr.psys_attr_radius().logical_expression();
                            continue;
                        } else if(attr.psys_attr_emissions()!=null) {
                            cmd.emissionsPerSecExpr = attr.psys_attr_emissions().emissions_per_second().logical_expression();
                            cmd.particlesPerEmissionExpr = attr.psys_attr_emissions().particles_per_emission().logical_expression();
                        } else if(attr.psys_attr_attach_to()!=null) {
                            cmd.attachToEntity = attr.psys_attr_attach_to().var_decl().getText();
                        }

                    }

                }

                return cmd;
            }

            public StatementDef visitWaterActions(SceneMaxParser.WaterActionsContext ctx) {

                SceneMaxParser.Water_action_showContext show = ctx.water_actions().water_action().water_action_show();
                if(show!=null) {
                    WaterShowCommand cmd = new WaterShowCommand();
                    if(show.water_having_expr()!=null) {
                        for(SceneMaxParser.Water_attrContext attr : show.water_having_expr().water_attributes().water_attr()) {
                            ParserRuleContext attrCtx = attr.print_pos_attr();
                            if(attrCtx!=null) {
                                cmd.pos = (SceneMaxParser.Print_pos_attrContext) attrCtx;
                            }

                            attrCtx = attr.water_depth_attr();
                            if(attrCtx!=null) {
                                cmd.depth = (SceneMaxParser.Water_depth_attrContext) attrCtx;
                            }

                            attrCtx = attr.water_plane_size_attr();
                            if(attrCtx!=null) {
                                cmd.size = (SceneMaxParser.Water_plane_size_attrContext) attrCtx;
                            }

                            attrCtx = attr.water_strength_attr();
                            if(attrCtx!=null) {
                                cmd.strength = (SceneMaxParser.Water_strength_attrContext) attrCtx;
                            }

                            attrCtx = attr.water_wave_speed_attr();
                            if(attrCtx!=null) {
                                cmd.speed = (SceneMaxParser.Water_wave_speed_attrContext) attrCtx;
                            }

                        }
                    }

                    return cmd;

                }

                return null;

            }

            public StatementDef visitCsharpInvoke(SceneMaxParser.CsharpInvokeContext ctx) {
                CSharpInvokeCommand cmd = new CSharpInvokeCommand();
                cmd.funcName = ctx.csharp_invoke().java_func_name().getText();
                cmd.className = ctx.csharp_invoke().valid_java_class_name().getText();
                if(ctx.csharp_invoke().func_invok_variables()!=null) {
                    cmd.params = ctx.csharp_invoke().func_invok_variables().logical_expression();
                }
                if(ctx.csharp_invoke().csharp_register()!=null) {
                    //cmd.targetRegister =
                    for(SceneMaxParser.Res_var_declContext reg: ctx.csharp_invoke().csharp_register().res_var_decl()) {
                        cmd.targetRegister.add(reg.getText());
                    }
                }

                DoBlockCommand doCmd = new DoBlockCommand();
                doCmd.amount="1";
                doCmd.loopType="times";
                ProgramDef prg = new ProgramDef();
                prg.parent = this.prg;
                prg.actions.add(cmd);
                doCmd.prg=prg;

                return doCmd;
            }

            public StatementDef visitUsingResource(SceneMaxParser.UsingResourceContext ctx) {

                if(!parseUsingResource) {
                    return null;
                }

                for(SceneMaxParser.Resource_declarationContext res : ctx.using_resource().resource_declaration()) {
                    boolean isModel = res.Model()!=null;
                    boolean isSprite = !isModel && res.Sprite()!=null;
                    boolean isAudio = !isModel && !isSprite && res.Audio()!=null;

                    for(SceneMaxParser.Res_var_declContext resName : res.res_var_decl()) {
                        String name = resName.getText();
                        if(isModel) {
                            if(!modelsUsed.contains(name)) {
                                modelsUsed.add(name);
                            }
                        } else if(isSprite) {
                            if(!spriteSheetUsed.contains(name)) {
                                spriteSheetUsed.add(name);
                            }
                        } else if(isAudio) {
                            if(!audioUsed.contains(name)) {
                                audioUsed.add(name);
                                PlayStopSoundCommand cmd = new PlayStopSoundCommand();
                                cmd.sound = name;
                                prg.requireResourceActions.add(cmd);
                            }
                        }
                    }
                }
                return null;
            }

            public StatementDef visitSkyBoxActions(SceneMaxParser.SkyBoxActionsContext ctx) {
                SkyBoxCommand cmd = new SkyBoxCommand();
                SceneMaxParser.Skybox_action_showContext showCtx = ctx.skybox_actions().skybox_action().skybox_action_show();
                if(showCtx!=null) {
                    cmd.isShow = true;

                    if(showCtx.regular_skybox()!=null) {
                        cmd.showExpr = showCtx.regular_skybox().QUOTED_STRING().getText();
                        cmd.showExpr=cmd.showExpr.substring(1,cmd.showExpr.length()-1);
                        if(!skyboxUsed.contains(cmd.showExpr)) {
                            skyboxUsed.add(cmd.showExpr);
                        }
                    } else {
                        cmd.isShowSolarSystem = true;
                        SceneMaxParser.Solar_systemContext solarSysCtx = showCtx.solar_system();
                        if(solarSysCtx.solar_system_having_expr()!=null) {

                            setSolarSystemOptions(cmd,solarSysCtx.solar_system_having_expr().solar_system_having_options().solar_system_option());
                        }
                    }
                } else {
                    SceneMaxParser.Skybox_setupContext setupCtx = ctx.skybox_actions().skybox_action().skybox_setup();
                    if(setupCtx!=null) {
                        cmd.isSetup=true;
                        setSolarSystemOptions(cmd, setupCtx.solar_system_setup_options().solar_system_option());

                    }

                }

                return cmd;

            }

            private void setSolarSystemOptions(SkyBoxCommand cmd, List<SceneMaxParser.Solar_system_optionContext> solarSystemOptions) {

                for (SceneMaxParser.Solar_system_optionContext opt : solarSystemOptions) {
                    if (opt.cloud_flattening() != null) {
                        cmd.cloudFlatteningExpr = opt.cloud_flattening().logical_expression();
                    } else if (opt.cloudiness() != null) {
                        cmd.cloudinessExpr = opt.cloudiness().logical_expression();
                    } else if(opt.hour_of_day()!=null) {
                        cmd.hourOfDayExpr = opt.hour_of_day().logical_expression();
                    }
                }
            }


            public ActionStatementBase visitReturnStatement(SceneMaxParser.ReturnStatementContext var1) {
                StopBlockCommand cmd = new StopBlockCommand();
                cmd.returnAction=true;
                return cmd;
            }


            public StatementDef visitStop_statement(SceneMaxParser.Stop_statementContext ctx) {
                StopBlockCommand cmd = new StopBlockCommand();
                return cmd;
            }

            public StatementDef visitWaitForStatement(SceneMaxParser.WaitForStatementContext ctx) {
                WaitForCommand cmd = new WaitForCommand();
                if(ctx.wait_for_statement().wait_for_options().wait_for_expression()!=null) {
                    cmd.waitForExpr = ctx.wait_for_statement().wait_for_options().wait_for_expression().logical_expression();
                } else {
                    String src = ctx.wait_for_statement().wait_for_options().wait_for_input().input_source().getText().toLowerCase();
                    if(src.startsWith("key ")) {
                        cmd.inputType="key";
                        cmd.inputKey = src.replace("key ","");
                    }

                }
                return cmd;
            }

            public StatementDef visitWaitStatement(SceneMaxParser.WaitStatementContext ctx) {
                WaitStatementVisitor v = new WaitStatementVisitor(prg);
                return ctx.accept(v);
            }

            public StatementDef visitPrintStatement(SceneMaxParser.PrintStatementContext ctx) {
                PrintStatementVisitor v = new PrintStatementVisitor(prg);
                return ctx.accept(v);
            }

            public StatementDef visitModifyVar(SceneMaxParser.ModifyVarContext ctx) {

                ModifyVariableVisitor v = new ModifyVariableVisitor(prg);
                VariableAssignmentCommand cmd = ctx.accept(v);
                return cmd;
            }

            public StatementDef visitInputStatement(SceneMaxParser.InputStatementContext ctx) {

                InputStatementCommand cmd = new InputStatementCommand();

                if(ctx.input().go_condition()!=null) {
                    cmd.goExpr = ctx.input().go_condition().logical_expression();
                }

                String src = ctx.input().input_source().getText().toLowerCase();
                if(src.startsWith("key ")) {
                    cmd.inputType="key";
                    cmd.inputKey = src.replace("key ","");
                } else if(src.startsWith("mouse ")) {
                    cmd.inputType="mouse";
                    cmd.inputKey = src.toLowerCase().trim();
                    if(ctx.input().on_entity()!=null) {
                        cmd.targetVar = ctx.input().on_entity().res_var_decl().getText();

                    }
                }

                cmd.once = ctx.input().input_action().is_pressed_action().Once()!=null;
                DoBlockCommand doBlock = new DoBlockVisitor(prg).visit(ctx.input().do_block());
                doBlock.isSecondLevelReturnPoint = true;
                cmd.doBlock = doBlock;

                return cmd;
            }

            public StatementDef visitCheckStaticStatement(SceneMaxParser.CheckStaticStatementContext ctx) {

                CheckIsStaticCommand cmd = new CheckIsStaticCommand();
                SceneMaxParser.Check_staticContext chk = ctx.check_static();
                DoBlockCommand doBlock = new DoBlockVisitor(prg).visit(chk.do_block());
                cmd.doBlock = doBlock;
                VariableDef vd = prg.getVar(chk.var_decl().getText());
                cmd.varDef=vd;
                cmd.timeExpr = chk.logical_expression();

                return cmd;
            }

            public StatementDef visitCollisionStatement(SceneMaxParser.CollisionStatementContext ctx) {
                CollisionStatementVisitor v = new CollisionStatementVisitor(prg);
                CollisionStatementCommand cmd = ctx.accept(v);
                return cmd;
            }

            public StatementDef visitIfStatement(SceneMaxParser.IfStatementContext ctx) {

                IfStatementVisitor v = new IfStatementVisitor(prg);
                IfStatementCommand cmd = ctx.accept(v);
                return cmd;
            }


//            public JavaNewInstanceDecl visitJavaNewInstance(SceneMaxParser.JavaNewInstanceContext ctx) {
//
//                JavaNewInstanceVisitor v = new JavaNewInstanceVisitor(prg);
//                JavaNewInstanceDecl decl = ctx.accept(v);
//
//                return decl;
//            }

//            public StatementDef visitJavaClass(SceneMaxParser.JavaClassContext ctx) {
//                DefineJavaClassVisitor v = new DefineJavaClassVisitor(prg);
//                JavaClassDef cmd = ctx.accept(v);
//                return cmd;
//            }

            public StatementDef visitFunction_statement(SceneMaxParser.Function_statementContext ctx) {
                FunctionBlockVisitor v = new FunctionBlockVisitor(prg);
                FunctionBlockDef cmd = ctx.accept(v);

                return cmd;
            }

            public StatementDef visitFunctionInvocation(SceneMaxParser.FunctionInvocationContext ctx) {
                FunctionInvocationVisitor v = new FunctionInvocationVisitor();
                FunctionInvocationCommand cmd = ctx.accept(v);
                return cmd;
            }

            public StatementDef visitDo_block(SceneMaxParser.Do_blockContext ctx) {
                DoBlockVisitor v = new DoBlockVisitor(prg);
                DoBlockCommand cmd = ctx.accept(v);
                return cmd;
            }


            public StatementDef visitDefSpriteImplicit(SceneMaxParser.DefSpriteImplicitContext ctx) {

                SpriteDef def = new SpriteDef();

                try {
                    SceneMaxParser.Dynamic_model_typeContext dynamicDef = ctx.define_sprite_implicit().dynamic_model_type();    //res_var_decl().getText();
                    if(dynamicDef.res_var_decl()!=null) {
                        def.name = dynamicDef.res_var_decl().getText();
                    } else {
                        def.nameExpr = dynamicDef.dynamic_model_type_name().logical_expression();
                    }

                    def.varName = ctx.define_sprite_implicit().var_decl().getText();

                    if(ctx.define_sprite_implicit().sprite_having_expr()!=null) {

                        for(SceneMaxParser.Sprite_attrContext spriteAttr : ctx.define_sprite_implicit().sprite_having_expr().sprite_attributes().sprite_attr()) {

                            if(spriteAttr.billboard_attr()!=null) {
                                def.isBillboard = true;
                            } else if(spriteAttr.rows_def()!=null) {
                                def.rows = (int)Double.parseDouble(spriteAttr.rows_def().number().getText());
                            } else if(spriteAttr.cols_def()!=null) {
                                def.cols = (int)Double.parseDouble(spriteAttr.cols_def().number().getText());
                            } else if(spriteAttr.print_pos_attr()!=null) {
                                if(spriteAttr.print_pos_attr().pos_axes()!=null) {

                                    if(spriteAttr.print_pos_attr().pos_axes().exception!=null) {
                                        return null;
                                    }

                                    def.xExpr = spriteAttr.print_pos_attr().pos_axes().print_pos_x().logical_expression();
                                    def.yExpr = spriteAttr.print_pos_attr().pos_axes().print_pos_y().logical_expression();
                                    def.zExpr = spriteAttr.print_pos_attr().pos_axes().print_pos_z().logical_expression();
                                } else {
                                    //def.entityPos=spriteAttr.print_pos_attr().pos_entity().getText();
                                    def.entityPos=new EntityPos();
                                    setEntityPos(def.entityPos,spriteAttr.print_pos_attr().pos_entity());
                                }
                            } else if (spriteAttr.init_scale_attr()!=null) {
                                def.scaleExpr = spriteAttr.init_scale_attr().logical_expression();
                            }

                        }

                    }

                    if(!spriteSheetUsed.contains(def.name)) {
                        spriteSheetUsed.add(def.name);
                    }


                    return def;
                }catch(Exception e){
                    return null;
                }

            }

            public StatementDef visitDefSphere(SceneMaxParser.DefSphereContext ctx) {

                String varName = ctx.define_sphere().res_var_decl().getText();
                SphereVariableDef varDef = new SphereVariableDef();
                varDef.varName = varName;
                varDef.resName="sphere";
                varDef.isStatic=ctx.define_sphere().Static()!=null;
                varDef.isCollider = ctx.define_sphere().Collider()!=null;

                if(ctx.define_sphere().sphere_having_expr()!=null) {

                    for(SceneMaxParser.Sphere_attrContext attr:ctx.define_sphere().sphere_having_expr().sphere_attributes().sphere_attr()) {
                        if(attr.model_attr()!=null) {
                            if(attr.model_attr().print_pos_attr()!=null) {
                                if(attr.model_attr().print_pos_attr().pos_axes()!=null) {
                                    if(attr.model_attr().print_pos_attr().pos_axes().exception!=null) {
                                        return null;
                                    }
                                    varDef.xExpr = attr.model_attr().print_pos_attr().pos_axes().print_pos_x().logical_expression();
                                    varDef.yExpr = attr.model_attr().print_pos_attr().pos_axes().print_pos_y().logical_expression();
                                    varDef.zExpr = attr.model_attr().print_pos_attr().pos_axes().print_pos_z().logical_expression();
                                } else {
                                    //varDef.entityPos=attr.model_attr().print_pos_attr().pos_entity().getText();
                                    varDef.entityPos=new EntityPos();
                                    setEntityPos(varDef.entityPos,attr.model_attr().print_pos_attr().pos_entity());
                                }
                            } else if (attr.model_attr().init_rotate_attr()!=null) {
                                SceneMaxParser.Rot_axesContext rotAxes = attr.model_attr().init_rotate_attr().rot_axes();
                                if(rotAxes!=null) {
                                    varDef.rxExpr = rotAxes.rotate_x().logical_expression();//new ActionLogicalExpression(attr.init_rotate_attr().rotate_x().logical_expression(),prg);
                                    varDef.ryExpr = rotAxes.rotate_y().logical_expression();
                                    varDef.rzExpr = rotAxes.rotate_z().logical_expression();
                                } else {
                                    varDef.entityRot = attr.model_attr().init_rotate_attr().rot_entity().getText();
                                }
                            } else if(attr.model_attr().init_mass_attr()!=null) {
                                varDef.massExpr = attr.model_attr().init_mass_attr().logical_expression();
                            } else if(attr.model_attr().init_static_attr()!=null) {
                                varDef.isStatic=true;
                            } else if(attr.model_attr().init_hidden_attr()!=null) {
                                varDef.visible=false;
                            } else if(attr.model_attr().shadow_mode_attr()!=null) {
                                SceneMaxParser.Shadow_mode_optionsContext shadow_opts = attr.model_attr().shadow_mode_attr().shadow_mode_options();
                                if(shadow_opts.Cast()!=null) {
                                    varDef.shadowMode = 1;
                                } else if(shadow_opts.Receive()!=null) {
                                    varDef.shadowMode = 2;
                                } else {
                                    varDef.shadowMode = 3;
                                }
                            }

                        } else if(attr.sphere_specific_attr()!=null) {
                            if(attr.sphere_specific_attr().radius_attr()!=null) {
                                varDef.radiusExpr = attr.sphere_specific_attr().radius_attr().logical_expression();
                            } else if(attr.sphere_specific_attr().material_attr()!=null) {
                                varDef.materialExpr = attr.sphere_specific_attr().material_attr().logical_expression();
                            }
                        }
                    }
                }

                return varDef;
            }

            @Override
            public StatementDef visitDefine_variable(SceneMaxParser.Define_variableContext ctx) {

                String varName = ctx.var_decl().getText();
                String resName = null;
                SceneMaxParser.Logical_expressionContext resNameExpr = null;
                if (ctx.dynamic_model_type().res_var_decl()!=null) {
                    resName = ctx.dynamic_model_type().res_var_decl().getText();
                } else {
                    resNameExpr = ctx.dynamic_model_type().dynamic_model_type_name().logical_expression();
                }

                VariableDef varDef = new VariableDef();
                varDef.varName = varName;
                varDef.resName=resName;
                varDef.resNameExpr=resNameExpr;
                varDef.varLineNum = ctx.var_decl().getStart().getLine();
                varDef.isVehicle=ctx.Vehicle()!=null;
                varDef.isStatic = ctx.Static()!=null;
                varDef.isDynamic = ctx.Dynamic()!=null;

                if(ctx.scene_entity_having_expr()!=null) {
                    for (SceneMaxParser.Model_attrContext attr : ctx.scene_entity_having_expr().model_attributes().model_attr()) {
                        if(attr.print_pos_attr()!=null) {
                            if(attr.print_pos_attr().pos_axes()!=null) {

                                if(attr.print_pos_attr().pos_axes().exception!=null) {
                                    return null;
                                }

                                varDef.xExpr = attr.print_pos_attr().pos_axes().print_pos_x().logical_expression();
                                varDef.yExpr = attr.print_pos_attr().pos_axes().print_pos_y().logical_expression();
                                varDef.zExpr = attr.print_pos_attr().pos_axes().print_pos_z().logical_expression();
                            } else {
                                //varDef.entityPos = attr.print_pos_attr().pos_entity().getText();
                                varDef.entityPos=new EntityPos();
                                setEntityPos(varDef.entityPos,attr.print_pos_attr().pos_entity());
                            }
                        } else if(attr.init_turn_attr()!=null ) {

                            varDef.useVerbalTurn = true;
                            SceneMaxParser.Turn_dirContext dirCtx = attr.init_turn_attr().turn_dir();
                            String dir = dirCtx==null?"left":dirCtx.getText();
                            if(dir.equalsIgnoreCase("left")) {
                                varDef.ryExpr = attr.init_turn_attr().turn_degrees().logical_expression();
                                varDef.rotDir=1;
                            } else if(dir.equalsIgnoreCase("right")) {
                                varDef.ryExpr = attr.init_turn_attr().turn_degrees().logical_expression();
                                varDef.rotDir=-1;
                            } else if(dir.equalsIgnoreCase("forward")) {
                                varDef.rxExpr = attr.init_turn_attr().turn_degrees().logical_expression();
                                varDef.rotDir=1;
                            } if(dir.equalsIgnoreCase("backward")) {
                                varDef.rotDir=-1;
                                varDef.rxExpr = attr.init_turn_attr().turn_degrees().logical_expression();
                            }

                        } else if(attr.init_rotate_attr()!=null) {
                            SceneMaxParser.Rot_axesContext rotAxes = attr.init_rotate_attr().rot_axes();
                            if(rotAxes!=null) {
                                varDef.rxExpr = rotAxes.rotate_x().logical_expression();
                                varDef.ryExpr = rotAxes.rotate_y().logical_expression();
                                varDef.rzExpr = rotAxes.rotate_z().logical_expression();
                            } else {
                                varDef.entityRot = attr.init_rotate_attr().rot_entity().getText();
                            }
                        } else if(attr.init_scale_attr()!=null) {
                            varDef.scaleExpr = attr.init_scale_attr().logical_expression();
                        } else if(attr.init_mass_attr()!=null) {
                            varDef.massExpr = attr.init_mass_attr().logical_expression();
                        } else if(attr.init_static_attr()!=null) {
                            varDef.isStatic=true;
                        } else if(attr.init_joints_attr()!=null) {

                            varDef.joints = new ArrayList<>();
                            for(TerminalNode str : attr.init_joints_attr().QUOTED_STRING()) {
                                varDef.joints.add(str.getText());
                            }

                        } else if(attr.init_hidden_attr()!=null) {
                            varDef.visible=false;
                        } else if(attr.shadow_mode_attr()!=null) {
                            SceneMaxParser.Shadow_mode_optionsContext shadow_opts = attr.shadow_mode_attr().shadow_mode_options();
                            if(shadow_opts.Cast()!=null) {
                                varDef.shadowMode = 1;
                            } else if(shadow_opts.Receive()!=null) {
                                varDef.shadowMode = 2;
                            } else {
                                varDef.shadowMode = 3;
                            }
                        } else if(attr.calibration_attr()!=null) {
                            varDef.calibration = new EntityPos(attr.calibration_attr().pos_axes().print_pos_x().logical_expression(),
                                    attr.calibration_attr().pos_axes().print_pos_y().logical_expression(),
                                    attr.calibration_attr().pos_axes().print_pos_z().logical_expression());
                        } else if(attr.collision_shape_attr()!=null) {
                            String shapeType = attr.collision_shape_attr().collision_shape_options().getText().toLowerCase();
                            if(shapeType.equals("box")) {
                                varDef.collisionShape = VariableDef.COLLISION_SHAPE_BOX;
                            } else if(shapeType.equals("boxes")) {
                                varDef.collisionShape = VariableDef.COLLISION_SHAPE_BOXES;
                            }
                        }
                    }
                }

                return varDef;
            }

            @Override
            public ActionStatementBase visitActionStatement(SceneMaxParser.ActionStatementContext ctx) {

                ActionStatementBase action = ctx.action_statement().action_operation().accept(new ActionStatementVisitor(prg));
                if(action!=null) {
                    action.isAsync = ctx.action_statement().async_expr() != null;
                }
                return action;
            }

        }


        private class WaitStatementVisitor extends SceneMaxBaseVisitor<WaitStatementCommand> {

            private final ProgramDef prg;

            public WaitStatementVisitor(ProgramDef prg) {
                this.prg=prg;

            }

            public WaitStatementCommand visitWaitStatement(SceneMaxParser.WaitStatementContext ctx) {

                WaitStatementCommand cmd=new WaitStatementCommand();
                cmd.waitExpr = ctx.wait_statement().logical_expression();//new ActionLogicalExpression(ctx.wait_statement().logical_expression(),prg);

                return cmd;
            }
        }

        private class PrintStatementVisitor extends SceneMaxBaseVisitor<PrintStatementCommand> {

            private final ProgramDef prg;

            public PrintStatementVisitor(ProgramDef prg) {
                this.prg=prg;

            }

            public PrintStatementCommand visitPrintStatement(SceneMaxParser.PrintStatementContext ctx) {
                PrintStatementCommand cmd = new PrintStatementCommand();
                cmd.printChannel = ctx.print_statement().res_var_decl().getText();
                cmd.text = ctx.print_statement().print_text_expr().logical_expression();// new ActionLogicalExpression(ctx.print_statement().print_text_expr().logical_expression(),prg);
                for(SceneMaxParser.Print_attrContext attr:ctx.print_statement().print_attr()) {
                    if(attr.print_pos_attr()!=null) {
                        if(attr.print_pos_attr().pos_axes()!=null) {

                            if(attr.print_pos_attr().pos_axes().exception!=null) {
                                return null;
                            }

                            cmd.x = attr.print_pos_attr().pos_axes().print_pos_x().logical_expression();//new ActionLogicalExpression(attr.print_pos_attr().print_pos_x().logical_expression(),prg);
                            cmd.y = attr.print_pos_attr().pos_axes().print_pos_y().logical_expression();//new ActionLogicalExpression(attr.print_pos_attr().print_pos_y().logical_expression(),prg);
                            cmd.z = attr.print_pos_attr().pos_axes().print_pos_z().logical_expression();//new ActionLogicalExpression(attr.print_pos_attr().print_pos_z().logical_expression(),prg);
                        }

                    } else if(attr.print_color_attr()!=null) {
                        cmd.color = attr.print_color_attr().SystemColor().getText();
                    } else if(attr.print_font_size_attr()!=null) {
                        cmd.fontSize=attr.print_font_size_attr().logical_expression();//new ActionLogicalExpression(attr.print_font_size_attr().logical_expression(),prg);
                    } else if(attr.print_append_attr()!=null) {
                        cmd.append=true;
                    } else if(attr.print_font_attr()!=null) {
                        cmd.font=attr.print_font_attr().QUOTED_STRING().getText();
                        if(cmd.font.length()>2) {
                            cmd.font=cmd.font.substring(1,cmd.font.length()-1);
                            if(!fontsUsed.contains(cmd.font)) {
                                fontsUsed.add(cmd.font);
                            }
                        }
                    }


                }

                return cmd;
            }


        }

//        private static class JavaNewInstanceVisitor extends SceneMaxBaseVisitor<JavaNewInstanceDecl> {
//
//            private final ProgramDef prg;
//
//            public JavaNewInstanceVisitor(ProgramDef prg) {
//                this.prg=prg;
//
//            }
//
////            public JavaNewInstanceDecl visitJava_var_name_and_assignment(SceneMaxParser.Java_var_name_and_assignmentContext ctx) {
////
////                JavaNewInstanceDecl retval = new JavaNewInstanceDecl();
////
////                //retval.type=this.varType;
//////                if(ctx.java_type_decl()!=null) {
//////                    retval.type = ctx.java_type_decl().valid_java_class_name().ID().getText();
//////                }
////
////                retval.varName = ctx.res_var_decl().getText();
////
////                if(ctx.java_assignment_decl()!=null) {
////                    if(ctx.java_assignment_decl().java_assignment_expr()!=null &&
////                            ctx.java_assignment_decl().java_assignment_expr().new_instance_expr()!=null) {
////                        retval.val = ctx.java_assignment_decl().java_assignment_expr().new_instance_expr().res_var_decl().getText();
////                        retval.isClassType = true;
////                    } else {
////                        if(ctx.java_assignment_decl().java_assignment_expr()!=null) {
////                            // assignments will happen in run-time
////                            retval.valExpr = ctx.java_assignment_decl().java_assignment_expr().logical_expression();
////                        }
////                    }
////                }
////
////                return retval;
////            }
//
////            public JavaNewInstanceDecl visitJavaNewInstance(SceneMaxParser.JavaNewInstanceContext ctx) {
////
////                JavaNewInstanceDecl rv = new JavaNewInstanceDecl();
////                rv.type = ctx.java_new_instance().java_type_decl().getText();
////                rv.siblings = new ArrayList<>();
////
////                for(SceneMaxParser.Java_var_name_and_assignmentContext c : ctx.java_new_instance().java_var_name_and_assignment()) {
////
////                    JavaNewInstanceDecl retval = new JavaNewInstanceDecl();
////                    retval.varName = c.res_var_decl().getText();
////
////                    if(c.java_assignment_decl()!=null) {
////                        if(c.java_assignment_decl().java_assignment_expr()!=null &&
////                                c.java_assignment_decl().java_assignment_expr().new_instance_expr()!=null) {
////                            retval.val = c.java_assignment_decl().java_assignment_expr().new_instance_expr().res_var_decl().getText();
////                            retval.isClassType = true;
////                        } else {
////                            if(c.java_assignment_decl().java_assignment_expr()!=null) {
////                                // assignments will happen in run-time
////                                retval.valExpr = c.java_assignment_decl().java_assignment_expr().logical_expression();
////                            }
////                        }
////                    }
////
////                    rv.siblings.add(retval);
////
////                }
////
////                return rv;
////            }
//
//
//
//        }

//        private static class DefineJavaClassVisitor extends SceneMaxBaseVisitor<JavaClassDef> {
//
//            private final ProgramDef prg;
//
//            public DefineJavaClassVisitor(ProgramDef prg) {
//                this.prg=prg;
//            }
//
//            public JavaClassDef visitJavaClass(SceneMaxParser.JavaClassContext ctx) {
//
//                JavaClassDef classDef = new JavaClassDef();
//
//                if(ctx.java_class().scope_expr()!=null) {
//                    classDef.scope = ctx.java_class().scope_expr().getText();
//                }
//                classDef.name = ctx.java_class().valid_java_class_name().ID().getText();
//                if(ctx.java_class().class_extends_expr()!=null) {
//                    classDef.extendsRes = ctx.java_class().class_extends_expr().extendable_objects().getText();
//                }
//
//                return classDef;
//            }
//
//        }

        private class ModifyVariableVisitor extends SceneMaxBaseVisitor<VariableAssignmentCommand> {
            private final ProgramDef prg;

            public ModifyVariableVisitor(ProgramDef prg) {
                this.prg=prg;
            }

            public VariableAssignmentCommand visitModifyVar(SceneMaxParser.ModifyVarContext ctx) {

                VariableAssignmentCommand cmd = new VariableAssignmentCommand();

                String var = ctx.modify_variable().var_decl().getText();
                VariableDef varDef = prg.getVar(var);
                if(varDef==null) {
                    // report error variable not exists
                    prg.syntaxErrors.add("Variable '"+var+"' not exists");
                }

                cmd.var = varDef;
                cmd.expression = ctx.modify_variable().java_assignment_decl().java_assignment_expr();//new ActionLogicalExpression(ctx.modify_variable().java_assignment_decl().java_assignment_expr().logical_expression(),prg);

                return cmd;
            }

        }

        private class CollisionStatementVisitor extends SceneMaxBaseVisitor<CollisionStatementCommand> {
            private final ProgramDef prg;

            public CollisionStatementVisitor(ProgramDef prg) {
                this.prg=prg;
            }

            public CollisionStatementCommand visitCollisionStatement(SceneMaxParser.CollisionStatementContext ctx) {

                CollisionStatementCommand cmd = new CollisionStatementCommand();

                if(ctx.collision().go_condition()!=null) {
                    cmd.goExpr = ctx.collision().go_condition().logical_expression();
                }

                String var1 = ctx.collision().var_decl(0).getText();
                String var2 = ctx.collision().var_decl(1).getText();
                String part1=null,part2=null;

                if(ctx.collision().collision_joint_1()!=null)  {
                    part1 = ctx.collision().collision_joint_1().QUOTED_STRING().getText();
                    if(part1.length()>2) {
                        part1=part1.substring(1,part1.length()-1);
                    }
                    //part1="."+part1;
                }

                if(ctx.collision().collision_joint_2()!=null)  {
                    part2 = ctx.collision().collision_joint_2().QUOTED_STRING().getText();
                    if(part2.length()>2) {
                        part2=part2.substring(1,part2.length()-1);
                    }
                    //part2="."+part2;
                }


                VariableDef vd = prg.getVar(var1);
                if(vd==null) {
                    prg.syntaxErrors.add("Object '"+var1+"' not defined at line:"+ctx.collision().var_decl(0).getStart().getLine());
                    return null;
                }
                cmd.varDef1=vd;
                cmd.part1=part1;
                vd = prg.getVar(var2);
                if(vd==null) {
                    prg.syntaxErrors.add("Object '"+var2+"' not defined at line:"+ctx.collision().var_decl(1).getStart().getLine());
                    return null;
                }
                cmd.varDef2=vd;
                cmd.part2=part2;

                DoBlockCommand doBlock = new DoBlockVisitor(prg).visit(ctx.collision().do_block());
                doBlock.isSecondLevelReturnPoint=true;
                cmd.doBlock = doBlock;

                return cmd;
            }

        }

        private class IfStatementVisitor extends SceneMaxBaseVisitor<IfStatementCommand> {

            private final ProgramDef prg;

            public IfStatementVisitor(ProgramDef prg) {
                this.prg=prg;
            }

            public IfStatementCommand visitIfStatement(SceneMaxParser.IfStatementContext ctx) {

                IfStatementCommand cmd = new IfStatementCommand();
                DoBlockCommand doBlock = new DoBlockVisitor(prg).visit(ctx.if_statement().do_block());
                cmd.doBlock = doBlock;
                cmd.expression = ctx.if_statement().logical_expression();// new ActionLogicalExpression(ctx.if_statement().logical_expression(),prg);

                // Add the else block
                if(ctx.if_statement().else_expr()!=null) {
                    cmd.elseCmd=new DoBlockVisitor(prg).visit(ctx.if_statement().else_expr().do_block());

                }

                // Add the else-if block(s)
                if(ctx.if_statement().else_if_expr()!=null) {
                    cmd.elseIfCommands=new ArrayList<>();
                    for(int i=0;i<ctx.if_statement().else_if_expr().size();++i) {
                        SceneMaxParser.Else_if_exprContext elseIfCtx = ctx.if_statement().else_if_expr(i);

                        IfStatementCommand elseIfCmd = new IfStatementCommand();
                        elseIfCmd.expression = elseIfCtx.logical_expression();//new ActionLogicalExpression(elseIfCtx.logical_expression(),prg);
                        elseIfCmd.doBlock = new DoBlockVisitor(prg).visit(elseIfCtx.do_block());

                        cmd.elseIfCommands.add(elseIfCmd);
                    }
                }

                return cmd;
            }

        }

        private class FunctionBlockVisitor extends SceneMaxBaseVisitor<FunctionBlockDef> {
            private final ProgramDef prg;

            public FunctionBlockVisitor(ProgramDef prg) {
                this.prg=prg;
            }

            public FunctionBlockDef visitFunction_statement(SceneMaxParser.Function_statementContext ctx) {

                FunctionBlockDef cmd = new FunctionBlockDef();
                List<String> inParams = new ArrayList<>();
                if(ctx.func_variables()!=null) {
                    for(SceneMaxParser.Res_var_declContext varCtx: ctx.func_variables().res_var_decl()) {
                        String varName = varCtx.getText();
                        inParams.add(varName);
                    }
                }


                SceneMaxParser.Do_blockContext dbctx = ctx.do_block();
                if(dbctx==null) {
                    prg.syntaxErrors.add("Syntax error in 'Do' block at line:"+ctx.getStart().getLine());
                    return null;
                }
                DoBlockCommand doBlock = new DoBlockVisitor(prg,inParams).visit(dbctx);
                doBlock.isReturnPoint=true; // functions are always return points (when using "return" command)
                cmd.doBlock = doBlock;
                cmd.name = ctx.java_func_name().getText();

                if(ctx.go_condition()!=null) {
                    cmd.goExpr = ctx.go_condition().logical_expression();
                }

                return cmd;
            }

        }

        private class FunctionInvocationVisitor extends SceneMaxBaseVisitor<FunctionInvocationCommand> {

            public FunctionInvocationCommand visitFunctionInvocation(SceneMaxParser.FunctionInvocationContext ctx) {

                FunctionInvocationCommand cmd = new FunctionInvocationCommand();
                cmd.funcName=ctx.function_invocation().java_func_name().getText();
                if(ctx.function_invocation().func_invok_variables()!=null) {
                    cmd.params=ctx.function_invocation().func_invok_variables().logical_expression();

                }

                if(ctx.function_invocation().every_time_expr()!=null) {
                    cmd.intervalExpr = ctx.function_invocation().every_time_expr().logical_expression();
                }

                cmd.isAsync = ctx.function_invocation().async_expr()!=null;

                return cmd;
            }

        }

        private class DoBlockVisitor extends SceneMaxBaseVisitor<DoBlockCommand> {

            private final ProgramDef prg;
            private List<String> inParams=null;

            public DoBlockVisitor(ProgramDef prg) {
                this.prg=prg;
            }

            public DoBlockVisitor(ProgramDef prg, List<String> inParams) {
                this.prg=prg;
                this.inParams = inParams;
            }

            @Override
            public DoBlockCommand visitDo_block(SceneMaxParser.Do_blockContext ctx) {

                DoBlockCommand loop = new DoBlockCommand();
                loop.inParams=this.inParams;

                if(ctx.go_condition()!=null) {
                    loop.goExpr = ctx.go_condition().logical_expression();
                }
                if(ctx.amount_of_times_expr()!=null) {
                    loop.amountExpr = ctx.amount_of_times_expr().logical_expression();//new ActionLogicalExpression(ctx.amount_of_times_expr().logical_expression(),prg);
                    loop.loopType = ctx.amount_of_times_expr().times_or_seconds().getText();
                } else {
                    // defaults for do block
                    loop.amount="1";
                    loop.loopType="times";
                }

                if(ctx.program_statements()!=null) {
                    prg.inParams=inParams;
                    loop.prg = new ProgramStatementsVisitor(prg,null).visit(ctx.program_statements());
                    loop.isAsync = ctx.async_expr() != null;
                } else {
                    prg.syntaxErrors.add("Do block has invalid statements at: ");
                    return null;
                }
                
                return loop;
            }

        }

        private class ActionStatementVisitor extends SceneMaxBaseVisitor<ActionStatementBase> {

            private final ProgramDef prg;


            public ActionStatementVisitor(ProgramDef prg) {
                this.prg=prg;
            }

            public ActionStatementBase visitReplayAction(SceneMaxParser.ReplayActionContext ctx) {

                ReplayCommand cmd = new ReplayCommand();
                cmd.targetVar = ctx.replay().var_decl().getText();

                SceneMaxParser.Replay_switch_toContext switchTo = ctx.replay().replay_options().replay_switch_to();
                if(switchTo!=null) {
                    cmd.option = ReplayCommand.SWITCH_TO;
                    cmd.dataArrayName = switchTo.replay_data().var_decl().getText();


                    if(switchTo.replay_attributes()!=null) {
                        for(SceneMaxParser.Replay_attributeContext attr:switchTo.replay_attributes().replay_attribute()) {
                            if(attr.replay_attr_offset()!=null) {
                                String axis = attr.replay_attr_offset().all_axes_names().getText();
                                if(axis.equalsIgnoreCase("x")) {
                                    cmd.offsetXExpr = attr.replay_attr_offset().logical_expression();
                                } else if(axis.equalsIgnoreCase("y")) {
                                    cmd.offsetYExpr = attr.replay_attr_offset().logical_expression();
                                } else if(axis.equalsIgnoreCase("z")) {
                                    cmd.offsetZExpr = attr.replay_attr_offset().logical_expression();
                                } else if(axis.equalsIgnoreCase("rx")) {
                                    cmd.offsetRXExpr = attr.replay_attr_offset().logical_expression();
                                } else if(axis.equalsIgnoreCase("ry")) {
                                    cmd.offsetRYExpr = attr.replay_attr_offset().logical_expression();
                                } else if(axis.equalsIgnoreCase("rz")) {
                                    cmd.offsetRZExpr = attr.replay_attr_offset().logical_expression();
                                }

                            }
                        }
                    }


                    return cmd;
                }

                if(ctx.replay().replay_options().replay_stop()!=null) {
                    cmd.option = ReplayCommand.STOP;
                    return cmd;
                }

                if(ctx.replay().replay_options().replay_pause()!=null) {
                    cmd.option = ReplayCommand.PAUSE;
                    return cmd;
                }

                if(ctx.replay().replay_options().replay_resume()!=null) {
                    cmd.option = ReplayCommand.RESUME;
                    return cmd;
                }

                if(ctx.replay().replay_options().replay_change_speed()!=null) {
                    cmd.option = ReplayCommand.CHANGE_SPEED;
                    cmd.speedExpr = ctx.replay().replay_options().replay_change_speed().speed_expr().logical_expression();
                    return cmd;
                }

                SceneMaxParser.Replay_commandContext replayCmdCtx = ctx.replay().replay_options().replay_command();
                if(replayCmdCtx!=null) {
                    cmd.dataArrayName = replayCmdCtx.replay_data().var_decl().getText();
                    if(replayCmdCtx.starting_at_expr()!=null) {
                        cmd.startAtExpr = replayCmdCtx.starting_at_expr().logical_expression();
                    }

                    cmd.speedExpr = replayCmdCtx.speed_expr().logical_expression();

                    if(replayCmdCtx.loop_expr()!=null) {
                        cmd.loopExpr = replayCmdCtx.loop_expr().logical_expression();
                    }

                }

                if(ctx.replay().replay_attributes()!=null) {
                    for(SceneMaxParser.Replay_attributeContext attr:ctx.replay().replay_attributes().replay_attribute()) {
                        if(attr.replay_attr_offset()!=null) {
                            String axis = attr.replay_attr_offset().all_axes_names().getText();
                            if(axis.equalsIgnoreCase("x")) {
                                cmd.offsetXExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("y")) {
                                cmd.offsetYExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("z")) {
                                cmd.offsetZExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("rx")) {
                                cmd.offsetRXExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("ry")) {
                                cmd.offsetRYExpr = attr.replay_attr_offset().logical_expression();
                            } else if(axis.equalsIgnoreCase("rz")) {
                                cmd.offsetRZExpr = attr.replay_attr_offset().logical_expression();
                            }

                        }
                    }
                }

                return cmd;
            }

            public ActionStatementBase visitDelete(SceneMaxParser.DeleteContext ctx) {
                KillEntityCommand cmd = new KillEntityCommand();
                cmd.targetVar = ctx.var_decl().getText();
                return cmd;
            }

            public ActionStatementBase visitSetMaterialAction(SceneMaxParser.SetMaterialActionContext ctx) {

                SetMaterialCommand cmd = new SetMaterialCommand();
                cmd.materialNameExpr = ctx.set_material_action().logical_expression();
                cmd.targetVar = ctx.set_material_action().var_decl().getText();
                return cmd;
            }

            public ActionStatementBase visitCharacterActions(SceneMaxParser.CharacterActionsContext ctx) {
                if(ctx.character_actions().character_action().character_action_jump()!=null) {
                    CharacterJumpCommand cmd = new CharacterJumpCommand();
                    cmd.targetVar = ctx.character_actions().var_decl().getText();

                    if(ctx.character_actions().character_action().character_action_jump().speed_of_expr()!=null) {
                        cmd.speedExpr = ctx.character_actions().character_action().character_action_jump().speed_of_expr().logical_expression();
                    }

                    return cmd;
                }
                return null;
            }

            public ActionStatementBase visitScaleStatement(SceneMaxParser.ScaleStatementContext ctx) {

                ActionScaleCommand cmd = new ActionScaleCommand();
                cmd.targetVar = ctx.scale().var_decl().getText();
                cmd.scaleExpr = ctx.scale().logical_expression();

                return cmd;
            }


            public ActionStatementBase visitDettachParent(SceneMaxParser.DettachParentContext ctx) {

                DettachFromParentCommand cmd = new DettachFromParentCommand();
                cmd.targetVar = ctx.detach_parent().var_decl().getText();
                return cmd;
            }

            public ActionStatementBase visitAttachTo(SceneMaxParser.AttachToContext ctx) {

                AttachToCommand cmd ;
                String tvName = ctx.attach_to().var_decl(0).getText();

                cmd = new AttachToCommand();
                cmd.entityNameToAttach = tvName;
                cmd.targetVar = ctx.attach_to().var_decl(1).getText();
                if(ctx.attach_to().joint_name()!=null) {
                    cmd.jointName = ctx.attach_to().joint_name().getText();
                    cmd.jointName = stripQutes(cmd.jointName);

                }

                SceneMaxParser.Attach_to_having_exprContext havingCtx = ctx.attach_to().attach_to_having_expr();
                if(havingCtx!=null) {
                    for(SceneMaxParser.Attach_to_having_optionContext opt:havingCtx.attach_to_options().attach_to_having_option()) {
                        if(opt.print_pos_attr()!=null) {
                            if(opt.print_pos_attr().pos_axes()!=null) {

                                if(opt.print_pos_attr().pos_axes().exception!=null) {
                                    return null;
                                }

                                cmd.xExpr = opt.print_pos_attr().pos_axes().print_pos_x().logical_expression();
                                cmd.yExpr = opt.print_pos_attr().pos_axes().print_pos_y().logical_expression();
                                cmd.zExpr = opt.print_pos_attr().pos_axes().print_pos_z().logical_expression();
                            }
                        }

                        if(opt.init_rotate_attr()!=null) {
                            SceneMaxParser.Rot_axesContext rotAxes = opt.init_rotate_attr().rot_axes();
                            if(rotAxes!=null) {
                                cmd.rxExpr = rotAxes.rotate_x().logical_expression();
                                cmd.ryExpr = rotAxes.rotate_y().logical_expression();
                                cmd.rzExpr = rotAxes.rotate_z().logical_expression();
                            } else {
                                cmd.entityRot = opt.init_rotate_attr().rot_entity().getText();
                            }
                        }
                    }
                }

                return cmd;
            }

            public ActionStatementBase visitTurnVerbalTo(SceneMaxParser.TurnVerbalToContext ctx) {

                LookAtCommand cmd = new LookAtCommand();

                if(ctx.turn_verbal_to().move_to_target().position_statement()!=null) {
                    SceneMaxParser.Position_statementContext posCtx = ctx.turn_verbal_to().move_to_target().position_statement();
                    cmd.posStatement = parsePositionStatement(posCtx);

                } else if(ctx.turn_verbal_to().move_to_target().ID()!=null) {
                    cmd.moveToTarget = ctx.turn_verbal_to().move_to_target().ID().getText();
                } else {
                    SceneMaxParser.Pos_axesContext posAxes = ctx.turn_verbal_to().move_to_target().pos_axes();
                    if(posAxes!=null) {
                        if(posAxes.exception==null) {
                            cmd.moveToTargetXExpr = posAxes.print_pos_x().logical_expression();
                            cmd.moveToTargetYExpr = posAxes.print_pos_y().logical_expression();
                            cmd.moveToTargetZExpr = posAxes.print_pos_z().logical_expression();
                        } else {

                            return null;
                        }
                    }
                }

                cmd.targetVar = ctx.turn_verbal_to().var_decl().getText();
                cmd.varLineNum = ctx.turn_verbal_to().var_decl().getStart().getLine();
                if(ctx.turn_verbal_to().speed_expr()!=null) {
                    cmd.speedExpr = ctx.turn_verbal_to().speed_expr().logical_expression();
                }
                cmd.validate(prg);

                return cmd;

            }


            public ActionStatementBase visitTurnStatement(SceneMaxParser.TurnStatementContext ctx) {

                ActionCommandRotate rotate = new ActionCommandRotate();
                String axis = "";
                String numSign = "";

                if(ctx.turn_verbal().loop_expr()!=null) {
                    rotate.loopExpr = ctx.turn_verbal().loop_expr().logical_expression();
                }

                SceneMaxParser.Turn_dirContext dirCtx = ctx.turn_verbal().turn_dir();
                String dir = dirCtx==null?"left":dirCtx.getText();
                if(dir.equalsIgnoreCase("left")) {
                    axis="y";
                    numSign="+";
                } else if(dir.equalsIgnoreCase("right")) {
                    axis="y";
                    numSign="-";
                } else if(dir.equalsIgnoreCase("forward")) {
                    axis="x";
                    numSign="+";
                } if(dir.equalsIgnoreCase("backward")) {
                    axis="x";
                    numSign="-";
                }

                String var = ctx.turn_verbal().var_decl().getText();
                ActionCommandRotate cmd = new ActionCommandRotate();
                VariableDef vd = prg.getVar(var);
                cmd.varDef=vd;
                cmd.targetVar = var;
                cmd.axis = axis;
                cmd.numSign = numSign;
                cmd.numExpr = ctx.turn_verbal().turn_degrees().logical_expression();
                cmd.speedExp = ctx.turn_verbal().speed_expr().logical_expression();

                rotate.statements.add(cmd);
                return rotate;

            }

            public ActionStatementBase visitRollStatement(SceneMaxParser.RollStatementContext ctx) {

                ActionCommandRotate rotate = new ActionCommandRotate();
                String axis = "z";
                String numSign = "";

                SceneMaxParser.Turn_dirContext dirCtx = ctx.roll_verbal().turn_dir();
                String dir = dirCtx==null?"left":dirCtx.getText();

                if(ctx.roll_verbal().loop_expr()!=null) {
                    rotate.loopExpr = ctx.roll_verbal().loop_expr().logical_expression();
                }

                if(dir.equalsIgnoreCase("left")) {
                    numSign="-";
                } else if(dir.equalsIgnoreCase("right")) {
                    numSign="+";
                }

                String var = ctx.roll_verbal().var_decl().getText();
                ActionCommandRotate cmd = new ActionCommandRotate();
                VariableDef vd = prg.getVar(var);
                cmd.varDef=vd;
                cmd.targetVar = var;
                cmd.axis = axis;
                cmd.numSign = numSign;
                cmd.numExpr = ctx.roll_verbal().turn_degrees().logical_expression();
                cmd.speedExp = ctx.roll_verbal().speed_expr().logical_expression();

                rotate.statements.add(cmd);
                return rotate;

            }

            public ActionStatementBase visitMoveVerbalStatement(SceneMaxParser.MoveVerbalStatementContext ctx) {

                String var = ctx.move_verbal().var_decl().getText();
                String axis = "z";
                String numSign = "";
                String dir = ctx.move_verbal().move_direction().getText();

                VariableDef vd = prg.getVar(var);
                int verbalCommand=0;

                if(dir.equalsIgnoreCase("left")) {
                    verbalCommand=ActionCommandMove.VERBAL_MOVE_LEFT;
                    axis="x";
                    numSign="+";
                } else if(dir.equalsIgnoreCase("right")) {
                    verbalCommand=ActionCommandMove.VERBAL_MOVE_RIGHT;
                    axis="x";
                    numSign="-";
                } else if(dir.equalsIgnoreCase("up")) {
                    verbalCommand=ActionCommandMove.VERBAL_MOVE_UP;
                    axis="y";
                    numSign="+";
                } else if(dir.equalsIgnoreCase("down")) {
                    verbalCommand=ActionCommandMove.VERBAL_MOVE_DOWN;
                    axis="y";
                    numSign="-";
                } else if(dir.equalsIgnoreCase("forward")) {
                    verbalCommand=ActionCommandMove.VERBAL_MOVE_FORWARD;
                    axis="z";
                    numSign="+";
                } else if(dir.equalsIgnoreCase("backward")) {
                    verbalCommand=ActionCommandMove.VERBAL_MOVE_BACKWARD;
                    axis="z";
                    numSign="-";
                }

                SceneMaxParser.Speed_exprContext speedExprCtx = ctx.move_verbal().speed_expr();
                if (speedExprCtx == null) {
                    return null;
                }

                ActionCommandMove move = new ActionCommandMove();
                ActionCommandMove cmd = new ActionCommandMove();

                cmd.verbalCommand=verbalCommand;
                cmd.varDef=vd;
                cmd.targetVar = var;

                cmd.axis = axis;
                cmd.numSign = numSign;
                cmd.numExpr = ctx.move_verbal().logical_expression();
                cmd.speedExpr=speedExprCtx.logical_expression();

                move.statements.add(cmd);

                return move;

            }

            public ActionStatementBase visitMoveTo(SceneMaxParser.MoveToContext ctx) {
                MoveToCommand cmd = new MoveToCommand();

                if(ctx.move_to().looking_at_expr()!=null) {
                    cmd.lookingAtStatement = parsePositionStatement(ctx.move_to().looking_at_expr().position_statement());
                }

                if(ctx.move_to().move_to_target().position_statement()!=null) {
                    SceneMaxParser.Position_statementContext posCtx = ctx.move_to().move_to_target().position_statement();
                    cmd.posStatement = parsePositionStatement(posCtx);

                } else if(ctx.move_to().move_to_target().ID()!=null) {
                    cmd.moveToTarget = ctx.move_to().move_to_target().ID().getText();
                } else {
                    SceneMaxParser.Pos_axesContext posAxes = ctx.move_to().move_to_target().pos_axes();
                    if(posAxes!=null) {
                        if(posAxes.exception==null) {
                            cmd.moveToTargetXExpr = posAxes.print_pos_x().logical_expression();
                            cmd.moveToTargetYExpr = posAxes.print_pos_y().logical_expression();
                            cmd.moveToTargetZExpr = posAxes.print_pos_z().logical_expression();
                        } else {
//                            String err="Unrecognized target definition in line: "+posAxes.exception.getOffendingToken().getLine();
//                            prg.syntaxErrors.add(err);
                            return null;
                        }
                    }
                }

                cmd.targetVar = ctx.move_to().var_decl().getText();
                cmd.varLineNum = ctx.move_to().var_decl().getStart().getLine();
                cmd.extraDistanceExpr = ctx.move_to().logical_expression();
                cmd.speedExpr = ctx.move_to().speed_expr().logical_expression();

                cmd.validate(prg);

                return cmd;
            }

            public ActionStatementBase visitRayCheckStatement(SceneMaxParser.RayCheckStatementContext ctx) {
                RayCheckCommand cmd = new RayCheckCommand();
                cmd.targetGroup = ctx.ray_check().var_decl().getText();

                SceneMaxParser.Ray_check_fromContext from = ctx.ray_check().ray_check_from();
                if(from!=null) {
                    SceneMaxParser.Pos_axesContext axes = from.pos_axes();
                    if(axes!=null) {
                        cmd.posX = axes.print_pos_x().logical_expression();
                        cmd.posY = axes.print_pos_y().logical_expression();
                        cmd.posZ = axes.print_pos_z().logical_expression();
                    } else {
                        cmd.entityPos = from.pos_entity().getText();
                    }
                }

                if(ctx.ray_check()!=null) {

                    if(ctx.ray_check().exception!=null) {
                        prg.syntaxErrors.add(ctx.ray_check().exception.getMessage());
                        return null;
                    }

                    DoBlockCommand doBlock = new DoBlockVisitor(prg).visit(ctx.ray_check().do_block());
                    cmd.doBlock = doBlock;
                }
                return cmd;
            }

            public ActionStatementBase visitUserDataStatement(SceneMaxParser.UserDataStatementContext ctx) {
                SetUserDataCommand cmd = new SetUserDataCommand();
                cmd.varName = ctx.user_data().var_decl().getText();
                cmd.fieldName = ctx.user_data().field_name().getText();
                cmd.dataExpr = ctx.user_data().logical_expression() ;

                return cmd;

            }

            public ActionStatementBase visitVelocityStatement(SceneMaxParser.VelocityStatementContext ctx) {
                ChangeVelocityCommand cmd = new ChangeVelocityCommand();
                cmd.targetVar=ctx.velocity().var_decl().getText();
                cmd.velocityExpr = ctx.velocity().logical_expression();
                return cmd;
            }

            public ActionStatementBase visitMassStatement(SceneMaxParser.MassStatementContext ctx) {
                ChangeMassCommand cmd = new ChangeMassCommand();
                cmd.varName = ctx.mass().var_decl().getText();
                cmd.massExpr = ctx.mass().logical_expression();

                return cmd;

            }

            public ActionStatementBase visitClearModes(SceneMaxParser.ClearModesContext ctx) {
                ClearModeCommand cmd = new ClearModeCommand();
                cmd.varName = ctx.clear_modes().var_decl().getText();
                for(SceneMaxParser.Clear_mode_optionContext mode : ctx.clear_modes().clear_modes_options().clear_mode_option()) {
                    if(mode.character_mode()!=null) {
                        cmd.modeToClear=SwitchModeCommand.CHARACTER;
                    }
                }
                return cmd;
            }

            public ActionStatementBase visitSwitch_mode(SceneMaxParser.Switch_modeContext ctx) {

                SwitchModeCommand cmd = new SwitchModeCommand();
                cmd.varName = ctx.var_decl().getText();
                if(ctx.switch_options().switch_to_character()!=null){
                    cmd.switchTo=SwitchModeCommand.CHARACTER;
                    SceneMaxParser.Character_mode_attributesContext charAttr = ctx.switch_options().switch_to_character().character_mode_attributes();
                    if(charAttr!=null) {
                        for(SceneMaxParser.Character_mode_attributeContext attr:charAttr.character_mode_attribute()) {
                            if(attr.scalar_gravity()!=null) {
                                cmd.gravityExpr = attr.scalar_gravity().logical_expression();
                            }
                        }
                    }


                } else if(ctx.switch_options().switch_to_rigid_body()!=null){
                    cmd.switchTo=SwitchModeCommand.RIGID_BODY;
               } else if(ctx.switch_options().switch_to_ragdoll()!=null) {
                    cmd.switchTo=SwitchModeCommand.RAGDOLL;
                } else if(ctx.switch_options().switch_to_kinematic()!=null) {
                    cmd.switchTo=SwitchModeCommand.KINEMATIC;
                } else if(ctx.switch_options().switch_to_floating()!=null) {
                    cmd.switchTo=SwitchModeCommand.FLOATING;
                }


                return cmd;
            }

            public ActionStatementBase visitPosStatement(SceneMaxParser.PosStatementContext ctx) {

                ActionCommandPos cmd = new ActionCommandPos();
                cmd.varName = ctx.pos().var_decl().getText();
                if(ctx.pos().position_statement()!=null) {
                    SceneMaxParser.Position_statementContext posCtx = ctx.pos().position_statement();
                    cmd.posStatement = parsePositionStatement(posCtx);
                } else if(ctx.pos().pos_axes()!=null) {

                    if(ctx.pos().pos_axes().exception!=null) {
                        return null;
                    }

                    cmd.x = ctx.pos().pos_axes().print_pos_x().logical_expression();
                    cmd.y = ctx.pos().pos_axes().print_pos_y().logical_expression();
                    cmd.z = ctx.pos().pos_axes().print_pos_z().logical_expression();
                } else {
                    cmd.entityPos = ctx.pos().pos_entity().getText();
                }

                return cmd;
            }

            public ActionStatementBase visitVehicleEngineSetup(SceneMaxParser.VehicleEngineSetupContext ctx) {
                VehicleSetupCommand cmd = new VehicleSetupCommand();
                cmd.setupEngine=true;
                cmd.targetVar = ctx.vehicle_engine_setup().var_decl().getText();

                if(ctx.vehicle_engine_setup().engine_options().engine_power_option()!=null) {
                    cmd.enginePowerExp = ctx.vehicle_engine_setup().engine_options().engine_power_option().logical_expression();
                } else if(ctx.vehicle_engine_setup().engine_options().engine_breaking_option()!=null) {
                    cmd.engineBreakingExp = ctx.vehicle_engine_setup().engine_options().engine_breaking_option().logical_expression();
                } else if(ctx.vehicle_engine_setup().engine_options().engine_action_start_off()!=null) {
                    cmd.engineOnOff = ctx.vehicle_engine_setup().engine_options().engine_action_start_off().getText();
                }

                return cmd;
            }

            public ActionStatementBase visitVehicleInputSetup(SceneMaxParser.VehicleInputSetupContext ctx) {
                VehicleSetupCommand cmd = new VehicleSetupCommand();
                cmd.setupInput=true;
                cmd.targetVar = ctx.vehicle_input_setup().var_decl().getText();

                if(ctx.vehicle_input_setup().vehicle_input_setup_options()!=null) {
                    InputMapping im = new InputMapping();
                    for (SceneMaxParser.Vehicle_input_optionContext opt : ctx.vehicle_input_setup().vehicle_input_setup_options().vehicle_input_option()) {

                        String key = opt.input_source().getText();
                        key=key.replace("key ","").toLowerCase();
                        String action = opt.vehicle_action().getText();
                        cmd.addInputSource(action, im.getKeyVal(key));
                    }

                } else if(ctx.vehicle_input_setup().on_off_options()!=null) {
                    cmd.inputOnOffCommand = ctx.vehicle_input_setup().on_off_options().getText();
                }
                return cmd;
            }

            public ActionStatementBase visitVehicleSetup(SceneMaxParser.VehicleSetupContext ctx) {
                VehicleSetupCommand cmd = new VehicleSetupCommand();
                cmd.targetVar = ctx.vehicle_setup().var_decl().getText();
                cmd.setupFront = ctx.vehicle_setup().vehicle_side().getText().equalsIgnoreCase("front");

                for(SceneMaxParser.Vehicle_optionContext opt:ctx.vehicle_setup().vehicle_setup_options().vehicle_option()) {
                    if(opt.vehicle_friction_option()!=null) {
                        cmd.frictionExpr = opt.vehicle_friction_option().logical_expression();
                    } else if(opt.vehicle_suspension_option()!=null) {

                        for(SceneMaxParser.Specific_suspension_optionContext suspOpt:opt.vehicle_suspension_option().specific_suspension_options().specific_suspension_option()) {
                            if(suspOpt.specific_suspension_opt_compression()!=null) {
                                cmd.compressionExpr=suspOpt.specific_suspension_opt_compression().logical_expression();
                            } else if(suspOpt.specific_suspension_opt_damping()!=null) {
                                cmd.dampingExpr = suspOpt.specific_suspension_opt_damping().logical_expression();
                            } else if(suspOpt.specific_suspension_opt_stiffness()!=null) {
                                cmd.stiffnessExpr = suspOpt.specific_suspension_opt_stiffness().logical_expression();
                            } else if(suspOpt.specific_suspension_opt_length()!=null) {
                                cmd.lengthExpr = suspOpt.specific_suspension_opt_length().logical_expression();
                            }
                        }
                    }

                }
                return cmd;

            }

            public ActionStatementBase visitAccelerateStatement(SceneMaxParser.AccelerateStatementContext ctx) {
                AccelerateCommand cmd = new AccelerateCommand();
                cmd.targetVar=ctx.accelerate().var_decl().getText();
                cmd.accelerateExp=ctx.accelerate().logical_expression();
                return cmd;
            }

            public ActionStatementBase visitBrakeStatement(SceneMaxParser.BrakeStatementContext ctx) {
                CarBrakeCommand cmd = new CarBrakeCommand();
                cmd.targetVar=ctx.brake().var_decl().getText();
                cmd.brakeExp=ctx.brake().logical_expression();
                return cmd;
            }

            public ActionStatementBase visitTurboStatement(SceneMaxParser.TurboStatementContext ctx) {
                CarTurboCommand cmd = new CarTurboCommand();
                cmd.targetVar=ctx.turbo().var_decl().getText();
                cmd.xExpr = ctx.turbo().print_pos_x().logical_expression();
                cmd.yExpr = ctx.turbo().print_pos_y().logical_expression();
                cmd.zExpr = ctx.turbo().print_pos_z().logical_expression();

                return cmd;
            }

            public ActionStatementBase visitSteerStatement(SceneMaxParser.SteerStatementContext ctx) {
                CarSteerCommand cmd = new CarSteerCommand();
                cmd.targetVar=ctx.steer().var_decl().getText();
                cmd.steerExp=ctx.steer().logical_expression();
                return cmd;
            }

            public ActionStatementBase visitResetStatement(SceneMaxParser.ResetStatementContext ctx) {
                CarResetCommand cmd = new CarResetCommand();
                cmd.targetVar=ctx.reset_vehicle().var_decl().getText();
                return cmd;
            }



            public ActionStatementBase visitHideStatement(SceneMaxParser.HideStatementContext ctx) {
                ActionCommandShowHide cmd = new ActionCommandShowHide();
                cmd.varName = ctx.hide().var_decl().getText();
                cmd.show=false;
                if(ctx.hide().show_options()!=null) {
                    if(ctx.hide().show_options().show_info_option()!=null) {
                        cmd.info = true;
                    } else if(ctx.hide().show_options().Wireframe()!=null) {
                        cmd.wireframe = true;
                    } else if(ctx.hide().show_options().Speedo()!=null) {
                        cmd.speedo = true;
                    } else if(ctx.hide().show_options().Tacho()!=null) {
                        cmd.tacho = true;
                    }  else if(ctx.hide().show_options().show_joints_option()!=null) {
                        cmd.joints = true;
                    } else if(ctx.hide().show_options().Outline()!=null) {
                        cmd.outline = true;
                    }
                }
                return cmd;
            }

            public ActionStatementBase visitShowStatement(SceneMaxParser.ShowStatementContext ctx) {
                ActionCommandShowHide cmd = new ActionCommandShowHide();
                cmd.varName = ctx.show().var_decl().getText();
                cmd.show=true;
                if(ctx.show().show_options()!=null) {
                    if(ctx.show().show_options().show_info_option()!=null) {
                        cmd.info = true;
                        if(ctx.show().show_options().show_info_option().show_info_attributes()!=null) {
                            for(SceneMaxParser.Show_info_attributeContext attr: ctx.show().show_options().show_info_option().show_info_attributes().show_info_attribute()) {
                                if(attr.file_attr()!=null) {
                                    String txt = attr.file_attr().QUOTED_STRING().getText();
                                    if(txt.length()>0) {
                                        txt=txt.substring(1,txt.length()-1);
                                    }
                                    cmd.infoDumpFile = txt;
                                }
                            }
                        }
                    } else if(ctx.show().show_options().Wireframe()!=null) {
                        cmd.wireframe = true;
                    } else if(ctx.show().show_options().show_axis_option()!=null) {
                        cmd.axisX = ctx.show().show_options().show_axis_option().X()!=null;
                        cmd.axisY = ctx.show().show_options().show_axis_option().Y()!=null;
                        cmd.axisZ = ctx.show().show_options().show_axis_option().Z()!=null;
                    } else if(ctx.show().show_options().Speedo()!=null) {
                        cmd.speedo = true;
                    } else if(ctx.show().show_options().Tacho()!=null) {
                        cmd.tacho = true;
                    } else if(ctx.show().show_options().show_joints_option()!=null) {
                        cmd.joints = true;
                        SceneMaxParser.Show_joints_attributesContext joints_attr = ctx.show().show_options().show_joints_option().show_joints_attributes();
                        if(joints_attr!=null) {
                            for(SceneMaxParser.Show_joints_attributeContext attr:joints_attr.show_joints_attribute()) {
                                if(attr.scalar_size_attr()!=null) {
                                    cmd.showJointsSizeExpr = attr.scalar_size_attr().logical_expression();
                                }
                            }
                        }
                    } else if(ctx.show().show_options().Outline()!=null) {
                        cmd.outline = true;
                    }
                }

                return cmd;
            }

            public ActionStatementBase visitStopStatement(SceneMaxParser.StopStatementContext ctx) {
                String var=ctx.stop().var_decl().getText();
                ActionCommandStop cmd = new ActionCommandStop();
                VariableDef vd = prg.getVar(var);
                cmd.varDef=vd;
                cmd.targetVar = var;

                return cmd;
            }


            public ActionStatementBase visitDirectionalMove(SceneMaxParser.DirectionalMoveContext ctx) {

                DirectionalMoveCommand cmd = new DirectionalMoveCommand();

                String var = ctx.directional_move().var_decl().getText();
                VariableDef vd = prg.getVar(var);
                cmd.varDef=vd;
                cmd.targetVar = var;
                SceneMaxParser.Move_directionContext md = ctx.directional_move().move_direction();

                if(md.Forward()!=null) {
                    cmd.direction=DirectionalMoveCommand.FORWARD;
                } else if(md.Backward()!=null) {
                    cmd.direction=DirectionalMoveCommand.BACKWARD;
                } else if(md.Left()!=null) {
                    cmd.direction=DirectionalMoveCommand.LEFT;
                } else if(md.Right()!=null) {
                    cmd.direction=DirectionalMoveCommand.RIGHT;
                }

                cmd.distanceExpr = ctx.directional_move().logical_expression(0);
                if(ctx.directional_move().logical_expression().size()>1) {
                    cmd.timeExpr = ctx.directional_move().logical_expression(1);
                }
                return cmd;

            }

            public ActionStatementBase visitMoveStatement(SceneMaxParser.MoveStatementContext ctx) {
                try {
                    String var = ctx.move().var_decl().getText();

                    SceneMaxParser.Speed_exprContext speedExprCtx = ctx.move().speed_expr();
                    if (speedExprCtx == null) {
                        return null;
                    }

                    ActionCommandMove move = new ActionCommandMove();

                    for (int i = 0; i < ctx.move().axis_expr().size(); ++i) {
                        SceneMaxParser.Axis_exprContext actx = ctx.move().axis_expr().get(i);
                        String axis = actx.axis_id().getText();
                        String numSign = actx.number_sign().getText();

                        ActionCommandMove cmd = new ActionCommandMove();
                        VariableDef vd = prg.getVar(var);
                        cmd.varDef=vd;
                        cmd.targetVar = var;

                        cmd.axis = axis;
                        cmd.numSign = numSign;
                        cmd.numExpr = actx.logical_expression();//new ActionLogicalExpression(actx.logical_expression(),prg);
                        cmd.speedExpr=speedExprCtx.logical_expression();//speedExpr;

                        move.statements.add(cmd);

                    }
                    return move;
                }catch(Exception e){
                    return null;
                }
            }


            public ActionStatementBase visitRotateReset(SceneMaxParser.RotateResetContext ctx) {
                RotateResetCommand cmd = new RotateResetCommand();
                cmd.xExpr = ctx.rotate_reset().print_pos_x().logical_expression();
                cmd.yExpr = ctx.rotate_reset().print_pos_y().logical_expression();
                cmd.zExpr = ctx.rotate_reset().print_pos_z().logical_expression();
                cmd.targetVar=ctx.rotate_reset().var_decl().getText();
                return cmd;
            }

            @Override
            public ActionStatementBase visitRotateToStatement(SceneMaxParser.RotateToStatementContext ctx) {

                ActionCommandRotateTo cmd = new ActionCommandRotateTo();
                cmd.targetVar = ctx.rotate_to().var_decl().getText();
                cmd.axis = ctx.rotate_to().axis_name().getText();
                cmd.speedExpr = ctx.rotate_to().speed_expr().logical_expression();
                cmd.rotateValExpr = ctx.rotate_to().logical_expression();
                return cmd;
            }

            public ActionStatementBase visitRecord(SceneMaxParser.RecordContext ctx) {
                ActionCommandRecord cmd = new ActionCommandRecord();
                cmd.targetVar = ctx.var_decl().getText();
                if(ctx.record_actions().record_transitions()!=null) {
                    cmd.recordType=ActionCommandRecord.RECORD_TYPE_TRANSITIONS;
                    cmd.everyTimeExpr = ctx.record_actions().record_transitions().every_time_expr().logical_expression();
                } else if(ctx.record_actions().record_save()!=null) {
                    cmd.recordType=ActionCommandRecord.RECORD_TYPE_SAVE;
                    cmd.savePath = ctx.record_actions().record_save().QUOTED_STRING().getText();
                    cmd.savePath = trimQuotedString(cmd.savePath);
                } else if(ctx.record_actions().record_stop()!=null) {
                    cmd.recordType=ActionCommandRecord.RECORD_TYPE_STOP;

                }

                return cmd;

            }

            @Override
            public ActionStatementBase visitRotate(SceneMaxParser.RotateContext ctx) {

                try {
                    String var = ctx.var_decl().getText();

                    SceneMaxParser.Speed_exprContext speedExprCtx = ctx.speed_expr();
                    if (speedExprCtx == null) {
                        return null;
                    }

                    ActionCommandRotate rotate = new ActionCommandRotate();

                    if(ctx.loop_expr()!=null) {
                        rotate.loopExpr = ctx.loop_expr().logical_expression();
                    }

                    for (int i = 0; i < ctx.axis_expr().size(); ++i) {
                        SceneMaxParser.Axis_exprContext actx = ctx.axis_expr().get(i);
                        String axis = actx.axis_id().getText();
                        String numSign = actx.number_sign() != null ? actx.number_sign().getText() : "+";

                        ActionCommandRotate cmd = new ActionCommandRotate();
                        VariableDef vd = prg.getVar(var);
                        cmd.varDef=vd;
                        cmd.targetVar = var;
                        cmd.axis = axis;
                        cmd.numSign = numSign;
                        cmd.numExpr = actx.logical_expression();//new ActionLogicalExpression(actx.logical_expression(),prg);

                        //cmd.speed = speed;
                        cmd.speedExp = speedExprCtx.logical_expression();//speedExp;

                        rotate.statements.add(cmd);

                    }
                    return rotate;
                }catch(Exception e){
                    return null;
                }
            }

            @Override
            public ActionStatementBase visitPlay(SceneMaxParser.PlayContext ctx) {

                try {
                    String var = ctx.var_decl().getText();
                    //System.out.println("Going to play frames on: " + var);

                    ActionCommandPlay cmd = new ActionCommandPlay();
                    VariableDef vd = prg.getVar(var);
                    cmd.varDef=vd;
                    cmd.targetVar = var;

                    cmd.fromFrameExpr = ctx.frames_expr().from_frame().logical_expression();//new ActionLogicalExpression(ctx.frames_expr().from_frame().logical_expression(),prg);
                    cmd.toFrameExpr = ctx.frames_expr().to_frame().logical_expression();//new ActionLogicalExpression(ctx.frames_expr().to_frame().logical_expression(),prg);
                    cmd.speedExpr = ctx.speed_expr()==null?null:ctx.speed_expr().logical_expression();//new ActionLogicalExpression(ctx.speed_expr().logical_expression(),prg);

                    if(ctx.play_duration_strategy()==null) {
                        cmd.durationStrategy=0;//once
                    } else if(ctx.play_duration_strategy().Once()!=null) {
                        cmd.durationStrategy=0;//once
                    } else if(ctx.play_duration_strategy().play_duration_loop_strategy()!=null) {
                        cmd.durationStrategy=2;//loop
                        if(ctx.play_duration_strategy().play_duration_loop_strategy().number()!=null) {
                            cmd.loopTimes = ctx.play_duration_strategy().play_duration_loop_strategy().number().getText();
                        } else {
                            cmd.loopTimes="-1";// endless loop
                        }
                    }
                    else {
                        cmd.durationStrategy=1;//time
                        //cmd.forTime = ctx.play_duration_strategy().for_time_expr().number().getText();
                        cmd.forTimeExpr = ctx.play_duration_strategy().for_time_expr().logical_expression();//new ActionLogicalExpression(ctx.play_duration_strategy().for_time_expr().logical_expression(),prg);
                    }

                    return cmd;
                }catch(Exception e) {
                    return null;
                }
            }

            public ActionStatementBase visitAnimate(SceneMaxParser.AnimateContext ctx) {

                AnimateOptionsCommand cmd = new AnimateOptionsCommand();
                cmd.targetVar=ctx.var_decl().getText();

                for(SceneMaxParser.Animation_attrContext attr: ctx.animation_attr()) {
                    if(attr.anim_attr_speed()!=null) {

                        cmd.speedExpr = attr.anim_attr_speed().logical_expression();

                        if(attr.anim_attr_speed().speed_for_seconds()!=null) {
                            cmd.forTimeExpr = attr.anim_attr_speed().speed_for_seconds().logical_expression();
                        }

                        if(attr.anim_attr_speed().when_frames_above()!=null) {
                            cmd.aboveFramesExpr = attr.anim_attr_speed().when_frames_above().logical_expression();
                        }
                    }
                }

                return cmd;

            }

            public ActionStatementBase visitAnimate_short(SceneMaxParser.Animate_shortContext ctx) {
                String var = ctx.var_decl().getText();
                int varLineNum = ctx.var_decl().getStart().getLine();

                boolean firstAnim = true;

                ActionCommandAnimate animate = new ActionCommandAnimate();
                animate.loop = ctx.Loop()!=null;
                if(ctx.go_condition()!=null) {
                    animate.goExpr = ctx.go_condition().logical_expression();
                }

                for (int i = 0; i < ctx.anim_expr().size(); ++i) {
                    SceneMaxParser.Anim_exprContext actx = ctx.anim_expr().get(i);

                    String anim = actx.animation_name().getText();
                    if(anim.startsWith("\"") && anim.length()>2) {
                        anim=anim.substring(1,anim.length()-1);
                    }

                    if (firstAnim) {
                        firstAnim = false;

                    }

                    ActionCommandAnimate cmd = new ActionCommandAnimate();
                    cmd.animationName = anim;
                    VariableDef vd = prg.getVar(var);
                    cmd.varDef=vd;
                    cmd.targetVar = var;
                    cmd.varLineNum=varLineNum;
                    cmd.speedExpr=actx.speed_of_expr()==null?null:actx.speed_of_expr().logical_expression();//speedExpr;
                    cmd.goExpr = animate.goExpr;
                    animate.statements.add(cmd);

                }

                return animate;
            }

        }


    }


    private List<DirectionVerb> parseDirectionVerbs(List<SceneMaxParser.Dir_statementContext> dirStatements) {

        List<DirectionVerb> directionVerbs = new ArrayList<>();
        for(SceneMaxParser.Dir_statementContext ds : dirStatements) {
            DirectionVerb dv = new DirectionVerb();
            if(ds.dir_verb().Forward()!=null) {
                dv.verb = DirectionVerb.FORWARD;
            } else if(ds.dir_verb().Backward()!=null) {
                dv.verb = DirectionVerb.BACKWARD;
            } else if(ds.dir_verb().Left()!=null) {
                dv.verb = DirectionVerb.LEFT;
            } else if(ds.dir_verb().Right()!=null) {
                dv.verb = DirectionVerb.RIGHT;
            } else if(ds.dir_verb().Up()!=null) {
                dv.verb = DirectionVerb.UP;
            } else if(ds.dir_verb().Down()!=null) {
                dv.verb = DirectionVerb.DOWN;
            }

            dv.valExp = ds.logical_expression();
            directionVerbs.add(dv);
        }

        return directionVerbs;
    }

    private PositionStatement parsePositionStatement(SceneMaxParser.Position_statementContext posCtx) {

        PositionStatement ps = new PositionStatement();
        ps.startEntity = posCtx.var_decl().getText();
        if(posCtx.dir_statement()!=null) {
            ps.directionVerbs=parseDirectionVerbs(posCtx.dir_statement());
        }

        return ps;

    }

    private void setEntityPos(EntityPos pos, SceneMaxParser.Pos_entityContext entityPos) {
        pos.entityName = entityPos.var_decl().getText();
        if(entityPos.collision_joint_1()!=null) {
            pos.entityJointName = entityPos.collision_joint_1().QUOTED_STRING().getText();
            if(pos.entityJointName.length()>2) {
                pos.entityJointName = pos.entityJointName.substring(1, pos.entityJointName.length() - 1);
            }
        }
    }

    private String stripQutes(String str) {

        if(str.length()>2) {
            str=str.substring(1,str.length()-1);
        } else {
            return "";
        }

        return str;
    }

    private void setParserSourceFileName(String file) {
        _sourceFileName=file;
    }

}
