package com.example.v_sat_compass.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class KaTeXHelperTest {

    @Test
    public void containsMath_detectsLatexDelimiters() {
        assertTrue(KaTeXHelper.containsMath("Tính giá trị của $f(x) = x^2 + 1$"));
        assertTrue(KaTeXHelper.containsMath("Giải phương trình $$\\int_0^1 x dx$$"));
        assertTrue(KaTeXHelper.containsMath("Hàm số \\[y = \\sqrt{x}\\] liên tục"));
        assertTrue(KaTeXHelper.containsMath("Cho biểu thức \\(a + b\\)"));
        assertTrue(KaTeXHelper.containsMath("Tính \\frac{1}{2} + 3"));
        assertFalse(KaTeXHelper.containsMath("Câu hỏi văn học không có công thức toán"));
        assertFalse(KaTeXHelper.containsMath(null));
        assertFalse(KaTeXHelper.containsMath(""));
    }

    @Test
    public void escapeForHtml_escapesCharactersAndLineBreaks() {
        String input = "x < 5 & y > 2\nLine 2";
        String escaped = KaTeXHelper.escapeForHtml(input);
        assertEquals("x &lt; 5 &amp; y &gt; 2<br>Line 2", escaped);
    }

    @Test
    public void buildKaTeXHtml_embedsAssetsAndContent() {
        String html = KaTeXHelper.buildKaTeXHtml("f(x) = $x^2$");
        assertTrue(html.contains("file:///android_asset/katex/katex.min.css"));
        assertTrue(html.contains("file:///android_asset/katex/katex.min.js"));
        assertTrue(html.contains("file:///android_asset/katex/auto-render.min.js"));
        assertTrue(html.contains("f(x) = $x^2$"));
    }

    @Test
    public void containsMath_handlesExplanationFormulas() {
        assertTrue(KaTeXHelper.containsMath("Lời giải: Đặt $t = \\sqrt{x^2 + 1}$, suy ra $dt = \\frac{x}{\\sqrt{x^2+1}}dx$."));
        assertFalse(KaTeXHelper.containsMath("Sau tính từ sở hữu 'Her' cần danh từ"));
    }
}