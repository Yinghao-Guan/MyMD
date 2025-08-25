package com.guaguaaaa.mymd.pandoc;

// 代表行内元素的抽象类
public abstract class Inline implements PandocNode {
    public final String t;
    public final Object c;

    protected Inline(String t, Object c) {
        this.t = t;
        this.c = c;
    }
}