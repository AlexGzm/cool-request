package com.cool.request.component.api.scans;

import com.cool.request.common.bean.components.controller.Controller;
import com.cool.request.common.bean.components.controller.StaticController;
import com.cool.request.common.service.ControllerMapService;
import com.cool.request.component.http.net.HttpMethod;
import com.cool.request.lib.springmvc.ControllerAnnotation;
import com.cool.request.lib.springmvc.config.reader.UserProjectContextPathReader;
import com.cool.request.lib.springmvc.config.reader.UserProjectServerPortReader;
import com.cool.request.lib.springmvc.utils.ParamUtils;
import com.cool.request.utils.PsiUtils;
import com.cool.request.utils.StringUtils;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SpringMvcControllerScan {

    public List<Controller> scan(Project project) {
        List<Controller> result = new ArrayList<>();
        ModuleManager moduleManager = ModuleManager.getInstance(project);

        for (Module module : moduleManager.getModules()) {
            UserProjectServerPortReader userProjectServerPortReader = new UserProjectServerPortReader(project, module);
            UserProjectContextPathReader userProjectContextPathReader = new UserProjectContextPathReader(project, module);

            Integer currentModuleServerPort = userProjectServerPortReader.read();
            String contextPath = userProjectContextPathReader.read();
            scanByAnnotation(project, module, result, currentModuleServerPort, contextPath, ControllerAnnotation.Controller);
            scanByAnnotation(project, module, result, currentModuleServerPort, contextPath, ControllerAnnotation.RestController);

        }
        return result;
    }

    private void scanByAnnotation(Project project,
                                  Module module,
                                  List<Controller> result,
                                  Integer currentModuleServerPort,
                                  String contextPath, ControllerAnnotation controllerAnnotation) {
        Collection<PsiAnnotation> psiAnnotations = JavaAnnotationIndex.getInstance().get(controllerAnnotation.getName(), project,
                GlobalSearchScope.moduleScope(module));
        for (PsiAnnotation psiAnnotation : psiAnnotations) {
            PsiElement psiAnnotationParent = psiAnnotation.getParent();
            if (!controllerAnnotation.getAnnotationName().equalsIgnoreCase(psiAnnotation.getQualifiedName()))
                continue;
            if (psiAnnotationParent == null) continue;
            if (!(psiAnnotationParent instanceof PsiModifierList)) continue;
            PsiElement psiElement = psiAnnotationParent.getParent();
            if (!(psiElement instanceof PsiClass)) {
                continue;
            }
            if (!PsiUtils.isAbstractClass(((PsiClass) psiElement))) {
                result.addAll(extractHttpRouteMethods((PsiClass) psiElement, module, currentModuleServerPort, contextPath));
            }

        }
    }

    private List<StaticController> extractHttpRouteMethods(PsiClass originClass,
                                                           Module module,
                                                           Integer currentModuleServerPort,
                                                           String contextPath) {
        if (PsiUtils.isObjectClass(originClass)) return new ArrayList<>();

        List<StaticController> result = new ArrayList<>();
        for (PsiMethod psiMethod : originClass.getAllMethods()) {

            List<HttpMethod> httpMethod = PsiUtils.getHttpMethod(psiMethod);
            if (httpMethod.isEmpty()) continue;
            List<String> httpUrl = ParamUtils.getHttpUrl(originClass, psiMethod);
            if (httpUrl == null) continue;
            PsiClass superClassName = PsiUtils.getSuperClassName(psiMethod);
            for (String url : httpUrl) {
                // TODO: 2024/1/10 //这里有问题，先获取第一个
                StaticController controller = (StaticController) Controller.ControllerBuilder.aController()
                        .withHttpMethod(httpMethod.get(0).toString())
                        .withMethodName(psiMethod.getName())
                        .withContextPath(contextPath)
                        .withServerPort(currentModuleServerPort)
                        .withModuleName(module.getName())
                        .withUrl(StringUtils.addPrefixIfMiss(url, "/"))
                        .withSimpleClassName(originClass.getQualifiedName())
                        .withParamClassList(PsiUtils.getParamClassList(psiMethod))
                        .build(new StaticController(), module.getProject());
                controller.setSuperPsiClass(superClassName);
                result.add(controller);
                ControllerMapService.getInstance(psiMethod.getProject()).addMap(controller, psiMethod);
            }

        }
        return result;
    }

}
