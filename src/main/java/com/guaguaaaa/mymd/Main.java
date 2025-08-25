package com.guaguaaaa.mymd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.pandoc.PandocNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.guaguaaaa.mymd.MyMDLexer;
import com.guaguaaaa.mymd.MyMDParser;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. 准备输入
        String input = "这是**一段**重要 的文本。\n另一行**粗体**内容。";

        // 2. 创建词法分析器和语法分析器 (不变)
        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();

        // 3. 创建我们的Visitor实例
        PandocAstVisitor visitor = new PandocAstVisitor();

        // 4. 调用visit方法开始遍历，并获取最终的Pandoc AST根节点
        PandocNode ast = visitor.visit(tree);

        // 5. 使用Gson将Java对象转换为JSON字符串
        // prettyPrinting()是为了让输出的JSON格式更易读
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(ast);

        // 6. 打印JSON结果
        System.out.println(jsonOutput);
    }
}