package com.guaguaaaa.mymd.core.parser;

import com.guaguaaaa.mymd.core.ast.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;


/**
 * A visitor that traverses the ANTLR parse tree and constructs a Pandoc Abstract Syntax Tree (AST).
 * This class maps the custom MyMD grammar rules to Pandoc's native JSON format.
 */
public class PandocAstVisitor extends MyMDBaseVisitor<PandocNode> {

    /**
     * Visits the document rule and creates the root Pandoc AST node.
     * @param ctx The parse tree context for the document.
     * @return A {@link PandocAst} node representing the entire document.
     */
    @Override
    public PandocNode visitDocument(MyMDParser.DocumentContext ctx) {
        List<Block> blocks = ctx.block().stream()
                .map(this::visit)
                .map(node -> (Block) node)
                .collect(Collectors.toList());
        return new PandocAst(blocks);
    }

    /**
     * Visits a paragraph block and delegates to the paragraph rule.
     */
    @Override
    public PandocNode visitParagraphBlock(MyMDParser.ParagraphBlockContext ctx) {
        return visit(ctx.paragraph());
    }

    /**
     * Visits a block math rule and delegates to the block math rule.
     */
    @Override
    public PandocNode visitBlockMathRule(MyMDParser.BlockMathRuleContext ctx) {
        return visit(ctx.blockMath());
    }

    /**
     * Visits a bullet list rule and delegates to the bullet list block rule.
     */
    @Override
    public PandocNode visitBulletListRule(MyMDParser.BulletListRuleContext ctx) {
        return visit(ctx.bulletListBlock());
    }

    /**
     * Visits a code block rule and delegates to the code block rule.
     */
    @Override
    public PandocNode visitCodeBlockRule(MyMDParser.CodeBlockRuleContext ctx) {
        return visit(ctx.codeBlock());
    }

    /**
     * Visits a header rule and delegates to the header rule.
     */
    @Override
    public PandocNode visitHeaderRule(MyMDParser.HeaderRuleContext ctx) {
        return visit(ctx.header());
    }

    // --- Block 处理 ---

    @Override
    public PandocNode visitHorizontalRuleBlock(MyMDParser.HorizontalRuleBlockContext ctx) {
        return new HorizontalRule();
    }

    @Override
    public PandocNode visitBlockQuoteBlock(MyMDParser.BlockQuoteBlockContext ctx) {
        List<Inline> inlines = ctx.blockquote().inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());

        Para para = new Para(inlines);
        return new BlockQuote(Collections.singletonList(para));
    }


    /**
     * Visits a header node and creates a Pandoc Header node with the correct level and content.
     * @param ctx The parse tree context for the header.
     * @return A {@link Header} node.
     */
    @Override
    public PandocNode visitHeader(MyMDParser.HeaderContext ctx) {
        int level = 0;
        if (ctx.H1() != null) level = 1;
        else if (ctx.H2() != null) level = 2;
        else if (ctx.H3() != null) level = 3;
        else if (ctx.H4() != null) level = 4;
        else if (ctx.H5() != null) level = 5;
        else if (ctx.H6() != null) level = 6;

        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());

        return new Header(level, inlines);
    }

    /**
     * Visits a block math node and creates a Pandoc Para node containing a Math node.
     * @param ctx The parse tree context for the block math.
     * @return A {@link Para} node with the display math content.
     */
    @Override
    public PandocNode visitBlockMath(MyMDParser.BlockMathContext ctx) {
        String fullText = ctx.BLOCK_MATH().getText();
        String mathText = fullText.substring(2, fullText.length() - 2).trim();
        MathNode mathNode = new MathNode(MathNode.MathType.DISPLAY_MATH, mathText);
        return new Para(Collections.singletonList(mathNode));
    }

    /**
     * Visits a code block node and creates a Pandoc CodeBlock node.
     * @param ctx The parse tree context for the code block.
     * @return A {@link CodeBlock} node.
     */
    @Override
    public PandocNode visitCodeBlock(MyMDParser.CodeBlockContext ctx) {
        String fullText = ctx.CODE_BLOCK().getText();
        String codeText = fullText.substring(3, fullText.length() - 3).trim();
        return new CodeBlock(codeText);
    }

    /**
     * Visits a bullet list block and delegates to the bullet list rule.
     */
    @Override
    public PandocNode visitBulletListBlock(MyMDParser.BulletListBlockContext ctx) {
        return visit(ctx.bulletList());
    }

    /**
     * Visits a bullet list node and creates a Pandoc BulletList node.
     * @param ctx The parse tree context for the bullet list.
     * @return A {@link BulletList} node.
     */
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

    /**
     * Visits a paragraph node and creates a Pandoc Para node.
     * @param ctx The parse tree context for the paragraph.
     * @return A {@link Para} node.
     */
    @Override
    public PandocNode visitParagraph(MyMDParser.ParagraphContext ctx) {
        List<Inline> inlines = ctx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());
        return new Para(inlines);
    }

    // --- Inline Elements ---
    @Override
    public PandocNode visitImageInline(MyMDParser.ImageInlineContext ctx) {
        MyMDParser.ImageContext imgCtx = ctx.image();
        String url = imgCtx.url().getText();

        List<Inline> altText = imgCtx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());

        return new Image(altText, url);
    }

    @Override
    public PandocNode visitLinkInline(MyMDParser.LinkInlineContext ctx) {
        MyMDParser.LinkContext linkCtx = ctx.link();
        String url = linkCtx.url().getText();

        List<Inline> content = linkCtx.inline().stream()
                .map(this::visit)
                .map(node -> (Inline) node)
                .collect(Collectors.toList());

        return new Link(content, url);
    }

    @Override
    public PandocNode visitBangInline(MyMDParser.BangInlineContext ctx) {
        return new Str("!");
    }

    @Override
    public PandocNode visitGtInline(MyMDParser.GtInlineContext ctx) {
        return new Str(">");
    }

    @Override
    public PandocNode visitLParenInline(MyMDParser.LParenInlineContext ctx) {
        return new Str("(");
    }

    @Override
    public PandocNode visitRParenInline(MyMDParser.RParenInlineContext ctx) {
        return new Str(")");
    }

    @Override
    public PandocNode visitUrlTextInline(MyMDParser.UrlTextInlineContext ctx) {
        // 这就是那个关键修复：把被误认为 URL 的单词当做普通文本返回
        return new Str(ctx.getText());
    }

    @Override
    public PandocNode visitDashInline(MyMDParser.DashInlineContext ctx) {
        return new Str("-");
    }

    @Override
    public PandocNode visitStarInline(MyMDParser.StarInlineContext ctx) {
        return new Str("*");
    }

    /**
     * Visits an inline code element and creates a Pandoc Code node.
     * @param ctx The parse tree context for the inline code.
     * @return A {@link Code} node.
     */
    @Override
    public PandocNode visitInlineCodeInline(MyMDParser.InlineCodeInlineContext ctx) {
        String fullText = ctx.INLINE_CODE().getText();
        String codeText = fullText.substring(1, fullText.length() - 1);
        return new Code(codeText);
    }

    /**
     * Visits a bold inline element and delegates to the bold rule.
     */
    @Override
    public PandocNode visitBoldInline(MyMDParser.BoldInlineContext ctx) { return visit(ctx.bold()); }

    /**
     * Visits an italic inline element and delegates to the italic rule.
     */
    @Override
    public PandocNode visitItalicInline(MyMDParser.ItalicInlineContext ctx) { return visit(ctx.italic()); }

    /**
     * Visits an escaped inline element and creates a Pandoc Str node, removing the escape character.
     * @param ctx The parse tree context for the escaped character.
     * @return A {@link Str} node.
     */
    @Override
    public PandocNode visitEscapedInline(MyMDParser.EscapedInlineContext ctx) { return new Str(ctx.ESCAPED().getText().substring(1)); }

    /**
     * Visits a text inline element and creates a Pandoc Str node.
     * @param ctx The parse tree context for the text.
     * @return A {@link Str} node.
     */
    @Override
    public PandocNode visitTextInline(MyMDParser.TextInlineContext ctx) { return new Str(ctx.TEXT().getText()); }

    /**
     * Visits a space inline element and creates a Pandoc Space node.
     * @param ctx The parse tree context for the space.
     * @return A {@link Space} node.
     */
    @Override
    public PandocNode visitSpaceInline(MyMDParser.SpaceInlineContext ctx) { return new Space(); }

    /**
     * Visits a soft break inline element and creates a Pandoc Space node.
     * @param ctx The parse tree context for the soft break.
     * @return A {@link Space} node.
     */
    @Override
    public PandocNode visitSoftBreakInline(MyMDParser.SoftBreakInlineContext ctx) { return new Space(); }

    /**
     * Visits a hard break inline element and creates a Pandoc LineBreak node.
     * @param ctx The parse tree context for the hard break.
     * @return A {@link LineBreak} node.
     */
    @Override
    public PandocNode visitHardBreakInline(MyMDParser.HardBreakInlineContext ctx) { return new LineBreak(); }

    /**
     * Visits an inline math element and creates a Pandoc Math node.
     * @param ctx The parse tree context for the inline math.
     * @return A {@link MathNode} node.
     */
    @Override
    public PandocNode visitInlineMathInline(MyMDParser.InlineMathInlineContext ctx) {
        String fullText = ctx.INLINE_MATH().getText();
        String mathText = fullText.substring(1, fullText.length() - 1);
        return new MathNode(MathNode.MathType.INLINE_MATH, mathText);
    }

    @Override
    public PandocNode visitCitationInline(MyMDParser.CitationInlineContext ctx) {
        String fullText = ctx.citation().getText();
        // 去掉开头的 "[@" 和结尾的 "]"，提取 ID
        String citeId = fullText.substring(2, fullText.length() - 1);
        return new Cite(citeId);
    }

    /**
     * Visits a bold node and creates a Pandoc Strong node.
     * @param ctx The parse tree context for the bold text.
     * @return A {@link Strong} node.
     */
    @Override
    public PandocNode visitBold(MyMDParser.BoldContext ctx) {
        List<Inline> inlines = ctx.inline().stream().map(this::visit).map(node -> (Inline) node).collect(Collectors.toList());
        return new Strong(inlines);
    }

    /**
     * Visits an italic node and creates a Pandoc Emph node.
     * @param ctx The parse tree context for the italic text.
     * @return An {@link Emph} node.
     */
    @Override
    public PandocNode visitItalic(MyMDParser.ItalicContext ctx) {
        List<Inline> inlines = ctx.inline().stream().map(this::visit).map(node -> (Inline) node).collect(Collectors.toList());
        return new Emph(inlines);
    }

    /**
     * 处理普通的左方括号
     */
    @Override
    public PandocNode visitLBracketInline(MyMDParser.LBracketInlineContext ctx) {
        return new Str("[");
    }

    /**
     * 处理普通的右方括号
     */
    @Override
    public PandocNode visitRBracketInline(MyMDParser.RBracketInlineContext ctx) {
        return new Str("]");
    }
}