package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;

// This class can represent both InlineMath and DisplayMath
public class MathNode extends Inline {

    // Use an enum for type safety to distinguish between math types
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

    // Updated constructor to accept a type
    public MathNode(MathType type, String text) {
        // The Pandoc AST structure is ["MathType", "equation text"]
        super("Math", Arrays.asList(type.getPandocName(), text));
    }
}