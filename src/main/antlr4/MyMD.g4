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
    | HARD_BREAK                # HardBreakInline
    | SOFT_BREAK                # SoftBreakInline
    | ESCAPED                   # EscapedInline
    | TEXT                      # TextInline
    | SPACE                     # SpaceInline
    ;

bold
    : '**' inline+ '**'
    ;


// ===== Lexer Rules (词法分析器规则) =====

HARD_BREAK : '\\\\' ;

// 修正后的规则
PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;

// 注意：SOFT_BREAK 规则必须在 PARAGRAPH_END 之后
SOFT_BREAK : '\r'? '\n' ;

ESCAPED : '\\' ~[\r\n] ;

TEXT : ~[*\\ \t\r\n]+ ; // 在中括号的排除列表里，加上空格和 \t

SPACE : [ \t]+ ;