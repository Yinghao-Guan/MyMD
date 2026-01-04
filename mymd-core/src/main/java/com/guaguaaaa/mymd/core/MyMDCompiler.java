package com.guaguaaaa.mymd.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.core.api.CompilationResult;
import com.guaguaaaa.mymd.core.api.Diagnostic;
import com.guaguaaaa.mymd.core.ast.PandocNode;
import com.guaguaaaa.mymd.core.parser.MyMDLexer;
import com.guaguaaaa.mymd.core.parser.MyMDParser;
import com.guaguaaaa.mymd.core.parser.PandocAstVisitor;
import com.guaguaaaa.mymd.core.parser.SyntaxErrorCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Collections;
import java.util.List;

public class MyMDCompiler {

    private static final Gson gson = new GsonBuilder().create();

    public static CompilationResult compile(String source) {
        SyntaxErrorCollector errorListener = new SyntaxErrorCollector();

        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        MyMDParser parser = new MyMDParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ParseTree tree = parser.doc();

        if (errorListener.hasErrors()) {
            return new CompilationResult(null, null, errorListener.getErrors());
        }

        try {
            PandocAstVisitor visitor = new PandocAstVisitor();
            visitor.visit(tree);
            String json = visitor.getPandocJson();
            return new CompilationResult(null, json, Collections.emptyList());

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();

            Diagnostic error = new Diagnostic(0, 0, 0, 0, "Compiler Error: " + msg);
            return new CompilationResult(null, null, Collections.singletonList(error));
        }
    }
}