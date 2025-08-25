package com.guaguaaaa.mymd.pandoc;

// 代表块级元素的抽象类
public abstract class Block implements PandocNode {
    // "t" 代表 type, "c" 代表 content
    public final String t;
    public final Object c;

    protected Block(String t, Object c) {
        this.t = t;
        this.c = c;
    }
}