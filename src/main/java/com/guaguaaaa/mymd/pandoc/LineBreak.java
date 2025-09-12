package com.guaguaaaa.mymd.pandoc;

// Represents a hard line break node in the Pandoc AST.
public class LineBreak extends Inline {
    /**
     * Constructs a new LineBreak node.
     */
    public LineBreak() {
        super("LineBreak", null);
    }
}