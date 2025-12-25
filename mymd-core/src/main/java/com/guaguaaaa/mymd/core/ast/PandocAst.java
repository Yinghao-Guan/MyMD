package com.guaguaaaa.mymd.core.ast;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Represents the top-level structure of a Pandoc document AST.
// This class is designed to be serialized to JSON for Pandoc consumption.
public class PandocAst implements PandocNode {
    @SerializedName("pandoc-api-version")
    private final List<Integer> pandocApiVersion = List.of(1, 23, 1);

    private final Map<String, Object> meta = Collections.emptyMap();
    private final List<Block> blocks;

    /**
     * Constructs a new PandocAst document.
     *
     * @param blocks A list of block-level elements that constitute the document's content.
     */

    public PandocAst(List<Block> blocks) {
        this.blocks = blocks;
    }

    public List<Block> getBlocks() {
        return blocks;
    }
}