package com.guaguaaaa.mymd;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// 注意：这里的 MyMDLexer 和 MyMDParser 是 ANTLR 根据 MyMD.g4 文件生成的，
// 需要先运行 Maven 构建才能引入。
import com.guaguaaaa.mymd.MyMDLexer;
import com.guaguaaaa.mymd.MyMDParser;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. 创建一个输入流，这里用一个简单的字符串作为例子
        String input = "这是**一段**重要的文本。\n这是第二行。";
        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(input));

        // 2. 创建一个词法符号流
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 3. 创建语法分析器
        MyMDParser parser = new MyMDParser(tokens);

        // 4. 开始解析，得到解析树
        ParseTree tree = parser.document();

        // 5. 打印解析树的结构（用于调试）
        System.out.println(tree.toStringTree(parser));

        // 接下来你可以创建一个 Visitor 或 Listener 来遍历这棵树，并进行实际的操作。
    }
}