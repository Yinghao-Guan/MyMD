package com.guaguaaaa.mymd.pandoc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// 标题节点
public class Header extends Block {
    /**
     * Pandoc AST 中 Header 节点的结构为:
     * [level, [identifier, [classes], [[key, val], ...]], [Inline, ...]]
     * level: 标题级别 (1-6)
     * a. identifier: 自动生成的ID
     * b. classes: CSS类
     * c. key-val pairs: 键值对属性
     * content: 标题的行内元素列表
     *
     * 为了简化，我们只实现 level 和 content 部分。
     */
    public Header(int level, List<Inline> content) {
        super("Header", Arrays.asList(
                level,
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()),
                content
        ));
    }
}