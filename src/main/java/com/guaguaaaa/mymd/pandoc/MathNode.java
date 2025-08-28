package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;

// 行内公式节点
public class MathNode extends Inline {
    public MathNode(String text) {
        // Pandoc AST 中 Math 节点的 "c" 是一个包含两个元素的列表：
        // 第一个是固定的元数据 ["t": "InlineMath"] (这里简化为字符串 "InlineMath")
        // 第二个是公式的文本内容
        super("Math", Arrays.asList("InlineMath", text));
    }
}