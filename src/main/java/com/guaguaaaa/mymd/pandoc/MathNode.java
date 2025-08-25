package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;

public class MathNode extends Inline {

    // 辅助内部类，用于生成 {"t": "TypeName"} 格式的 JSON 对象
    private static class MathTypeObject {
        private final String t;

        public MathTypeObject(String t) {
            this.t = t;
        }
    }

    public enum MathType {
        INLINE_MATH("InlineMath"),
        DISPLAY_MATH("DisplayMath");

        private final String pandocName;

        MathType(String pandocName) {
            this.pandocName = pandocName;
        }

        public String getPandocName() {
            return pandocName;
        }
    }

    // 构造函数已更新
    public MathNode(MathType type, String text) {
        // Pandoc AST 的新结构是: [ {"t": "MathType"}, "equation text" ]
        // 我们不再直接传递字符串，而是传递 MathTypeObject 的实例
        super("Math", Arrays.asList(new MathTypeObject(type.getPandocName()), text));
    }
}