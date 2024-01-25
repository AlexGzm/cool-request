package com.cool.request.script;


import com.cool.request.Constant;
import com.cool.request.utils.ClassResourceUtils;
import com.cool.request.utils.StringUtils;
import com.cool.request.view.page.ScriptPage;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCodeEngine {
    private static final String REQUEST_REGEX = "(class\\s+CoolRequestScript\\s*\\{)";
    private static final String RESPONSE_REGEX = "(class\\s+CoolResponseScript\\s*\\{)";
    public static final String REQUEST_CLASS = "com.cool.request.script.CoolRequestScript";
    public static final String RESPONSE_CLASS = "com.cool.request.script.CoolResponseScript";
    private static final Logger LOG = Logger.getInstance(ScriptPage.class);
    private final InMemoryJavaCompiler inMemoryJavaCompiler = new InMemoryJavaCompiler();

    public boolean execRequest(Request request, String code, ILog iLog) {
        if (StringUtils.isEmpty(code)) {
            return true;
        }
        try {
            Map<String, Class<?>> result = javac(prependPublicToCoolRequestScript(REQUEST_REGEX, code), REQUEST_CLASS);
            if (result.get(REQUEST_CLASS) != null) {
                return invokeRequest(result.get(REQUEST_CLASS), request, iLog);
            }
        } catch (Exception e) {
            Messages.showErrorDialog(e.getMessage(),
                    e instanceof CompilationException ?
                            "Request Script Syntax Error ,Please Check!" : "Request Script Run Error");
        }
        return false;
    }

    public boolean execResponse(Response response, String code, ILog iLog) {
        if (StringUtils.isEmpty(code)) {
            return true;
        }
        try {
            Map<String, Class<?>> result = javac(prependPublicToCoolRequestScript(RESPONSE_REGEX, code), RESPONSE_CLASS);
            if (result.get(RESPONSE_CLASS) != null) {
                return invokeResponse(result.get(RESPONSE_CLASS), response, iLog);
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> Messages.showErrorDialog(e.getMessage(),
                    e instanceof CompilationException ?
                            "Response Script Syntax Error ,Please Check!" : "Request Script Run Error"));
        }
        return false;
    }

    public Map<String, Class<?>> javac(String code, String source) throws Exception {
        ClassResourceUtils.copyTo(getClass().getResource(Constant.CLASSPATH_LIB_PATH), Constant.CONFIG_LIB_PATH.toString());
        InMemoryJavaCompiler javaCompiler = inMemoryJavaCompiler.useParentClassLoader(ScriptPage.class.getClassLoader());
        javaCompiler.useOptions("-encoding", "utf-8", "-cp", PathManager.getJarPathForClass(HTTPRequest.class));
        return javaCompiler.addSource(source, code).compileAll();
    }

    private boolean invokeRequest(Class<?> clas, Request request, ILog iLog) throws ScriptExecException {
        try {
            Object instance = clas.getConstructor().newInstance();
            MethodType methodType = MethodType.methodType(boolean.class, ILog.class, HTTPRequest.class);
            MethodHandle handle = MethodHandles.lookup().findVirtual(clas, "handlerRequest", methodType);
            Object result = handle.bindTo(instance).invokeWithArguments(iLog, request);
            if (result instanceof Boolean) {
                return ((boolean) result);
            }
            return true;
        } catch (Throwable e) {
            LOG.info(e);
            throw new ScriptExecException(e.getMessage());
        }
    }

    private boolean invokeResponse(Class<?> clas, Response response, ILog iLog) throws ScriptExecException {
        try {
            Object instance = clas.getConstructor().newInstance();
            MethodType methodType = MethodType.methodType(void.class, ILog.class, HTTPResponse.class);
            MethodHandle handle = MethodHandles.lookup().findVirtual(clas, "handlerResponse", methodType);
            handle.bindTo(instance).invokeWithArguments(iLog, response);
            return true;
        } catch (Throwable e) {
            LOG.info(e);
            throw new ScriptExecException(e.getMessage());
        }
    }

    public static String prependPublicToCoolRequestScript(String regex, String input) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String replacement = "public " + matcher.group(1);
            return matcher.replaceFirst(replacement);
        } else {
            return input; // 如果未找到匹配项，返回原始字符串
        }
    }
}
