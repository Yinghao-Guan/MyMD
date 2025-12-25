package com.guaguaaaa.mymd.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.core.api.CompilationResult;
import com.guaguaaaa.mymd.core.ast.PandocNode;
import com.guaguaaaa.mymd.core.parser.MyMDLexer;
import com.guaguaaaa.mymd.core.parser.MyMDParser;
import com.guaguaaaa.mymd.core.parser.PandocAstVisitor;
import com.guaguaaaa.mymd.core.parser.SyntaxErrorCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Collections;

public class MyMDCompiler {

    private static final Gson gson = new GsonBuilder().create();

    public static CompilationResult compile(String source) {
        // 设置错误收集
        SyntaxErrorCollector errorListener = new SyntaxErrorCollector();

        // 初始化 Lexer
        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        // 初始化 Parser
        MyMDParser parser = new MyMDParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        // 解析
        ParseTree tree = parser.document();

        // 检查语法错误
        if (errorListener.hasErrors()) {
            return new CompilationResult(null, null, errorListener.getErrors());
        }

        // 转换为 AST
        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);

        // 序列化为 JSON
        String json = gson.toJson(ast);

        return new CompilationResult(ast, json, Collections.emptyList());
    }
}