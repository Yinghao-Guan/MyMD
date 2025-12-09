grammar MyMD;

@header {
package com.guaguaaaa.mymd;
}

// ======================= Parser Rules =======================

document
    : block+ EOF
    ;

block
    : horizontalRule          # HorizontalRuleBlock
    | blockquote              # BlockQuoteBlock
    | bulletListBlock         # BulletListRule
    | codeBlock               # CodeBlockRule
    | header                  # HeaderRule
    | blockMath               # BlockMathRule
    | paragraph               # ParagraphBlock
    ;

// 分割线：三个或更多的 - 或 *
horizontalRule
    : (DASH DASH DASH+ | STAR STAR STAR+) (SOFT_BREAK | PARAGRAPH_END | EOF)
    ;

// 引用块：以 > 开头
blockquote
    : GT SPACE? inline+ (PARAGRAPH_END | EOF)
    ;

header
    : (H1 | H2 | H3 | H4 | H5 | H6) inline+ (PARAGRAPH_END | EOF)
    ;

paragraph
    : inline+ (PARAGRAPH_END | EOF)
    ;

blockMath
    : BLOCK_MATH (PARAGRAPH_END | EOF)?
    ;

bulletListBlock
    : bulletList (PARAGRAPH_END | EOF)?
    ;

codeBlock
    : CODE_BLOCK (PARAGRAPH_END | EOF)?
    ;

bulletList
    : listItem+
    ;

listItem
    : DASH SPACE inline+ (SOFT_BREAK | PARAGRAPH_END)
    ;

// Inline elements.
inline
    : image                   # ImageInline
    | link                    # LinkInline
    | bold                    # BoldInline
    | italic                  # ItalicInline
    | INLINE_MATH             # InlineMathInline
    | INLINE_CODE             # InlineCodeInline
    | citation                # CitationInline
    | lbracket                # LBracketInline
    | rbracket                # RBracketInline
    | bang                    # BangInline      // !
    | gt                      # GtInline        // >
    | lparen                  # LParenInline    // (
    | rparen                  # RParenInline    // )
    | urlText                 # UrlTextInline
    | dash                    # DashInline
    | star                    # StarInline
    | HARD_BREAK              # HardBreakInline
    | SOFT_BREAK              # SoftBreakInline
    | ESCAPED                 # EscapedInline
    | TEXT                    # TextInline
    | SPACE                   # SpaceInline
    ;

citation : CITATION ;
lbracket : LBRACKET ;
rbracket : RBRACKET ;
bold     : '**' inline+ '**' ;
italic   : '*' inline+ '*' ;

bang     : BANG ;
gt       : GT ;
lparen   : LPAREN ;
rparen   : RPAREN ;
urlText  : URL_TEXT ;
dash     : DASH ;
star     : STAR ;

image : BANG LBRACKET inline* RBRACKET LPAREN url RPAREN ;
link  : LBRACKET inline+ RBRACKET LPAREN url RPAREN ;

url : (URL_TEXT | DASH | STAR | TEXT)+ ;

// ======================= Lexer Rules =======================

HARD_BREAK
    : '  ' ('\r'? '\n')
    | '\\'
    ;
PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;
SOFT_BREAK : '\r'? '\n' ;

INLINE_MATH : '$' ~[$]+ '$' ;
BLOCK_MATH: '$$' ( . | '\r' | '\n' )*? '$$' ;
INLINE_CODE : '`' ~[`\r\n]+ '`' ;
CODE_BLOCK : '```' ( . | '\r' | '\n' )*? '```' ;
CITATION : '[' '@' [a-zA-Z0-9_:-]+ ']' ;

H1 : '#' [ \t]+ ;
H2 : '##' [ \t]+ ;
H3 : '###' [ \t]+ ;
H4 : '####' [ \t]+ ;
H5 : '#####' [ \t]+ ;
H6 : '######' [ \t]+ ;

DASH : '-' ;
STAR : '*' ;      // 显式定义 STAR
BANG : '!' ;      // 图片用
GT   : '>' ;      // 引用块用
LBRACKET : '[' ;
RBRACKET : ']' ;
LPAREN   : '(' ;  // 链接用
RPAREN   : ')' ;  // 链接用

ESCAPED : '\\' ~[\r\n] ;

// URL 识别：允许字母数字和常见URL符号
URL_TEXT : [a-zA-Z0-9:/.?#&=_%+]+ ;

// 文本规则
TEXT : ~[*\\$`#[\]!>() \t\r\n-]+ ;

SPACE : [ \t]+ ;