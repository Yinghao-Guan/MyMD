package com.guaguaaaa.mymd;

import com.guaguaaaa.mymd.pandoc.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public PandocNode visitParagraphBlock(MyMDParser.ParagraphBlockContext ctx) {
        return visit(ctx.paragraph());
    }

    @Override
    public PandocNode visitBlockMathRule(MyMDParser.BlockMathRuleContext ctx) {
        return visit(ctx.blockMath());
    }

    @Override
    public PandocNode visitBulletListRule(MyMDParser.BulletListRuleContext ctx) {
        return visit(ctx.bulletListBlock());
    }

    @Override
    public PandocNode visitCodeBlockRule(MyMDParser.CodeBlockRuleContext ctx) {
        return visit(ctx.codeBlock());
    }

    // --- 新增：处理标题规则的入口方法 ---
    @Override
    public PandocNode visitHeaderRule(MyMDParser.HeaderRuleContext ctx) {
        return visit(ctx.header());
    }

    // --- 新增：处理标题逻辑的核心方法 ---
    @Override
    public PandocNode visitHeader(MyMDParser.HeaderContext ctx) {
        // 1. 确定标题级别
        int level = 0;
        if (ctx.H1() != null) level = 1;
        else if (ctx.H2() != null) level = 2;
        else if (ctx.H3() != null) level = 3;
        else if (ctx.H4() != null) level = 4;
        else if (ctx.H5() != null) level = 5;
        else if (ctx.H6() != null) level = 6;

        // 2. 提取标题内容 (所有的行内元素)
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());

        // 3. 创建并返回 Header 节点
        return new Header(level, inlines);
    }


    @Override
    public PandocNode visitBlockMath(MyMDParser.BlockMathContext ctx) {
        String fullText = ctx.BLOCK_MATH().getText();
        String mathText = fullText.substring(2, fullText.length() - 2).trim();
        MathNode mathNode = new MathNode(MathNode.MathType.DISPLAY_MATH, mathText);
        return new Para(Collections.singletonList(mathNode));
    }

    @Override
    public PandocNode visitCodeBlock(MyMDParser.CodeBlockContext ctx) {
        String fullText = ctx.CODE_BLOCK().getText();
        String codeText = fullText.substring(3, fullText.length() - 3).trim();
        CodeNode codeNode = new CodeNode(CodeNode.CodeType.BLOCK, codeText);
        return new Para(Collections.singletonList(codeNode));
    }

    @Override
    public PandocNode visitBulletListBlock(MyMDParser.BulletListBlockContext ctx) {
        return visit(ctx.bulletList());
    }

    @Override
    public PandocNode visitBulletList(MyMDParser.BulletListContext ctx) {
        List<List<Block>> items = ctx.listItem().stream()
                .map(listItemCtx -> {
                    List<Inline> inlines = listItemCtx.inline().stream()
                            .map(this::visit)
                            .map(node -> (Inline) node)
                            .collect(Collectors.toList());
                    Para para = new Para(inlines);
                    return Collections.singletonList((Block) para);
                })
                .collect(Collectors.toList());

        return new BulletList(items);
    }

    @Override
    public PandocNode visitParagraph(MyMDParser.ParagraphContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Para(inlines);
    }

    // --- 以下所有处理 Inline 元素的方法保持不变 ---

    @Override
    public PandocNode visitBoldInline(MyMDParser.BoldInlineContext ctx) {
        return visit(ctx.bold());
    }

    @Override
    public PandocNode visitItalicInline(MyMDParser.ItalicInlineContext ctx) {
        return visit(ctx.italic());
    }

    @Override
    public PandocNode visitInlineMathInline(MyMDParser.InlineMathInlineContext ctx) {
        String fullText = ctx.INLINE_MATH().getText();
        String mathText = fullText.substring(1, fullText.length() - 1);
        return new MathNode(MathNode.MathType.INLINE_MATH, mathText);
    }

    @Override
    public PandocNode visitInlineCodeInline(MyMDParser.InlineCodeInlineContext ctx) {
        String fullText = ctx.INLINE_CODE().getText();
        String codeText = fullText.substring(1, fullText.length() - 1);
        return new CodeNode(CodeNode.CodeType.INLINE, codeText);
    }

    @Override
    public PandocNode visitEscapedInline(MyMDParser.EscapedInlineContext ctx) {
        String text = ctx.ESCAPED().getText();
        return new Str(text.substring(1));
    }

    @Override
    public PandocNode visitTextInline(MyMDParser.TextInlineContext ctx) {
        return new Str(ctx.TEXT().getText());
    }

    @Override
    public PandocNode visitSpaceInline(MyMDParser.SpaceInlineContext ctx) {
        return new Space();
    }

    @Override
    public PandocNode visitSoftBreakInline(MyMDParser.SoftBreakInlineContext ctx) {
        return new Space();
    }

    @Override
    public PandocNode visitHardBreakInline(MyMDParser.HardBreakInlineContext ctx) {
        return new LineBreak();
    }

    @Override
    public PandocNode visitBold(MyMDParser.BoldContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Strong(inlines);
    }

    @Override
    public PandocNode visitItalic(MyMDParser.ItalicContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Emph(inlines);
    }
}