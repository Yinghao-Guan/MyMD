package com.guaguaaaa.mymd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.pandoc.PandocNode;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // --- 步骤 1: 解析 MyMD 文件并生成 Pandoc AST (JSON) ---

        String inputFilePath = "input.txt";
        CharStream input = CharStreams.fromFileName(inputFilePath);

        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();

        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);

        Gson gson = new GsonBuilder().create(); // 使用紧凑格式，无需 pretty printing
        String jsonOutput = gson.toJson(ast);

        System.out.println("成功生成 Pandoc AST JSON。");
        // System.out.println(jsonOutput); // 可以取消注释来调试生成的JSON

        // --- 步骤 2: 调用外部 Pandoc 进程 ---

        System.out.println("正在调用 Pandoc 将 AST 转换为 LaTeX...");
        String outputTexFile = "output.tex";

        try {
            // 1. 构建 Pandoc 命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "pandoc",
                    "-f", "json",      // 输入格式：json
                    "-t", "latex",     // 输出格式：latex
                    "-o", outputTexFile // 输出文件名
            );

            // 2. 启动进程
            Process process = processBuilder.start();

            // 3. 将 JSON 字符串写入 Pandoc 进程的标准输入 (stdin)
            // 必须指定 UTF-8 编码以确保中文字符正确传递
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonOutput);
            }

            // 4. 等待进程执行完成
            int exitCode = process.waitFor();

            // 5. 检查执行结果
            if (exitCode == 0) {
                System.out.println("成功！已生成 " + outputTexFile + " 文件。");
            } else {
                System.err.println("Pandoc 执行失败，退出码: " + exitCode);
                // 读取并打印 Pandoc 的错误信息 (stderr)
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    System.err.println("Pandoc 错误信息:");
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("无法执行 Pandoc 命令。请确保 Pandoc 已经安装并添加到了系统的 PATH 环境变量中。");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Pandoc 进程被中断。");
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}