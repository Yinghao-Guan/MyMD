package com.guaguaaaa.mymd.pandoc;

// 普通文本节点
public class Str extends Inline {
    public Str(String text) {
        super("Str", text);
    }
}