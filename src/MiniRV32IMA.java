// j2me port of Charles Lohr's awesome rv32ima emulator
// made by the incredible guy by the name of Cucuzacu

public class MiniRV32IMA {

    public static final int MINIRV32_RAM_IMAGE_OFFSET = 0x80000000;

    public interface RVSystem {
        void handleMemStoreControl(int addy, int val);
        int handleMemLoadControl(int addy);
        void otherCSRWrite(int csrno, int val);
        int otherCSRRead(int csrno);
        void postExec(int pc, int ir, int trap);
    }

    public static class State {
        public int[] regs = new int[32];
        public int pc;
        public int mstatus;
        public int cyclel;
        public int cycleh;

        public int timerl;
        public int timerh;
        public int timermatchl;
        public int timermatchh;

        public int mscratch;
        public int mtvec;
        public int mie;
        public int mip;

        public int mepc;
        public int mtval;
        public int mcause;

        public int extraflags;
    }

    private static boolean isMMIORange(int n) {
        return (0x10000000 <= n) && (n < 0x12000000);
    }

    private static void store4(VirtualRAM image, int ofs, int val) { image.store4(ofs, val); }
    private static void store2(VirtualRAM image, int ofs, int val) { image.store2(ofs, val); }
    private static void store1(VirtualRAM image, int ofs, int val) { image.store1(ofs, val); }
    
    private static int load4(VirtualRAM image, int ofs) { return image.load4(ofs); }
    private static int load2(VirtualRAM image, int ofs) { return image.load2(ofs); }
    private static int load1(VirtualRAM image, int ofs) { return image.load1(ofs); }
    private static int load2Signed(VirtualRAM image, int ofs) { return image.load2Signed(ofs); }
    private static int load1Signed(VirtualRAM image, int ofs) { return image.load1Signed(ofs); }

    private static boolean isUnsignedLess(int a, int b) {
        return (a ^ 0x80000000) < (b ^ 0x80000000);
    }

    private static boolean isUnsignedGreaterOrEq(int a, int b) {
        return (a ^ 0x80000000) >= (b ^ 0x80000000);
    }

    public static int step(State state, VirtualRAM image, int ramSize, int elapsedUs, int count, RVSystem sys) {
        int new_timer = state.timerl + elapsedUs;
        if (isUnsignedLess(new_timer, state.timerl)) state.timerh++;
        state.timerl = new_timer;

        if ((isUnsignedGreaterOrEq(state.timerh, state.timermatchh) && state.timerh != state.timermatchh ||  (state.timerh == state.timermatchh && isUnsignedGreaterOrEq(state.timerl, state.timermatchl))) && (state.timermatchh != 0 || state.timermatchl != 0)) {
            state.extraflags &= ~4;
            state.mip |= 1 << 7;
        } else {
            state.mip &= ~(1 << 7);
        }

	if ((state.mip & state.mie) != 0) {
    	    state.extraflags &= ~4;
	}

        if ((state.extraflags & 4) != 0) return 1;

        int trap = 0;
        int rval = 0;
        int pc = state.pc;
        int cycle = state.cyclel;

        if ((state.mip & (1 << 7)) != 0 && (state.mie & (1 << 7)) != 0 && (state.mstatus & 0x8) != 0) {
            trap = 0x80000007;
            pc -= 4;
        } else {
            for (int icount = 0; icount < count; icount++) {
                int ir = 0;
                rval = 0;
                cycle++;
                int ofs_pc = pc - MINIRV32_RAM_IMAGE_OFFSET;

                if (isUnsignedGreaterOrEq(ofs_pc, ramSize)) {
                    trap = 1 + 1;
                    break;
                } else if ((ofs_pc & 3) != 0) {
                    trap = 1 + 0;
                    break;
                } else {
                    ir = load4(image, ofs_pc);
                    int rdid = (ir >>> 7) & 0x1f;

                    switch (ir & 0x7f) {
                        case 0x37:
                            rval = (ir & 0xfffff000);
                            break;
                        case 0x17:
                            rval = pc + (ir & 0xfffff000);
                            break;
                        case 0x6F:
                        {
                            int reladdy = ((ir & 0x80000000) >>> 11) | ((ir & 0x7fe00000) >>> 20) | ((ir & 0x00100000) >>> 9) | ((ir & 0x000ff000));
                            if ((reladdy & 0x00100000) != 0) reladdy |= 0xffe00000;
                            rval = pc + 4;
                            pc = pc + reladdy - 4;
                            break;
                        }
                        case 0x67:
                        {
                            int imm = ir >>> 20;
                            int imm_se = imm | (((imm & 0x800) != 0) ? 0xfffff000 : 0);
                            rval = pc + 4;
                            pc = ((state.regs[(ir >>> 15) & 0x1f] + imm_se) & ~1) - 4;
                            break;
                        }
                        case 0x63:
                        {
                            int immm4 = ((ir & 0xf00) >>> 7) | ((ir & 0x7e000000) >>> 20) | ((ir & 0x80) << 4) | ((ir >>> 31) << 12);
                            if ((immm4 & 0x1000) != 0) immm4 |= 0xffffe000;
                            int rs1 = state.regs[(ir >>> 15) & 0x1f];
                            int rs2 = state.regs[(ir >>> 20) & 0x1f];
                            immm4 = pc + immm4 - 4;
                            rdid = 0;
                            switch ((ir >>> 12) & 0x7) {
                                case 0: if (rs1 == rs2) pc = immm4; break;
                                case 1: if (rs1 != rs2) pc = immm4; break;
                                case 4: if (rs1 < rs2) pc = immm4; break;
                                case 5: if (rs1 >= rs2) pc = immm4; break;
                                case 6: if (isUnsignedLess(rs1, rs2)) pc = immm4; break;
                                case 7: if (isUnsignedGreaterOrEq(rs1, rs2)) pc = immm4; break;
                                default: trap = (2 + 1);
                            }
                            break;
                        }
                        case 0x03:
                        {
                            int rs1 = state.regs[(ir >>> 15) & 0x1f];
                            int imm = ir >>> 20;
                            int imm_se = imm | (((imm & 0x800) != 0) ? 0xfffff000 : 0);
                            int rsval = rs1 + imm_se;
                            
                            int funct3 = (ir >>> 12) & 0x7;
                            int sizeMinusOne = (1 << (funct3 & 3)) - 1; 

                            rsval -= MINIRV32_RAM_IMAGE_OFFSET;
                            
                            if (isUnsignedGreaterOrEq(rsval, ramSize - sizeMinusOne)) {
                                rsval += MINIRV32_RAM_IMAGE_OFFSET;
                                if (isMMIORange(rsval)) {
                                    int raw_val = sys.handleMemLoadControl(rsval);
                                    switch (funct3) {
                                        case 0: rval = (byte) raw_val; break;                   // lb (8-bit signed)
                                        case 1: rval = (short) raw_val; break;                  // lh (16-bit signed)
                                        case 2: rval = raw_val; break;                          // lw (32-bit)
                                        case 4: rval = raw_val & 0xFF; break;                   // lbu (8-bit unsigned)
                                        case 5: rval = raw_val & 0xFFFF; break;                 // lhu (16-bit unsigned)
                                        default: trap = (2 + 1);
                                    }
                                } else {
                                    trap = (5 + 1);
                                    rval = rsval;
                                }
                            } else {
                                switch (funct3) {
                                    case 0: rval = load1Signed(image, rsval); break;
                                    case 1: rval = load2Signed(image, rsval); break;
                                    case 2: rval = load4(image, rsval); break;
                                    case 4: rval = load1(image, rsval) & 0xFF; break;
                                    case 5: rval = load2(image, rsval) & 0xFFFF; break;
                                    default: trap = (2 + 1);
                                }
                            }
                            break;
                        }
                        case 0x23:
                        {
                            int rs1 = state.regs[(ir >>> 15) & 0x1f];
                            int rs2 = state.regs[(ir >>> 20) & 0x1f];
                            int addy = ((ir >>> 7) & 0x1f) | ((ir & 0xfe000000) >>> 20);
                            if ((addy & 0x800) != 0) addy |= 0xfffff000;
                            
                            int funct3 = (ir >>> 12) & 0x7;
                            int sizeMinusOne = (1 << (funct3 & 3)) - 1;

                            addy += rs1 - MINIRV32_RAM_IMAGE_OFFSET;
                            rdid = 0;

                            if (isUnsignedGreaterOrEq(addy, ramSize - sizeMinusOne)) {
                                addy += MINIRV32_RAM_IMAGE_OFFSET;
                                if (isMMIORange(addy)) {
                                    if (addy == 0x11100000)
                                        if (rs2 == 0x5555 || rs2 == 0x7777) return rs2;
                                    sys.handleMemStoreControl(addy, rs2);
                                } else {
                                    trap = (7 + 1);
                                    rval = addy;
                                }
                            } else {
                                switch (funct3) {
                                    case 0: store1(image, addy, rs2); break;
                                    case 1: store2(image, addy, rs2); break;
                                    case 2: store4(image, addy, rs2); break;
                                    default: trap = (2 + 1);
                                }
                            }
                            break;
                        }
                        case 0x13:
                        case 0x33:
                        {
                            int imm = ir >>> 20;
                            imm = imm | (((imm & 0x800) != 0) ? 0xfffff000 : 0);
                            int rs1 = state.regs[(ir >>> 15) & 0x1f];
                            boolean is_reg = (ir & 0x20) != 0;
                            int rs2 = is_reg ? state.regs[imm & 0x1f] : imm;

                            if (is_reg && (ir & 0x02000000) != 0) {
                                switch ((ir >>> 12) & 7) {
                                    case 0: rval = rs1 * rs2; break;
                                    case 1: rval = (int) (((long) rs1 * (long) rs2) >>> 32); break;
                                    case 2: rval = (int) (((long) rs1 * (rs2 & 0xFFFFFFFFL)) >>> 32); break;
                                    case 3: rval = (int) (((rs1 & 0xFFFFFFFFL) * (rs2 & 0xFFFFFFFFL)) >>> 32); break;
                                    case 4: if (rs2 == 0) rval = -1; else rval = (rs1 == 0x80000000 && rs2 == -1) ? rs1 : (rs1 / rs2); break;
                                    case 5: if (rs2 == 0) rval = 0xffffffff; else rval = (int) ((rs1 & 0xFFFFFFFFL) / (rs2 & 0xFFFFFFFFL)); break;
                                    case 6: if (rs2 == 0) rval = rs1; else rval = (rs1 == 0x80000000 && rs2 == -1) ? 0 : (rs1 % rs2); break;
                                    case 7: if (rs2 == 0) rval = rs1; else rval = (int) ((rs1 & 0xFFFFFFFFL) % (rs2 & 0xFFFFFFFFL)); break;
                                }
                            } else {
                                switch ((ir >>> 12) & 7) {
                                    case 0: rval = (is_reg && (ir & 0x40000000) != 0) ? (rs1 - rs2) : (rs1 + rs2); break;
                                    case 1: rval = rs1 << (rs2 & 0x1F); break;
                                    case 2: rval = (rs1 < rs2) ? 1 : 0; break;
                                    case 3: rval = isUnsignedLess(rs1, rs2) ? 1 : 0; break;
                                    case 4: rval = rs1 ^ rs2; break;
                                    case 5: rval = (ir & 0x40000000) != 0 ? (rs1 >> (rs2 & 0x1F)) : (rs1 >>> (rs2 & 0x1F)); break;
                                    case 6: rval = rs1 | rs2; break;
                                    case 7: rval = rs1 & rs2; break;
                                }
                            }
                            break;
                        }
                        case 0x0f: rdid = 0; break;
                        case 0x73:
                        {
                            int csrno = ir >>> 20;
                            int microop = (ir >>> 12) & 0x7;
                            if ((microop & 3) != 0) {
                                int rs1imm = (ir >>> 15) & 0x1f;
                                int rs1 = state.regs[rs1imm];
                                int writeval = rs1;

                                switch (csrno) {
                                    case 0x340: rval = state.mscratch; break;
                                    case 0x305: rval = state.mtvec; break;
                                    case 0x304: rval = state.mie; break;
                                    case 0xC00: rval = cycle; break;
                                    case 0xC80: rval = state.cycleh; break;
                                    case 0xC01: rval = state.timerl; break;
                                    case 0xC81: rval = state.timerh; break;
                                    case 0xC02: rval = cycle; break;
                                    case 0xC82: rval = state.cycleh; break;
                                    case 0x344: rval = state.mip; break;
                                    case 0x341: rval = state.mepc; break;
                                    case 0x300: rval = state.mstatus; break;
                                    case 0x342: rval = state.mcause; break;
                                    case 0x343: rval = state.mtval; break;
                                    case 0xf11: rval = 0xff0ff0ff; break;
                                    case 0x301: rval = 0x40401101; break;
                                    default: rval = sys.otherCSRRead(csrno); break;
                                }

                                switch (microop) {
                                    case 1: writeval = rs1; break;
                                    case 2: writeval = rval | rs1; break;
                                    case 3: writeval = rval & ~rs1; break;
                                    case 5: writeval = rs1imm; break;
                                    case 6: writeval = rval | rs1imm; break;
                                    case 7: writeval = rval & ~rs1imm; break;
                                }

                                switch (csrno) {
                                    case 0x340: state.mscratch = writeval; break;
                                    case 0x305: state.mtvec = writeval; break;
                                    case 0x304: state.mie = writeval; break;
                                    case 0x344: state.mip = writeval; break;
                                    case 0x341: state.mepc = writeval; break;
                                    case 0x300: state.mstatus = writeval; break;
                                    case 0x342: state.mcause = writeval; break;
                                    case 0x343: state.mtval = writeval; break;
                                    default: sys.otherCSRWrite(csrno, writeval); break;
                                }
                            } else if (microop == 0x0) {
                                rdid = 0;
                                if ((csrno & 0xff) == 0x02) {
                                    int startmstatus = state.mstatus;
                                    int startextraflags = state.extraflags;
                                    state.mstatus = (startmstatus & ~0x1888) | ((startmstatus & 0x80) >>> 4) | ((startextraflags & 3) << 11) | 0x80;
                                    state.extraflags = (startextraflags & ~3) | ((startmstatus >>> 11) & 3);
                                    pc = state.mepc - 4;
                                } else {
                                    switch (csrno) {
                                        case 0: 
    					    int priv = state.extraflags & 3;
    					    trap = (priv == 3) ? (11 + 1) : ((priv == 1) ? (9 + 1) : (8 + 1)); 
    					    break;
                                        case 1: trap = (3 + 1); break;
                                        case 0x105:
                                            state.mstatus |= 8;
                                            state.extraflags |= 4;
                                            if (isUnsignedGreaterOrEq(state.cyclel, cycle) && state.cyclel != cycle) state.cycleh++;
                                            state.cyclel = cycle;
                                            sys.postExec(pc, ir, trap);
                                            state.pc = pc + 4;
                                            return 1;
                                        default: trap = (2 + 1); break;
                                    }
                                }
                            } else {
                                trap = (2 + 1);
                            }
                            break;
                        }
                        case 0x2f:
                        {
                            int rs1 = state.regs[(ir >>> 15) & 0x1f];
                            int rs2 = state.regs[(ir >>> 20) & 0x1f];
                            int irmid = (ir >>> 27) & 0x1f;

                            rs1 -= MINIRV32_RAM_IMAGE_OFFSET;

                            if (isUnsignedGreaterOrEq(rs1, ramSize - 3)) {
                                trap = (7 + 1);
                                rval = rs1 + MINIRV32_RAM_IMAGE_OFFSET;
                            } else {
                                rval = load4(image, rs1);
                                boolean dowrite = true;
                                switch (irmid) {
                                    case 2:
                                        dowrite = false;
                                        state.extraflags = (state.extraflags & 0x07) | (rs1 << 3);
                                        break;
                                    case 3:
                                        rval = ((state.extraflags >>> 3) != (rs1 & 0x1fffffff)) ? 1 : 0;
                                        dowrite = (rval == 0);
                                        break;
                                    case 1: break;
                                    case 0: rs2 += rval; break;
                                    case 4: rs2 ^= rval; break;
                                    case 12: rs2 &= rval; break;
                                    case 8: rs2 |= rval; break;
                                    case 16: rs2 = (rs2 < rval) ? rs2 : rval; break;
                                    case 20: rs2 = (rs2 > rval) ? rs2 : rval; break;
                                    case 24: rs2 = isUnsignedLess(rs2, rval) ? rs2 : rval; break;
                                    case 28: rs2 = isUnsignedGreaterOrEq(rs2, rval) ? rs2 : rval; break;
                                    default: trap = (2 + 1); dowrite = false; break;
                                }
                                if (dowrite) store4(image, rs1, rs2);
                            }
                            break;
                        }
                        default: trap = (2 + 1);
                    }

                    if (trap != 0) {
                        state.pc = pc;
                        sys.postExec(pc, ir, trap);
                        break;
                    }

                    if (rdid != 0) {
                        state.regs[rdid] = rval;
                    }
                }
                sys.postExec(pc, ir, trap);
                pc += 4;
            }
        }

        if (trap != 0) {
            if ((trap & 0x80000000) != 0) {
                state.mcause = trap;
                state.mtval = 0;
                pc += 4;
            } else {
                state.mcause = trap - 1;
                state.mtval = (trap > 5 && trap <= 8) ? rval : pc;
            }
            state.mepc = pc;
            state.mstatus = (state.mstatus & ~0x1888) | ((state.mstatus & 0x08) << 4) | ((state.extraflags & 3) << 11);
            pc = state.mtvec - 4;
            state.extraflags |= 3;
            trap = 0;
            pc += 4;
        }

        if (isUnsignedGreaterOrEq(state.cyclel, cycle) && state.cyclel != cycle) state.cycleh++;
        state.cyclel = cycle;
        state.pc = pc;
        return 0;
    }
}
