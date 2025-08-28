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
    | BLOCK_MATH                # BlockMathBlock      // <-- ADDED: New block type for math
    ;

paragraph
    : inline+ (PARAGRAPH_END | EOF)
    ;

inline
    : bold                      # BoldInline
    | italic                    # ItalicInline
    | INLINE_MATH               # InlineMathInline
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

// REMOVED: The inlineMath parser rule is no longer needed here.

// ===== Lexer Rules (词法分析器规则) =====

PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;
SOFT_BREAK : '\r'? '\n' ;
HARD_BREAK : '\\\\' ;

INLINE_MATH : '$' ~[$]+ '$' ;

BLOCK_MATH: '$$' ( . | '\r' | '\n' )*? '$$' ;

ESCAPED : '\\' ~[\r\n] ;

// The TEXT rule no longer needs to exclude '$'
TEXT : ~[*\\$ \t\r\n]+ ;

SPACE : [ \t]+ ;