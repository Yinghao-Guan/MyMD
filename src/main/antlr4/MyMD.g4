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
    | blockMath                 # BlockMathRule
    | bulletListBlock           # BulletListRule
    | codeBlock                 # CodeBlockRule
    | header                    # HeaderRule
    ;

// --- 新增：标题的解析器规则 ---
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

inline
    : bold                      # BoldInline
    | italic                    # ItalicInline
    | INLINE_MATH               # InlineMathInline
    | INLINE_CODE               # InlineCodeInline
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

PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;
SOFT_BREAK : '\r'? '\n' ;
HARD_BREAK : '\\\\' ;

INLINE_MATH : '$' ~[$]+ '$' ;
BLOCK_MATH: '$$' ( . | '\r' | '\n' )*? '$$' ;

INLINE_CODE : '`' ~[`\r\n]+ '`' ;
CODE_BLOCK : '```' ( . | '\r' | '\n' )*? '```' ;

// --- 新增：标题的词法规则 ---
H1 : '#' [ \t]+ ;
H2 : '##' [ \t]+ ;
H3 : '###' [ \t]+ ;
H4 : '####' [ \t]+ ;
H5 : '#####' [ \t]+ ;
H6 : '######' [ \t]+ ;

DASH : '-' ;
ESCAPED : '\\' ~[\r\n] ;

// --- 修改 TEXT 规则，使其不匹配 # ---
TEXT : ~[*\\$`# \t\r\n-]+ ;
SPACE : [ \t]+ ;