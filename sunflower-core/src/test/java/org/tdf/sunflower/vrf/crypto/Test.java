package org.tdf.sunflower.vrf.crypto;

public class Test {
    @org.junit.Test
    public void Test1() {
        T t = new T("".getBytes());
        t.isNull();
        assert t.rlpEncoded == null;
    }

    public static class T {
        private byte[] rlpEncoded;
        private byte[] other;

        public T(byte[] other) {
            this.other = other;
            this.rlpEncoded = null;
        }

        public T() {
            this.rlpEncoded = new byte[]{0, 1, 2};
        }

        public boolean isNull() {
            if (rlpEncoded == null) {
                return true;
            }
            return false;
        }
    }
}
