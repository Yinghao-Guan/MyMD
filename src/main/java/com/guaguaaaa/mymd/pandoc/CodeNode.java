package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;

// This class can represent both Inline Code and Block Code
public class CodeNode extends Inline {

    // Use an enum for type safety to distinguish between code types
    public enum CodeType {
        INLINE("Code"),
        BLOCK("CodeBlock"); // We'll handle the block-level wrapping in the visitor

        private final String pandocName;

        CodeType(String pandocName) {
            this.pandocName = pandocName;
        }

        public String getPandocName() {
            return pandocName;
        }
    }

    // Updated constructor to accept a type
    public CodeNode(CodeType type, String text) {
        // Pandoc AST for Code: ["", [], []], "the code"
        // Pandoc AST for CodeBlock: [["", [], []], "the code"]
        // The structure is very similar, so we can unify them here.
        // The visitor will determine if this node is wrapped in a Para or not.
        super(type.getPandocName(), Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()),
                text
        ));
    }
}