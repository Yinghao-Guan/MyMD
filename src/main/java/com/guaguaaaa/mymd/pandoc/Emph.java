package com.guaguaaaa.mymd.pandoc;

import java.util.List;

// 斜体节点
public class Emph extends Inline {
    public Emph(List<Inline> content) {
        super("Emph", content);
    }
}