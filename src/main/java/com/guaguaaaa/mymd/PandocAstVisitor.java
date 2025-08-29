package com.guaguaaaa.mymd;

import com.guaguaaaa.mymd.pandoc.*;
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

    // --- visitParagraphBlock 保持不变 ---
    @Override
    public PandocNode visitParagraphBlock(MyMDParser.ParagraphBlockContext ctx) {
        return visit(ctx.paragraph());
    }

    // --- 方法名和上下文类型已根据新的 G4 标签更新 ---
    @Override
    public PandocNode visitBlockMathRule(MyMDParser.BlockMathRuleContext ctx) {
        // 访问子规则 blockMath
        return visit(ctx.blockMath());
    }

    // --- 方法名和上下文类型已根据新的 G4 标签更新 ---
    @Override
    public PandocNode visitBulletListRule(MyMDParser.BulletListRuleContext ctx) {
        // 访问子规则 bulletListBlock
        return visit(ctx.bulletListBlock());
    }

    // --- 方法名和上下文类型已根据新的 G4 标签更新 ---
    @Override
    public PandocNode visitCodeBlockRule(MyMDParser.CodeBlockRuleContext ctx) {
        // 访问子规则 codeBlock
        return visit(ctx.codeBlock());
    }


    // --- 以下是处理具体块级元素逻辑的方法 ---

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