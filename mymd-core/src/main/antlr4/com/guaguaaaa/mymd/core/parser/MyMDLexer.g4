lexer grammar MyMDLexer;

// ======================= Lexer Rules =======================

// 只有在文档绝对开始位置 (index 0) 且匹配 --- 时才进入 YAML 模式
YAML_START : { getCharIndex() == 0 }? '---' -> pushMode(YAML) ;

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

// 增加 BOLD_MARK 避免在 split grammar 中使用匿名 token
BOLD_MARK : '**' ;

DASH : '-' ;
STAR : '*' ;
BANG : '!' ;
GT   : '>' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
LPAREN   : '(' ;
RPAREN   : ')' ;

ESCAPED : '\\' ~[\r\n] ;

URL_TEXT : [a-zA-Z0-9:/.?#&=_%+]+ ;

TEXT : ~[*\\$`#[\]!>() \t\r\n-]+ ;

SPACE : [ \t]+ ;

// ======================= YAML Lexer Mode =======================
mode YAML;

YAML_END   : '---' -> popMode ;
YAML_COLON : ':' ;
YAML_KEY   : [a-zA-Z0-9_-]+ ;
YAML_VALUE : [ \t]* ~[\r\n]+ ;
YAML_NL    : '\r'? '\n' ;
YAML_WS    : [ \t]+ -> skip ;