package com.guaguaaaa.mymd;

import com.guaguaaaa.mymd.pandoc.*;
import com.guaguaaaa.mymd.MyMDBaseVisitor;
import com.guaguaaaa.mymd.MyMDParser;

import java.util.List;
import java.util.stream.Collectors;

// 这次 MyMDBaseVisitor 应该可以被正确找到了
public class PandocAstVisitor extends MyMDBaseVisitor<PandocNode> {

    @Override
    public PandocNode visitDocument(MyMDParser.DocumentContext ctx) {
        List<Block> blocks = ctx.block().stream()
                .map(this::visit)
                .map(node -> (Block) node)
                .collect(Collectors.toList());
        return new PandocAst(blocks);
    }

    @Override
    public PandocNode visitBlock(MyMDParser.BlockContext ctx) {
        // 一个 block 节点的核心内容就是它的 paragraph 子节点。
        // 我们直接访问 paragraph 子节点并返回它的结果。
        return visit(ctx.paragraph());
    }

    @Override
    public PandocNode visitParagraph(MyMDParser.ParagraphContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit) // 这里会根据标签调用下面的 visitBoldInline, visitTextInline等
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Para(inlines);
    }

    // --- 使用标签生成的新方法 ---

    @Override
    public PandocNode visitBoldInline(MyMDParser.BoldInlineContext ctx) {
        // 直接访问bold规则节点
        return visit(ctx.bold());
    }

    @Override
    public PandocNode visitEscapedInline(MyMDParser.EscapedInlineContext ctx) {
        // 处理转义字符，去掉前面的反斜杠
        String text = ctx.ESCAPED().getText();
        return new Str(text.substring(1));
    }

    @Override
    public PandocNode visitTextInline(MyMDParser.TextInlineContext ctx) {
        // 处理普通文本
        return new Str(ctx.TEXT().getText());
    }

    @Override
    public PandocNode visitSpaceInline(MyMDParser.SpaceInlineContext ctx) {
        // 在Pandoc中，多个空格通常被表示为一个Space节点
        return new Space();
    }

    @Override
    public PandocNode visitSoftBreakInline(MyMDParser.SoftBreakInlineContext ctx) {
        // 1. 实现第一个目标：一个换行符等于一个空格
        return new Space();
    }

    @Override
    public PandocNode visitHardBreakInline(MyMDParser.HardBreakInlineContext ctx) {
        // 3. 实现第三个目标：\\ 强制换行
        // 你需要先创建一个 LineBreak.java 类，它对应 Pandoc AST 的 LineBreak 节点
        // LineBreak.java 应该和 Space.java 类似：
        // public class LineBreak extends Inline {
        //     public LineBreak() { super("LineBreak", null); }
        // }
        return new LineBreak();
    }

    // --- 处理具体规则的方法 ---

    @Override
    public PandocNode visitBold(MyMDParser.BoldContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Strong(inlines);
    }
}