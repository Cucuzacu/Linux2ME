//got this from https://github.com/AzizBgBoss/linux-j2me/blob/main/src/com/azizbgboss/bblinux/TinyFont.java

import javax.microedition.lcdui.Graphics;

public final class TinyFont {

    public static final int GLYPH_W = 3;
    public static final int GLYPH_H = 5;
    public static final int CELL_W = 4;
    public static final int CELL_H = 6;

    private TinyFont() {
    }

    public static void drawChar(Graphics g, char ch, int x, int y) {
        String[] rows = glyph(ch);
        int ry;
        int rx;

        for (ry = 0; ry < rows.length; ry++) {
            String row = rows[ry];
            for (rx = 0; rx < row.length(); rx++) {
                if (row.charAt(rx) == '1')
                    g.fillRect(x + rx, y + ry, 1, 1);
            }
        }
    }

    private static String[] glyph(char ch) {
        switch (ch) {
            case 'A': case 'a': return p("010", "101", "111", "101", "101");
            case 'B': case 'b': return p("110", "101", "110", "101", "110");
            case 'C': case 'c': return p("011", "100", "100", "100", "011");
            case 'D': case 'd': return p("110", "101", "101", "101", "110");
            case 'E': case 'e': return p("111", "100", "110", "100", "111");
            case 'F': case 'f': return p("111", "100", "110", "100", "100");
            case 'G': case 'g': return p("011", "100", "101", "101", "011");
            case 'H': case 'h': return p("101", "101", "111", "101", "101");
            case 'I': case 'i': return p("111", "010", "010", "010", "111");
            case 'J': case 'j': return p("001", "001", "001", "101", "010");
            case 'K': case 'k': return p("101", "101", "110", "101", "101");
            case 'L': case 'l': return p("100", "100", "100", "100", "111");
            case 'M': case 'm': return p("101", "111", "111", "101", "101");
            case 'N': case 'n': return p("101", "111", "111", "111", "101");
            case 'O': case 'o': return p("010", "101", "101", "101", "010");
            case 'P': case 'p': return p("110", "101", "110", "100", "100");
            case 'Q': case 'q': return p("010", "101", "101", "111", "011");
            case 'R': case 'r': return p("110", "101", "110", "101", "101");
            case 'S': case 's': return p("011", "100", "010", "001", "110");
            case 'T': case 't': return p("111", "010", "010", "010", "010");
            case 'U': case 'u': return p("101", "101", "101", "101", "111");
            case 'V': case 'v': return p("101", "101", "101", "101", "010");
            case 'W': case 'w': return p("101", "101", "111", "111", "101");
            case 'X': case 'x': return p("101", "101", "010", "101", "101");
            case 'Y': case 'y': return p("101", "101", "010", "010", "010");
            case 'Z': case 'z': return p("111", "001", "010", "100", "111");

            case '0': return p("111", "101", "101", "101", "111");
            case '1': return p("010", "110", "010", "010", "111");
            case '2': return p("110", "001", "010", "100", "111");
            case '3': return p("110", "001", "010", "001", "110");
            case '4': return p("101", "101", "111", "001", "001");
            case '5': return p("111", "100", "110", "001", "110");
            case '6': return p("011", "100", "110", "101", "010");
            case '7': return p("111", "001", "010", "010", "010");
            case '8': return p("010", "101", "010", "101", "010");
            case '9': return p("010", "101", "011", "001", "110");

            case '[': return p("110", "100", "100", "100", "110");
            case ']': return p("011", "001", "001", "001", "011");
            case '(': return p("001", "010", "010", "010", "001");
            case ')': return p("100", "010", "010", "010", "100");
            case '{': return p("011", "010", "110", "010", "011");
            case '}': return p("110", "010", "011", "010", "110");
            case '<': return p("001", "010", "100", "010", "001");
            case '>': return p("100", "010", "001", "010", "100");
            case '/': return p("001", "001", "010", "100", "100");
            case '\\': return p("100", "100", "010", "001", "001");
            case '-': return p("000", "000", "111", "000", "000");
            case '_': return p("000", "000", "000", "000", "111");
            case '=': return p("000", "111", "000", "111", "000");
            case '+': return p("000", "010", "111", "010", "000");
            case ':': return p("000", "010", "000", "010", "000");
            case ';': return p("000", "010", "000", "010", "100");
            case '.': return p("000", "000", "000", "000", "010");
            case ',': return p("000", "000", "000", "010", "100");
            case '\'': return p("010", "010", "000", "000", "000");
            case '"': return p("101", "101", "000", "000", "000");
            case '!': return p("010", "010", "010", "000", "010");
            case '?': return p("110", "001", "010", "000", "010");
            case '*': return p("000", "101", "010", "101", "000");
            case '#': return p("101", "111", "101", "111", "101");
            case '%': return p("101", "001", "010", "100", "101");
            case '&': return p("010", "101", "010", "101", "011");
            case '|': return p("010", "010", "010", "010", "010");
            case '@': return p("111", "101", "111", "100", "011");
            case '^': return p("010", "101", "000", "000", "000");
            case '`': return p("100", "010", "000", "000", "000");
            case '~': return p("000", "011", "110", "000", "000");
            case '$': return p("010", "110", "010", "011", "010");
            case ' ': return p("000", "000", "000", "000", "000");
            default: return p("111", "001", "010", "000", "010");
        }
    }

    private static String[] p(String a, String b, String c, String d, String e) {
        return new String[] { a, b, c, d, e };
    }
}
