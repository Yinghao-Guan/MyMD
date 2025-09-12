package com.guaguaaaa.mymd.pandoc;

import java.util.List;

// Represents a bold (strong) text node in the Pandoc AST.
public class Strong extends Inline {
    /**
     * Constructs a new Strong node.
     *
     * @param content A list of inline elements contained within the bold formatting.
     */
    public Strong(List<Inline> content) {
        super("Strong", content);
    }
}