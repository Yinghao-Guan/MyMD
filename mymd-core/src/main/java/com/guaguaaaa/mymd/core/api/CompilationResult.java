package com.guaguaaaa.mymd.core.api;

import com.guaguaaaa.mymd.core.ast.PandocNode;
import java.util.List;

public class CompilationResult {
    public final PandocNode rootAst;
    public final String pandocJson;
    public final List<Diagnostic> diagnostics;

    public CompilationResult(PandocNode rootAst, String pandocJson, List<Diagnostic> diagnostics) {
        this.rootAst = rootAst;
        this.pandocJson = pandocJson;
        this.diagnostics = diagnostics;
    }

    public boolean hasErrors() {
        return diagnostics != null && !diagnostics.isEmpty();
    }
}