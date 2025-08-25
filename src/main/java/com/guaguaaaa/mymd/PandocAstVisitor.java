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
        // return new Space();  // <-- 你需要先创建Space.java
        // 为了简单起见，我们暂时先把它也当作普通文本处理
        return new Str(" ");
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