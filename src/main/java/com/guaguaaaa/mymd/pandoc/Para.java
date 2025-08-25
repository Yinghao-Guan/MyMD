package com.guaguaaaa.mymd.pandoc;

import java.util.List;

// 段落节点
public class Para extends Block {
    public Para(List<Inline> content) {
        super("Para", content);
    }
}