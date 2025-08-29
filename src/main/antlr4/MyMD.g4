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
    ;

paragraph
    : inline+ (PARAGRAPH_END | EOF)
    ;

// 新增：为块级元素创建独立的解析器规则，以便处理它们末尾的分隔符
blockMath
    : BLOCK_MATH (PARAGRAPH_END | EOF)?
    ;

bulletListBlock
    : bulletList (PARAGRAPH_END | EOF)?
    ;

codeBlock
    : CODE_BLOCK (PARAGRAPH_END | EOF)?
    ;

// --- 列表的定义保持不变 ---
bulletList
    : listItem+
    ;

listItem
    : DASH SPACE inline+ (SOFT_BREAK | PARAGRAPH_END)
    ;

// --- 行内元素的定义保持不变 ---
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

// ===== Lexer Rules (词法分析器规则) - 保持不变 =====

PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;
SOFT_BREAK : '\r'? '\n' ;
HARD_BREAK : '\\\\' ;

INLINE_MATH : '$' ~[$]+ '$' ;
BLOCK_MATH: '$$' ( . | '\r' | '\n' )*? '$$' ;

INLINE_CODE : '`' ~[`\r\n]+ '`' ;
CODE_BLOCK : '```' ( . | '\r' | '\n' )*? '```' ;

DASH : '-' ;
ESCAPED : '\\' ~[\r\n] ;

TEXT : ~[*\\$` \t\r\n-]+ ;
SPACE : [ \t]+ ;