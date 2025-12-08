package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Cite extends Inline {
    /**
     * Pandoc Cite 结构: [ [Citation列表], [Fallback显示文本列表] ]
     */
    public Cite(String citationId) {
        super("Cite", Arrays.asList(
                Collections.singletonList(new Citation(citationId)), // 引用数据
                Collections.singletonList(new Str("[@" + citationId + "]")) // 如果渲染失败，显示的原文本
        ));
    }
}