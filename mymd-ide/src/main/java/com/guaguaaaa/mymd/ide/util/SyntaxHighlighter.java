package com.guaguaaaa.mymd.ide.util;

import com.guaguaaaa.mymd.core.parser.MyMDLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SyntaxHighlighter {

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        MyMDLexer lexer = new MyMDLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastTokenEnd = 0;

        // === 状态变量 ===
        boolean isBold = false;
        boolean isItalic = false;

        // 记录当前是否处于“行首”（即前面只有换行或空格）
        boolean isLineStart = true;

        for (Token token : lexer.getAllTokens()) {
            int tokenStart = token.getStartIndex();
            int tokenEnd = token.getStopIndex() + 1;

            if (tokenStart > lastTokenEnd) {
                spansBuilder.add(Collections.emptyList(), tokenStart - lastTokenEnd);
            }

            int tokenType = token.getType();
            List<String> styles = new ArrayList<>();

            // 处理动态样式 (Bold, Italic, List Marker)
            // 处理粗体 **
            if (tokenType == MyMDLexer.T__0) {
                isBold = !isBold;
                styles.add("bold-marker");
                isLineStart = false;
            }
            // 处理斜体 *
            else if (tokenType == MyMDLexer.STAR) {
                isItalic = !isItalic;
                styles.add("italic-marker");
                isLineStart = false;
            }
            // 处理连字符 - (仅在行首时视为列表)
            else if (tokenType == MyMDLexer.DASH) {
                if (isLineStart) {
                    styles.add("list-marker");
                }
                // 只要出现了非空格字符，这一行后面就不算“行首”了
                isLineStart = false;
            }
            // 维护行首状态
            else if (tokenType == MyMDLexer.HARD_BREAK || tokenType == MyMDLexer.PARAGRAPH_END || tokenType == MyMDLexer.SOFT_BREAK) {
                isBold = false;
                isItalic = false;
                isLineStart = true;
            }
            else if (tokenType == MyMDLexer.SPACE) {
                // 遇到空格，isLineStart 状态保持不变！
                // 这样 "  - Item" 里的 "-" 依然会被识别为列表
            }
            else {
                // 遇到其他任何文字，行首状态结束
                isLineStart = false;
            }

            // 获取基础样式 (Header, CodeBlock...)
            String baseStyle = getStyleClass(tokenType);
            if (baseStyle != null) {
                styles.add(baseStyle);
            }

            // 叠加样式 (Bold / Italic)
            if (baseStyle == null && !styles.contains("list-marker")) {
                if (isBold) styles.add("bold");
                if (isItalic) styles.add("italic");
            }

            spansBuilder.add(styles.isEmpty() ? Collections.emptyList() : styles, tokenEnd - tokenStart);
            lastTokenEnd = tokenEnd;
        }

        if (lastTokenEnd < text.length()) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastTokenEnd);
        }

        return spansBuilder.create();
    }

    private static String getStyleClass(int tokenType) {
        switch (tokenType) {
            case MyMDLexer.H1:
            case MyMDLexer.H2:
            case MyMDLexer.H3:
            case MyMDLexer.H4:
            case MyMDLexer.H5:
            case MyMDLexer.H6:
                return "header";

            case MyMDLexer.CODE_BLOCK:
                return "code-block";
            case MyMDLexer.INLINE_CODE:
                return "inline-code";

            case MyMDLexer.BLOCK_MATH:
            case MyMDLexer.INLINE_MATH:
                return "math";

            case MyMDLexer.GT:
                return "blockquote";

            case MyMDLexer.BANG:     // !
                return "image-marker";

            case MyMDLexer.LBRACKET: // [
            case MyMDLexer.RBRACKET: // ]
            case MyMDLexer.LPAREN:   // (
            case MyMDLexer.RPAREN:   // )
                return "link-marker";

            default:
                return null;
        }
    }
}