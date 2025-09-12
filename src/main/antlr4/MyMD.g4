grammar MyMD;

@header {
package com.guaguaaaa.mymd;
}

// ======================= Parser Rules =======================

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

// A header block with a level from 1 to 6, followed by inline content.
header
    : (H1 | H2 | H3 | H4 | H5 | H6) inline+ (PARAGRAPH_END | EOF)
    ;

// A paragraph is a sequence of inline elements.
paragraph
    : inline+ (PARAGRAPH_END | EOF)
    ;

// A display math block enclosed by $$.
blockMath
    : BLOCK_MATH (PARAGRAPH_END | EOF)?
    ;

bulletListBlock
    : bulletList (PARAGRAPH_END | EOF)?
    ;

// A code block enclosed by ```.
codeBlock
    : CODE_BLOCK (PARAGRAPH_END | EOF)?
    ;

// A bullet list is one or more list items.
bulletList
    : listItem+
    ;

// A list item starts with a dash and a space, followed by inline content.
listItem
    : DASH SPACE inline+ (SOFT_BREAK | PARAGRAPH_END)
    ;

// Inline elements.
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

// Bold text enclosed by double asterisks.
bold
    : '**' inline+ '**'
    ;

// Italic text enclosed by single asterisks.
italic
    : '*' inline+ '*'
    ;

// ======================= Lexer Rules =======================

// Marks the end of a paragraph (two or more newlines).
PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;

// A soft break is a single newline.
SOFT_BREAK : '\r'? '\n' ;

// A hard break is a backslash.
HARD_BREAK : '\\\\' ;

// Inline math enclosed by a single dollar sign.
INLINE_MATH : '$' ~[$]+ '$' ;
// A block math element enclosed by double dollar signs.
BLOCK_MATH: '$$' ( . | '\r' | '\n' )*? '$$' ;

// Inline code enclosed by a single backtick.
INLINE_CODE : '`' ~[`\r\n]+ '`' ;
// A code block enclosed by three backticks.
CODE_BLOCK : '```' ( . | '\r' | '\n' )*? '```' ;

// Header tokens based on the number of hash symbols.
H1 : '#' [ \t]+ ;
H2 : '##' [ \t]+ ;
H3 : '###' [ \t]+ ;
H4 : '####' [ \t]+ ;
H5 : '#####' [ \t]+ ;
H6 : '######' [ \t]+ ;

// A dash, used for list items.
DASH : '-' ;

// An escaped character (e.g., `\*`).
ESCAPED : '\\' ~[\r\n] ;

// A general text token. Modified to not match special characters.
TEXT : ~[*\\$`# \t\r\n-]+ ;

// A space token.
SPACE : [ \t]+ ;