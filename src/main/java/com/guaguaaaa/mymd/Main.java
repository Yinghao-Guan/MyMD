package com.guaguaaaa.mymd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.pandoc.PandocNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Update input to test the new block math feature
        String input =
                "这是一个段落。\n\n" +
                        "- 列表项一，*包含斜体*。\n" +
                        "- 列表项二，**包含粗体**。\n" +
                        "- 列表项三，有行内公式 $a^2 + b^2 = c^2$。\n\n" +
                        "列表结束后是另一个段落。";


        // 2. The rest of the code remains the same
        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();

        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(ast);

        System.out.println(jsonOutput);
    }
}