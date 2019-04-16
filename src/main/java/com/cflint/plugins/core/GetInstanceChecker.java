package com.cflint.plugins.core;

import cfml.parsing.cfscript.script.CFDoWhileStatement;
import cfml.parsing.cfscript.script.CFExpressionStatement;
import cfml.parsing.cfscript.script.CFForInStatement;
import cfml.parsing.cfscript.script.CFForStatement;
import cfml.parsing.cfscript.script.CFIfStatement;
import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.cfscript.script.CFSwitchStatement;
import cfml.parsing.cfscript.script.CFWhileStatement;
import com.cflint.BugList;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;
import ro.fortsoft.pf4j.Extension;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Extension
public class GetInstanceChecker extends CFLintScannerAdapter {

    @Override
    public void expression(final CFScriptStatement expression, final Context context, final BugList bugs) {
        if (expression instanceof CFExpressionStatement) {
            final String code = ((CFExpressionStatement) expression).getExpression().Decompile(0);
            applyChecks(code, context);
        } else if (expression instanceof CFIfStatement) {
            final String code = ((CFIfStatement) expression).getCond().Decompile(0);
            applyChecks(code, context);
        } else if (expression instanceof CFSwitchStatement) {
            final String code = ((CFSwitchStatement) expression).getVariable().Decompile(0);
            applyChecks(code, context);
        } else if (expression instanceof CFForStatement) {
            final String code = ((CFForStatement) expression).getCond().Decompile(0);
            applyChecks(code, context);
        } else if (expression instanceof CFForInStatement) {
            final String code = ((CFForInStatement) expression).getStructure().Decompile(0);
            applyChecks(code, context);
        } else if (expression instanceof CFWhileStatement) {
            final String code = ((CFWhileStatement) expression).getCond().Decompile(0);
            applyChecks(code, context);
        } else if (expression instanceof CFDoWhileStatement) {
            final String code = ((CFDoWhileStatement) expression).getCondition().Decompile(0);
            applyChecks(code, context);
        }
    }

    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String code = element.getStartTag().getTagContent().toString();
        applyChecks(code, context);
    }

    private void applyChecks(final String code, final Context context) {
        Pattern pattern = Pattern.compile(
            "(?i)application\\.injector\\.getInstance\\(['\"]{1,2}([\\w.]+)['\"]{1,2}\\)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            File rootDirectory = identifyRootDirectory(new File(context.getFilename()));
            File component = new File(rootDirectory,
                                      matcher.group(1).replaceAll("\\.", "/") + ".cfc");

            if (!component.exists()) {
                context.addMessage("INCORRECT_COMPONENT_NAME", matcher.group(1));
            }
        }
    }

    private File identifyRootDirectory(File file) {
        if (file.getName().equals("Application.cfc")) {
            return file.getParentFile();
        }

        boolean isParentDirectory = file.getParentFile() != null && file.getParentFile().exists();

        if (file.isDirectory()) {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                if (f.getName().equals("Application.cfc")) {
                    return identifyRootDirectory(f);
                }
            }

            if (isParentDirectory) {
                return identifyRootDirectory(file.getParentFile());
            }
        } else if (isParentDirectory) {
            return identifyRootDirectory(file.getParentFile());
        }

        return null;
    }

}
