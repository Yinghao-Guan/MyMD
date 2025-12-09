package com.guaguaaaa.mymd.pandoc;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Image extends Inline {
    /**
     * @param altText  图片的替代文本 (List<Inline>)
     * @param url      图片路径
     */
    public Image(List<Inline> altText, String url) {
        super("Image", Arrays.asList(
                Arrays.asList("", Collections.emptyList(), Collections.emptyList()), // 1. Attr
                altText,                                                             // 2. Alt text
                Arrays.asList(url, "fig:")                                           // 3. Target (url, title) - "fig:" 触发 Pandoc 自动生成题注
        ));
    }
}