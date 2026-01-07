package com.guaguaaaa.mymd.core.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.guaguaaaa.mymd.core.ast.*;
import com.guaguaaaa.mymd.core.util.MetadataConverter;
import com.guaguaaaa.mymd.core.ast.Cite;
import com.guaguaaaa.mymd.core.util.ListMarker;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A visitor that traverses the ANTLR parse tree and constructs a Pandoc Abstract Syntax Tree (AST).
 * This class maps the custom MyMD grammar rules to Pandoc's native JSON format.
 */
public class PandocAstVisitor extends MyMDParserBaseVisitor<PandocNode> {

    // 保存解析出来的 Metadata
    private JsonObject metadata = new JsonObject();
    // 保存正文块
    private List<Block> blocks = new ArrayList<>();

    private final Gson gson = new GsonBuilder().create();

    /**
     * 获取最终的 Pandoc JSON 字符串
     */
    public String getPandocJson() {
        JsonObject root = new JsonObject();

        JsonArray apiVersion = new JsonArray();
        apiVersion.add(1);
        apiVersion.add(23);
        root.add("pandoc-api-version", apiVersion);

        root.add("meta", this.metadata != null ? this.metadata : new JsonObject());

        root.add("blocks", gson.toJsonTree(this.blocks));

        return gson.toJson(root);
    }

    /**
     * 对应 MyMDParser.g4 中的 doc 规则
     */
    @Override
    public PandocNode visitDoc(MyMDParser.DocContext ctx) {
        if (ctx.yaml_block() != null) {
            visit(ctx.yaml_block());
        }

        this.blocks = ctx.block().stream()
                .map(this::visit)
                .map(node -> (Block) node)
                .collect(Collectors.toList());

        return null;
    }

    /**
     * 处理 YAML 块
     */
    @Override
    public PandocNode visitYaml_block(MyMDParser.Yaml_blockContext ctx) {
        String rawYaml = ctx.getText();
        JsonObject parsedMeta = MetadataConverter.parseYamlToPandocMeta(rawYaml);

        // 合并 Metadata
        if (parsedMeta != null) {
            for (String key : parsedMeta.keySet()) {
                this.metadata.add(key, parsedMeta.get(key));
            }
        }
        return null;
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

    @Override
    public PandocNode visitLatexEnv(MyMDParser.LatexEnvContext ctx) {
        return new RawBlock("latex", ctx.LATEX_ENV_BLOCK().getText());
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

        if (!inlines.isEmpty()) {
            Inline lastNode = inlines.get(inlines.size() - 1);

            if (lastNode instanceof RawInline) {
                RawInline raw = (RawInline) lastNode;
                String content = raw.getContent();
                if ("latex".equals(raw.getFormat()) && content != null && content.startsWith("\\ref{")) {
                    String id = content.substring(5, content.length() - 1);

                    inlines.remove(inlines.size() - 1);

                    inlines.add(new RawInline("latex", "\\label{" + id + "}"));
                }
            }
        }

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

        if (ctx.REF_ID() != null) {
            String labelRaw = ctx.REF_ID().getText();
            String labelId = labelRaw.substring(1, labelRaw.length() - 1);
            mathText += " \\label{" + labelId + "}";
        }

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

        String inner = fullText.substring(3, fullText.length() - 3);

        String language = "";
        String codeContent = inner;

        int firstNewLineIndex = inner.indexOf('\n');
        if (firstNewLineIndex > 0) {
            language = inner.substring(0, firstNewLineIndex).trim();
            codeContent = inner.substring(firstNewLineIndex + 1);
        } else if (!inner.isBlank()) {

        }

        return new CodeBlock(codeContent, language);
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
                .map(itemCtx -> {
                    // 收集行内子节点 (排除 DASH 和 SPACE)
                    // Parser 规则: DASH SPACE inlineCommon+ ...
                    // itemCtx.children 包含所有 token
                    List<ParseTree> inlineNodes = new ArrayList<>();
                    for (ParseTree child : itemCtx.children) {
                        if (child instanceof MyMDParser.InlineCommonContext) {
                            inlineNodes.add(child);
                        }
                    }
                    return processListItem(inlineNodes, itemCtx.nestedBody());
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

        List<Inline> inlines = new ArrayList<>();
        for (ParseTree child : ctx.children) {
            if (child instanceof TerminalNode tn) {
                if (tn.getSymbol().getType() == MyMDLexer.SOFT_BREAK) {
                    inlines.add(new Space());
                }
                continue;
            }
            PandocNode node = visit(child);
            if (node instanceof Inline inline) {
                inlines.add(inline);
            }
        }
        return new Para(inlines);
    }

    // --- Inline Elements ---
    @Override
    public PandocNode visitEscapedExceptionInline(MyMDParser.EscapedExceptionInlineContext ctx) {
        MyMDParser.EscapeExceptionContext escCtx = ctx.escapeException();
        if (escCtx.ESCAPED_NEWLINE() != null) return new LineBreak();

        String text = escCtx.getText();
        // Remove the leading backslash
        // \\ -> \
        // \* -> *
        // \- -> -
        // \[ -> [
        // \] -> ]
        // \` -> `
        return new Str(text.substring(1));
    }

    @Override
    public PandocNode visitRawLatexInline(MyMDParser.RawLatexInlineContext ctx) {
        return new RawInline("latex", ctx.getText());
    }

    @Override
    public PandocNode visitImageInline(MyMDParser.ImageInlineContext ctx) {
        MyMDParser.ImageContext imgCtx = ctx.image();
        String url = imgCtx.url().getText();
        List<Inline> altText;

        if (imgCtx.REF_ID() != null) {
            String text = imgCtx.REF_ID().getText();
            String linkText = text.substring(1, text.length() - 1);
            altText = Collections.singletonList(new Str(linkText));
        } else {
            altText = imgCtx.inline().stream()
                    .map(this::visit)
                    .map(node -> (Inline) node)
                    .collect(Collectors.toList());
        }

        return new Image(altText, url);
    }

    @Override
    public PandocNode visitLinkInline(MyMDParser.LinkInlineContext ctx) {
        MyMDParser.LinkContext linkCtx = ctx.link();
        String url = linkCtx.url().getText();
        List<Inline> content;

        if (linkCtx.REF_ID() != null) {
            String text = linkCtx.REF_ID().getText();
            String linkText = text.substring(1, text.length() - 1);
            content = Collections.singletonList(new Str(linkText));
        } else {
            content = linkCtx.inline().stream()
                    .map(this::visit)
                    .map(node -> (Inline) node)
                    .collect(Collectors.toList());
        }

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
    public PandocNode visitTextInline(MyMDParser.TextInlineContext ctx) {
        String text;
        if (ctx.TEXT() != null) {
            text = ctx.TEXT().getText();
        } else if (ctx.ORDERED_LIST_ITEM() != null) {
            text = ctx.ORDERED_LIST_ITEM().getText();
        } else if (ctx.PLUS_ITEM() != null) {
            text = ctx.PLUS_ITEM().getText();
        } else {
            text = "";
        }
        return new Str(text);
    }

    /**
     * Visits a space inline element and creates a Pandoc Space node.
     * @param ctx The parse tree context for the space.
     * @return A {@link Space} node.
     */
    @Override
    public PandocNode visitSpaceInline(MyMDParser.SpaceInlineContext ctx) { return new Space(); }

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
        String citeId = fullText.substring(2, fullText.length() - 1);
        return new Cite(citeId);
    }

    @Override
    public PandocNode visitCitation(MyMDParser.CitationContext ctx) {
        String text = ctx.getText();
        String citeId = text.substring(2, text.length() - 1);
        return new Cite(citeId);
    }

    /**
     * 处理引用/标签语法 [type:id]
     * 默认行为：生成 LaTeX 的 \ref{type:id}
     * (如果在标题末尾，会在 visitHeader 中被转换为 \label)
     */
    @Override
    public PandocNode visitRef(MyMDParser.RefContext ctx) {
        String text = ctx.getText();
        String labelId = text.substring(1, text.length() - 1);
        return new RawInline("latex", "\\ref{" + labelId + "}");
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

    @Override
    public PandocNode visitOrderedListBlock(MyMDParser.OrderedListBlockContext ctx) {
        return visit(ctx.orderedList());
    }

    @Override
    public PandocNode visitOrderedList(MyMDParser.OrderedListContext ctx) {
        List<List<Block>> items = new ArrayList<>();

        ListMarker firstMarker = null;

        for (int i = 0; i < ctx.orderedListItem().size(); i++) {
            MyMDParser.OrderedListItemContext itemCtx = ctx.orderedListItem(i);

            // Marker Logic
            String markerText;
            if (itemCtx.ORDERED_LIST_ITEM() != null) {
                markerText = itemCtx.ORDERED_LIST_ITEM().getText().trim();
            } else {
                markerText = "+";
            }

            if (!markerText.equals("+")) {
                ListMarker currentMarker = ListMarker.parse(markerText);
                if (firstMarker == null) {
                    firstMarker = currentMarker;
                } else {
                    // Strict Check logic
                    boolean styleMatch = (currentMarker.style == firstMarker.style);
                    if (!styleMatch) {
                        boolean isAlphaRomanConflict =
                                (firstMarker.style == ListAttributes.Style.LowerAlpha && currentMarker.style == ListAttributes.Style.LowerRoman) ||
                                        (firstMarker.style == ListAttributes.Style.UpperAlpha && currentMarker.style == ListAttributes.Style.UpperRoman);
                        if (isAlphaRomanConflict) styleMatch = true;
                    }
                    if (!styleMatch || currentMarker.delim != firstMarker.delim) {
                        throw new RuntimeException("Syntax Error: List marker mismatch. Expected " +
                                firstMarker.style + "/" + firstMarker.delim +
                                ", found " + currentMarker.style + "/" + currentMarker.delim);
                    }
                }
            }

            // Content Logic (Updated to use processListItem)
            List<ParseTree> inlineNodes = new ArrayList<>();
            for (ParseTree child : itemCtx.children) {
                if (child instanceof MyMDParser.InlineCommonContext) {
                    inlineNodes.add(child);
                }
            }
            items.add(processListItem(inlineNodes, itemCtx.nestedBody()));
        }

        // Attributes Logic
        ListAttributes attrs;
        if (firstMarker != null) {
            attrs = new ListAttributes(firstMarker.startNumber, firstMarker.style, firstMarker.delim);
        } else {
            attrs = new ListAttributes(1, ListAttributes.Style.Decimal, ListAttributes.Delim.Period);
        }

        return new OrderedList(attrs, items);
    }

    private List<Block> processListItem(List<ParseTree> inlineChildren, MyMDParser.NestedBodyContext nestedBodyCtx) {
        List<Block> blocks = new ArrayList<>();

        // 1. 处理首行文本 (The "Header" Paragraph)
        List<Inline> firstParaInlines = new ArrayList<>();
        for (ParseTree child : inlineChildren) {
            // 跳过标记符号本身的 Token 已经在调用方处理了，这里只传了 children
            // 但我们需要过滤掉 null 或者非 inline 的东西
            PandocNode node = visit(child);
            if (node instanceof Inline inline) {
                firstParaInlines.add(inline);
            }
        }
        if (!firstParaInlines.isEmpty()) {
            blocks.add(new Para(firstParaInlines));
        }

        // 2. 处理嵌套内容 (Nested Body)
        if (nestedBodyCtx != null) {
            for (ParseTree child : nestedBodyCtx.children) {
                PandocNode node = visit(child);
                if (node instanceof Block block) {
                    blocks.add(block);
                } else if (node instanceof List) {
                    // 有时候 visitBlock 可能返回 List<Block> (虽然现在架构主要是单个Block)
                    // 以防万一
                    try {
                        blocks.addAll((List<Block>) node);
                    } catch (Exception ignored) {}
                }
            }
        }

        return blocks;
    }
}