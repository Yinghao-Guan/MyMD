package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;

// 代码块节点 (继承自 Block)
public class CodeBlock extends Block {
    // Pandoc AST 中 CodeBlock 节点的结构为: [["", [], []], "the code"]
    public CodeBlock(String content) {
        super("CodeBlock", Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()),
                content
        ));
    }
}