lexer grammar MyMDLexer;

// ======================= Lexer Rules =======================

// YAML Block
YAML_BLOCK
    : '---' [ \t]* [\r\n]+ ( . | [\r\n] )*? [\r\n]+ '---'
    ;

// Horizontal Rule (水平分割线)
HR
    : '---' '-'*
    | '***' '*'*
    | '___' '_'*
    ;

// Headers
H1 : '#' [ \t]+ ;
H2 : '##' [ \t]+ ;
H3 : '###' [ \t]+ ;
H4 : '####' [ \t]+ ;
H5 : '#####' [ \t]+ ;
H6 : '######' [ \t]+ ;

// Lists & Quotes
DASH : '-' ;
STAR : '*' ;
BANG : '!' ;
GT   : '>' ;
LPAREN   : '(' ;
RPAREN   : ')' ;

// Text Format
BOLD_MARK : '**' ;
INLINE_MATH : '$' ~[$]+ '$' ;
BLOCK_MATH: '$$' ( . | [\r\n] )*? '$$' ;
INLINE_CODE : '`' ~[`\r\n]+ '`' ;
CODE_BLOCK : '```' ( . | [\r\n] )*? '```' ;
CITATION : '[' '@' [a-zA-Z0-9_:-]+ ']' ;
REF_ID : '[' ~[ \t\r\n\]]+ ']' ;

LBRACKET : '[' ;
RBRACKET : ']' ;

// Whitespace & Text
HARD_BREAK
    : '  ' ('\r'? '\n')
    | '\\'
    ;

PARAGRAPH_END : ('\r'? '\n') ('\r'? '\n')+ ;
SOFT_BREAK : '\r'? '\n' ;

ESCAPED : '\\' ~[\r\n] ;
URL_TEXT : [a-zA-Z0-9:/.?#&=_%+]+ ;
TEXT : ~[*\\$`#[\]!>() \t\r\n-]+ ;
SPACE : [ \t]+ ;