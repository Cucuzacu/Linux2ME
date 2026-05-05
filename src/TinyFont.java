//got this from https://github.com/AzizBgBoss/linux-j2me/blob/main/src/com/azizbgboss/bblinux/TinyFont.java

import javax.microedition.lcdui.Graphics;

public final class TinyFont {

    public static final int GLYPH_W = 3;
    public static final int GLYPH_H = 5;
    public static final int CELL_W = 4;
    public static final int CELL_H = 6;

    private static final short[] GLYPHS = new short[128];

    static {
        short defaultGlyph = 061202;
        for (int i = 0; i < 128; i++) {
            GLYPHS[i] = defaultGlyph;
        }

        GLYPHS['A'] = GLYPHS['a'] = 025755;
        GLYPHS['B'] = GLYPHS['b'] = 065656;
        GLYPHS['C'] = GLYPHS['c'] = 034443;
        GLYPHS['D'] = GLYPHS['d'] = 065556;
        GLYPHS['E'] = GLYPHS['e'] = 074647;
        GLYPHS['F'] = GLYPHS['f'] = 074644;
        GLYPHS['G'] = GLYPHS['g'] = 034553;
        GLYPHS['H'] = GLYPHS['h'] = 055755;
        GLYPHS['I'] = GLYPHS['i'] = 072227;
        GLYPHS['J'] = GLYPHS['j'] = 011152;
        GLYPHS['K'] = GLYPHS['k'] = 055655;
        GLYPHS['L'] = GLYPHS['l'] = 044447;
        GLYPHS['M'] = GLYPHS['m'] = 057755;
        GLYPHS['N'] = GLYPHS['n'] = 057775;
        GLYPHS['O'] = GLYPHS['o'] = 025552;
        GLYPHS['P'] = GLYPHS['p'] = 065644;
        GLYPHS['Q'] = GLYPHS['q'] = 025573;
        GLYPHS['R'] = GLYPHS['r'] = 065655;
        GLYPHS['S'] = GLYPHS['s'] = 034216;
        GLYPHS['T'] = GLYPHS['t'] = 072222;
        GLYPHS['U'] = GLYPHS['u'] = 055557;
        GLYPHS['V'] = GLYPHS['v'] = 055552;
        GLYPHS['W'] = GLYPHS['w'] = 055775;
        GLYPHS['X'] = GLYPHS['x'] = 055255;
        GLYPHS['Y'] = GLYPHS['y'] = 055222;
        GLYPHS['Z'] = GLYPHS['z'] = 071247;

        GLYPHS['0'] = 075557;
        GLYPHS['1'] = 026227;
        GLYPHS['2'] = 061247;
        GLYPHS['3'] = 061216;
        GLYPHS['4'] = 055711;
        GLYPHS['5'] = 074616;
        GLYPHS['6'] = 034652;
        GLYPHS['7'] = 071222;
        GLYPHS['8'] = 025252;
        GLYPHS['9'] = 025316;

        GLYPHS['['] = 064446;
        GLYPHS[']'] = 031113;
        GLYPHS['('] = 012221;
        GLYPHS[')'] = 042224;
        GLYPHS['{'] = 032623;
        GLYPHS['}'] = 062326;
        GLYPHS['<'] = 012421;
        GLYPHS['>'] = 042124;
        GLYPHS['/'] = 011244;
        GLYPHS['\\']= 044211;
        GLYPHS['-'] = 000700;
        GLYPHS['_'] = 000007;
        GLYPHS['='] = 007070;
        GLYPHS['+'] = 002720;
        GLYPHS[':'] = 002020;
        GLYPHS[';'] = 002024;
        GLYPHS['.'] = 000002;
        GLYPHS[','] = 000024;
        GLYPHS['\'']= 022000;
        GLYPHS['"'] = 055000;
        GLYPHS['!'] = 022202;
        GLYPHS['?'] = 061202;
        GLYPHS['*'] = 005250;
        GLYPHS['#'] = 057575;
        GLYPHS['%'] = 051245;
        GLYPHS['&'] = 025253;
        GLYPHS['|'] = 022222;
        GLYPHS['@'] = 075743;
        GLYPHS['^'] = 025000;
        GLYPHS['`'] = 042000;
        GLYPHS['~'] = 003600;
        GLYPHS['$'] = 026232;
        GLYPHS[' '] = 000000;
    }

    private TinyFont() {
    }

    public static void drawChar(Graphics g, char ch, int x, int y) {
        short glyph = (ch >= 0 && ch < 128) ? GLYPHS[ch] : GLYPHS['?'];
        
        if (glyph == 0) return;

        int mask = 16384; 

        for (int ry = 0; ry < GLYPH_H; ry++) {
            for (int rx = 0; rx < GLYPH_W; rx++) {
                if ((glyph & mask) != 0) {
                    g.fillRect(x + rx, y + ry, 1, 1);
                }
                mask >>= 1;
            }
        }
    }
}
