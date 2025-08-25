package com.guaguaaaa.mymd.pandoc;

import java.util.List;

// 粗体节点
public class Strong extends Inline {
    public Strong(List<Inline> content) {
        super("Strong", content);
    }
}