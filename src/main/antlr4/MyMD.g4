grammar MyMD;

@header {
package com.guaguaaaa.mymd;
}

// ===== Parser Rules (解析器规则) =====

document
    : block+ EOF
    ;

block
    : paragraph                 # ParagraphBlock
    ;

paragraph
    : inline+ (PARAGRAPH_END | EOF)
    ;

inline
    : bold                      # BoldInline
    | italic                    # ItalicInline
    | HARD_BREAK                # HardBreakInline
    | SOFT_BREAK                # SoftBreakInline
    | ESCAPED                   # EscapedInline
    | TEXT                      # TextInline
    | SPACE                     # SpaceInline
    ;

bold
    : '**' inline+ '**'
    ;

italic
    : '*' inline+ '*'
    ;

// ===== Lexer Rules (词法分析器规则) =====

// 两个或更多的换行符，代表一个段落的真正结束
PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;

// 单个换行符，它在段落内部
// 注意：这个规则必须在 PARAGRAPH_END 之后，以遵循 ANTLR 的“最长匹配原则”
SOFT_BREAK : '\r'? '\n' ;

HARD_BREAK : '\\\\' ;

ESCAPED : '\\' ~[\r\n] ;

TEXT : ~[*\\ \t\r\n]+ ;

SPACE : [ \t]+ ;