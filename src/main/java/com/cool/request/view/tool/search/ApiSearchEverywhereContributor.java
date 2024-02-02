package com.cool.request.view.tool.search;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ApiSearchEverywhereContributor  implements SearchEverywhereContributorFactory {
    @Override
    public @NotNull SearchEverywhereContributor createContributor(@NotNull AnActionEvent initEvent) {
        return new ApiAbstractGotoSEContributor(initEvent);
    }


}