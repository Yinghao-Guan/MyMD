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
    | INLINE_MATH               # InlineMathInline  // <-- CHANGED: Now a direct token
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

// NEW and IMPROVED rule for inline math
// It matches a '$', followed by any sequence of characters that are NOT '$', then a final '$'.
INLINE_MATH : '$' ~[$]+ '$' ;

ESCAPED : '\\' ~[\r\n] ;

// The TEXT rule no longer needs to exclude '$'
TEXT : ~[*\\$ \t\r\n]+ ;

SPACE : [ \t]+ ;