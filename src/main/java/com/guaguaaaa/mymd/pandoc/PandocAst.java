package com.guaguaaaa.mymd.pandoc;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// 代表整个Pandoc文档的顶层结构
public class PandocAst implements PandocNode {
    // --- 修改：将 API 版本更新为 Pandoc 期望的版本 ---
    @SerializedName("pandoc-api-version")
    private final List<Integer> pandocApiVersion = List.of(1, 23, 1);

    private final Map<String, Object> meta = Collections.emptyMap();
    private final List<Block> blocks;

    public PandocAst(List<Block> blocks) {
        this.blocks = blocks;
    }
}