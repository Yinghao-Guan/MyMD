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

    @Override
    public PandocNode visitParagraphBlock(MyMDParser.ParagraphBlockContext ctx) {
        return visit(ctx.paragraph());
    }

    @Override
    public PandocNode visitBlockMathBlock(MyMDParser.BlockMathBlockContext ctx) {
        String fullText = ctx.BLOCK_MATH().getText();
        String mathText = fullText.substring(2, fullText.length() - 2).trim();
        MathNode mathNode = new MathNode(MathNode.MathType.DISPLAY_MATH, mathText);
        return new Para(Collections.singletonList(mathNode));
    }

    // --- 新增：处理整个列表块的方法 ---
    @Override
    public PandocNode visitBulletListBlock(MyMDParser.BulletListBlockContext ctx) {
        return visit(ctx.bulletList());
    }

    // --- 修改：将 listItem 的处理逻辑直接整合到这里 ---
    @Override
    public PandocNode visitBulletList(MyMDParser.BulletListContext ctx) {
        List<List<Block>> items = ctx.listItem().stream()
                .map(listItemCtx -> { // 直接处理每个 listItem 上下文
                    // 1. 获取一个列表项中的所有行内元素
                    List<Inline> inlines = listItemCtx.inline().stream()
                            .map(this::visit)
                            .map(node -> (Inline) node)
                            .collect(Collectors.toList());
                    // 2. 将行内元素包装成一个段落 (Para)
                    Para para = new Para(inlines);
                    // 3. 每个列表项是一个 Block 列表，这里只包含一个 Para
                    return Collections.singletonList((Block) para);
                })
                .collect(Collectors.toList());

        return new BulletList(items);
    }

    // --- 已移除 visitListItem 方法 ---

    @Override
    public PandocNode visitParagraph(MyMDParser.ParagraphContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Para(inlines);
    }

    // --- 以下是所有处理 Inline 元素的方法，保持不变 ---

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