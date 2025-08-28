package com.guaguaaaa.mymd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.pandoc.PandocNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. 准备一个能体现所有逻辑的输入
        String input =
                "这是第一个段落的第一行。\n" + // 这是一个 SOFT_BREAK，应该被转换成空格
                        "这是第一个段落的第二行。\n\n" + // 这是一个 PARAGRAPH_END，应该结束第一个段落
                        "现在是第二个段落，*包含斜体*。\n\n\n" + // 多个空行也算一个 PARAGRAPH_END
                        "这是第三个段落，**包含粗体**。"; // 最后没有换行符，应该由 EOF 来结束段落

        // 2. 创建词法分析器和语法分析器
        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();

        // 3. 创建我们的Visitor实例
        PandocAstVisitor visitor = new PandocAstVisitor();

        // 4. 调用visit方法开始遍历
        PandocNode ast = visitor.visit(tree);

        // 5. 使用Gson将Java对象转换为JSON字符串
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(ast);

        // 6. 打印JSON结果
        System.out.println(jsonOutput);
    }
}