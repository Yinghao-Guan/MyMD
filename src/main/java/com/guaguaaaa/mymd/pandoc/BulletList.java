package com.guaguaaaa.mymd.pandoc;

import java.util.List;

// 无序列表节点
public class BulletList extends Block {
    // Pandoc 中，一个 BulletList 的内容 "c" 是一个列表，
    // 其中每个元素代表一个列表项 (<li>)，而每个列表项本身又是一个 Block 元素的列表。
    // 例如：[[Para [Str "item", Space, Str "1"]], [Para [Str "item", Space, Str "2"]]]
    public BulletList(List<List<Block>> items) {
        super("BulletList", items);
    }
}