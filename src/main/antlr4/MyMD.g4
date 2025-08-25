grammar MyMD;

document : block+ EOF ;

block
    : paragraph
    ;

paragraph
    : inline+ (NEWLINE inline+)* (NEWLINE NEWLINE+)?  // 单换行折叠，双换行分段
    ;

inline
    : bold
    | ESCAPED
    | TEXT
    ;

bold : '**' inline+ '**' ;

ESCAPED : '\\' . ;     // 转义符
TEXT    : ~[*\\\r\n]+ ; // 普通文字

NEWLINE : '\r'? '\n' ;

WS : [ \t]+ -> skip ;
