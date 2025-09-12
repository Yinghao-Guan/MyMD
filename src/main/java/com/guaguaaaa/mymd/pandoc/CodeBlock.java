package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;

// Represents a code block node in the Pandoc AST.
// The structure of a CodeBlock node in Pandoc is typically `[["", [], []], "the code"]`.
public class CodeBlock extends Block {
    /**
     * Constructs a new CodeBlock node.
     *
     * @param content The text content of the code block.
     */
    public CodeBlock(String content) {
        super("CodeBlock", Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()),
                content
        ));
    }
}