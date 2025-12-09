package com.guaguaaaa.mymd.pandoc;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Link extends Inline {
    /**
     * @param content  链接显示的文字 (List<Inline>)
     * @param url      跳转目标 URL
     */
    public Link(List<Inline> content, String url) {
        super("Link", Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()), // 1. Attr (id, classes, kv) - 暂时留空
                content,                                                             // 2. 显示内容
                Arrays.asList(url, "")                                               // 3. Target (url, title) - title 暂时留空
        ));
    }
}