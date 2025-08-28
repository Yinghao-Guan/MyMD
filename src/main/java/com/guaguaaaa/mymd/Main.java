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
                "This is a paragraph with an inline equation $E=mc^2$.\n\n" +
                        "$$\n" +
                        "E = mc^2\n" +
                        "$$\n\n" +
                        "And this is a paragraph after the math block.";


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