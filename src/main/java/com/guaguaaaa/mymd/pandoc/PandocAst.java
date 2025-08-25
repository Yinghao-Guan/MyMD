package com.guaguaaaa.mymd.pandoc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// 代表整个Pandoc文档的顶层结构
public class PandocAst implements PandocNode {
    private final String pandocApiVersion = "1.22"; // Pandoc版本号，可以先固定
    private final Map<String, Object> meta = Collections.emptyMap(); // 文档元数据，暂时为空
    private final List<Block> blocks; // 文档的核心内容，由块级元素组成

    public PandocAst(List<Block> blocks) {
        this.blocks = blocks;
    }
}