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
                "这是一个包含行内公式的段落, 比如爱因斯坦的质能方程 $E=mc^2$。公式后面还有一些文字。\n\n" +
                        "这是第二个段落，测试一下转义的美元符号 \\$ 和普通的星号 \\*。";

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