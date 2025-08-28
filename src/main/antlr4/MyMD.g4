grammar MyMD;

@header {
package com.guaguaaaa.mymd;
}

// ===== Parser Rules (解析器规则) =====

document
    : block+ EOF
    ;

block
    : paragraph PARAGRAPH_END
    | paragraph EOF
    ;

paragraph
    : inline+
    ;

inline
    : bold                      # BoldInline
    | italic                    # ItalicInline  // 添加此行
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

HARD_BREAK : '\\\\' ;

PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;

SOFT_BREAK : '\r'? '\n' ;

ESCAPED : '\\' ~[\r\n] ;

TEXT : ~[*\\ \t\r\n]+ ;

SPACE : [ \t]+ ;