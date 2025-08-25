package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;

// 行内代码节点 (继承自 Inline)
public class Code extends Inline {
    // Pandoc AST 中 Code 节点的结构为: [["", [], []], "the code"]
    public Code(String content) {
        super("Code", Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()),
                content
        ));
    }
}