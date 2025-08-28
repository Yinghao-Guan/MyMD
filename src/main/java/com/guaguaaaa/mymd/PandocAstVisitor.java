package com.guaguaaaa.mymd;

import com.guaguaaaa.mymd.pandoc.*;
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
    public PandocNode visitParagraph(MyMDParser.ParagraphContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Para(inlines);
    }

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
        // 1. Get the complete token text (e.g., "$E=mc^2$")
        String fullText = ctx.INLINE_MATH().getText();

        // 2. Remove the first and last characters ('$') to get the content
        String mathText = fullText.substring(1, fullText.length() - 1);

        // 3. Create the MathNode
        return new MathNode(mathText);
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
        // 单个换行符被视为一个空格
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