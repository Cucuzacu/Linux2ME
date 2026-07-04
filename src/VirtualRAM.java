// a file that gives you FREE RAM!!!!!!!

import javax.microedition.rms.RecordStore;
import java.io.InputStream;
import java.io.IOException;

public class VirtualRAM {
    private static final int PAGE_SHIFT = 14;
    private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    private static final int PAGE_MASK = PAGE_SIZE - 1;
    
    private static final int MAX_CACHE_PAGES = 16;

    public final int length;
    private int numPages;

    private byte[][] cachePages;
    private int[] cacheTags;
    private long[] cacheAge;
    private boolean[] dirty;
    private long tick = 0;

    private RecordStore swap;
    private int[] pageToRecordId;

    public VirtualRAM(int sizeInBytes) {
        this.length = sizeInBytes;
        this.numPages = (sizeInBytes + PAGE_SIZE - 1) / PAGE_SIZE;

        int cacheSize = Math.min(this.numPages, MAX_CACHE_PAGES);
        this.cachePages = new byte[cacheSize][];
        this.cacheTags = new int[cacheSize];
        this.cacheAge = new long[cacheSize];
        this.dirty = new boolean[cacheSize];

        for (int i = 0; i < cacheSize; i++) {
            cacheTags[i] = -1;
        }

        if (this.numPages > cacheSize) {
            this.pageToRecordId = new int[this.numPages];
            try {
                RecordStore.deleteRecordStore("VRAM_SWAP");
            } catch (Exception e) {}
            try {
                swap = RecordStore.openRecordStore("VRAM_SWAP", true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] getPage(int pageIdx, boolean forWrite) {
        int cacheLen = cacheTags.length;
        
        for (int i = 0; i < cacheLen; i++) {
            if (cacheTags[i] == pageIdx) {
                cacheAge[i] = ++tick;
                if (forWrite) dirty[i] = true;
                return cachePages[i];
            }
        }

        int slot = -1;
        long oldestAge = Long.MAX_VALUE;

        for (int i = 0; i < cacheLen; i++) {
            if (cacheTags[i] == -1) {
                slot = i;
                break;
            }
            if (cacheAge[i] < oldestAge) {
                oldestAge = cacheAge[i];
                slot = i;
            }
        }

        if (cacheTags[slot] != -1) {
            int oldPage = cacheTags[slot];
            if (dirty[slot] && swap != null) {
                try {
                    if (pageToRecordId[oldPage] == 0) {
                        pageToRecordId[oldPage] = swap.addRecord(cachePages[slot], 0, PAGE_SIZE);
                    } else {
                        swap.setRecord(pageToRecordId[oldPage], cachePages[slot], 0, PAGE_SIZE);
                    }
                } catch (Exception e) {
                        System.out.println("VRAM SWAP FAILED! Storage full?");
                        e.printStackTrace();
                }
            }
        }

        if (cachePages[slot] == null) {
            cachePages[slot] = new byte[PAGE_SIZE];
        }

        if (swap != null && pageToRecordId[pageIdx] != 0) {
            try {
                swap.getRecord(pageToRecordId[pageIdx], cachePages[slot], 0);
            } catch (Exception e) {
                byte[] p = cachePages[slot];
                for (int i = 0; i < PAGE_SIZE; i++) p[i] = 0;
            }
        } else {
            byte[] p = cachePages[slot];
            for (int i = 0; i < PAGE_SIZE; i++) p[i] = 0;
        }

        cacheTags[slot] = pageIdx;
        cacheAge[slot] = ++tick;
        dirty[slot] = forWrite;

        return cachePages[slot];
    }

    public void copyFrom(byte[] src, int srcPos, int destAddr, int len) {
        int remaining = len;
        int currentSrc = srcPos;
        int currentDest = destAddr;
        while (remaining > 0) {
            int pageIdx = currentDest >>> PAGE_SHIFT;
            int ofs = currentDest & PAGE_MASK;
            int chunk = Math.min(remaining, PAGE_SIZE - ofs);
            byte[] p = getPage(pageIdx, true);
            System.arraycopy(src, currentSrc, p, ofs, chunk);
            currentSrc += chunk;
            currentDest += chunk;
            remaining -= chunk;
        }
    }

    public int readFromStream(InputStream is, int destAddr, int maxLen) throws IOException {
        int totalRead = 0;
        int currentDest = destAddr;
        while (totalRead < maxLen) {
            int pageIdx = currentDest >>> PAGE_SHIFT;
            int ofs = currentDest & PAGE_MASK;
            int chunk = Math.min(maxLen - totalRead, PAGE_SIZE - ofs);
            byte[] p = getPage(pageIdx, true);
            int r = is.read(p, ofs, chunk);
            if (r <= 0) break;
            currentDest += r;
            totalRead += r;
        }
        return totalRead;
    }

    public void store1(int addr, int val) {
        getPage(addr >>> PAGE_SHIFT, true)[addr & PAGE_MASK] = (byte) val;
    }

    public void store2(int addr, int val) {
        int pageIdx = addr >>> PAGE_SHIFT;
        int ofs = addr & PAGE_MASK;
        if (ofs <= PAGE_SIZE - 2) {
            byte[] p = getPage(pageIdx, true);
            p[ofs] = (byte) val;
            p[ofs + 1] = (byte) (val >>> 8);
        } else {
            store1(addr, val);
            store1(addr + 1, val >>> 8);
        }
    }

    public void store4(int addr, int val) {
        int pageIdx = addr >>> PAGE_SHIFT;
        int ofs = addr & PAGE_MASK;
        if (ofs <= PAGE_SIZE - 4) {
            byte[] p = getPage(pageIdx, true);
            p[ofs] = (byte) val;
            p[ofs + 1] = (byte) (val >>> 8);
            p[ofs + 2] = (byte) (val >>> 16);
            p[ofs + 3] = (byte) (val >>> 24);
        } else {
            store1(addr, val);
            store1(addr + 1, val >>> 8);
            store1(addr + 2, val >>> 16);
            store1(addr + 3, val >>> 24);
        }
    }

    public int load1(int addr) {
        return getPage(addr >>> PAGE_SHIFT, false)[addr & PAGE_MASK] & 0xFF;
    }

    public int load1Signed(int addr) {
        return getPage(addr >>> PAGE_SHIFT, false)[addr & PAGE_MASK];
    }

    public int load2(int addr) {
        int pageIdx = addr >>> PAGE_SHIFT;
        int ofs = addr & PAGE_MASK;
        if (ofs <= PAGE_SIZE - 2) {
            byte[] p = getPage(pageIdx, false);
            return (p[ofs] & 0xFF) | ((p[ofs + 1] & 0xFF) << 8);
        } else {
            return load1(addr) | (load1(addr + 1) << 8);
        }
    }

    public int load2Signed(int addr) {
        int pageIdx = addr >>> PAGE_SHIFT;
        int ofs = addr & PAGE_MASK;
        if (ofs <= PAGE_SIZE - 2) {
            byte[] p = getPage(pageIdx, false);
            return (p[ofs] & 0xFF) | (p[ofs + 1] << 8);
        } else {
            return load1(addr) | ((byte)load1(addr + 1) << 8);
        }
    }

    public int load4(int addr) {
        int pageIdx = addr >>> PAGE_SHIFT;
        int ofs = addr & PAGE_MASK;
        if (ofs <= PAGE_SIZE - 4) {
            byte[] p = getPage(pageIdx, false);
            return (p[ofs] & 0xFF) |
                   ((p[ofs + 1] & 0xFF) << 8) |
                   ((p[ofs + 2] & 0xFF) << 16) |
                   ((p[ofs + 3] & 0xFF) << 24);
        } else {
            return load1(addr) |
                   (load1(addr + 1) << 8) |
                   (load1(addr + 2) << 16) |
                   (load1(addr + 3) << 24);
        }
    }

    public void clear() {
        tick = 0;
        for (int i = 0; i < cacheTags.length; i++) {
            cacheTags[i] = -1;
            cacheAge[i] = 0;
            dirty[i] = false;
        
            if (cachePages[i] != null) {
                for (int j = 0; j < PAGE_SIZE; j++) {
                    cachePages[i][j] = 0;
                }
            }
        }

        if (swap != null) {
            try {
                swap.closeRecordStore();
                RecordStore.deleteRecordStore("VRAM_SWAP");
            
                swap = RecordStore.openRecordStore("VRAM_SWAP", true);
            
                if (pageToRecordId != null) {
                    for (int i = 0; i < pageToRecordId.length; i++) {
                        pageToRecordId[i] = 0;
                    }
                }
            } catch (Exception e) {
                try {
                    swap = RecordStore.openRecordStore("VRAM_SWAP", true);
                } catch (Exception ex) {}
            }
        }
    }
    
    public void cleanup() {
        if (swap != null) {
            try {
                swap.closeRecordStore();
                RecordStore.deleteRecordStore("VRAM_SWAP");
            } catch (Exception e) {}
        }
    }
}
