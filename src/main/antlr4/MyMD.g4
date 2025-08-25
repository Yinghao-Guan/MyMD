grammar MyMD;

@header {
package com.guaguaaaa.mymd;
}

// ... grammar MyMD; 和 @header 不变 ...

document : block+ EOF ;

block
    : paragraph
    ;

paragraph
    : inline+ (NEWLINE inline+)* (NEWLINE NEWLINE+)?
    ;

inline
    : bold                      # BoldInline
    | ESCAPED                   # EscapedInline
    | TEXT                      # TextInline
    | SPACE                     # SpaceInline   // <-- 新增
    ;

bold : '**' inline+ '**' ;

ESCAPED : '\\' . ;
// 修改TEXT，让它不包含空格和制表符
TEXT    : ~[*\\ \t\r\n]+ ;
// 新增SPACE规则
SPACE   : [ \t]+ ;

NEWLINE : '\r'? '\n' ;

// 注释掉原来的WS -> skip，因为我们现在想自己处理空格
// WS : [ \t]+ -> skip ;