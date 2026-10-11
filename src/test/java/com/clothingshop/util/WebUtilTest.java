package com.clothingshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WebUtilTest {

    @Test
    void keepsPathsInsideTheApplication() {
        assertEquals("/orders?id=5", WebUtil.safeRedirectPath("/orders?id=5", "/"));
    }

    @Test
    void refusesRedirectsToOtherSites() {
        assertEquals("/", WebUtil.safeRedirectPath("https://evil.example", "/"));
        assertEquals("/", WebUtil.safeRedirectPath("//evil.example", "/"));
        assertEquals("/", WebUtil.safeRedirectPath("/\\evil.example", "/"));
        assertEquals("/", WebUtil.safeRedirectPath("/a\r\nSet-Cookie: x=1", "/"));
        assertEquals("/", WebUtil.safeRedirectPath("", "/"));
        assertEquals("/", WebUtil.safeRedirectPath(null, "/"));
    }
}
