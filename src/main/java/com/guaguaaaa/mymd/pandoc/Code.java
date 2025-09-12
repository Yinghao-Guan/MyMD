package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;

// Represents an inline code node in the Pandoc AST.
// The structure of a Code node in Pandoc is typically `[["", [], []], "the code"]`.
public class Code extends Inline {
    /**
     * Constructs a new Code node.
     *
     * @param content The text content of the inline code.
     */
    public Code(String content) {
        super("Code", Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()),
                content
        ));
    }
}