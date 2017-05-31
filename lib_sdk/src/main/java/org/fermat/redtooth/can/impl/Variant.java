package org.fermat.redtooth.can.impl;

import org.fermat.redtooth.can.VariantVisitor;

public class Variant {
    abstract static class Value implements org.fermat.redtooth.can.Variant {
        @Override public byte[] getBytes() throws IllegalStateException {
            throw new IllegalStateException();
        }

        @Override public int getInt() throws IllegalStateException {
            throw new IllegalStateException();
        }

        @Override public long getLong() throws IllegalStateException {
            throw new IllegalStateException();
        }

        @Override public double getDouble() throws IllegalStateException {
            throw new IllegalStateException();
        }

        @Override public boolean getBoolean() throws IllegalStateException {
            throw new IllegalStateException();
        }

        @Override public java.lang.String getString() throws IllegalStateException {
            throw new IllegalStateException();
        }
    }

    static class Binary extends Value {
        private final byte[] value;

        Binary(byte[] value)
        {
            this.value = value;
        }

        @Override public byte[] getBytes() throws IllegalStateException {
            return this.value;
        }

        @Override public void accept(VariantVisitor visitor) {
            visitor.visitBytes(this.value);
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Binary)) return false;

            Binary other = (Binary)obj;
            return this.value.equals(other.value);
        }

        @Override public int hashCode() {
            return this.value.hashCode();
        }
    }

    static class Uint32 extends Value {
        private final int value;

        Uint32(int value)
        {
            this.value = value;
        }

        @Override public int getInt() throws IllegalStateException {
            return this.value;
        }

        @Override public void accept(VariantVisitor visitor) {
            visitor.visitInt(this.value);
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Uint32)) return false;

            Uint32 other = (Uint32)obj;
            return this.value == other.value;
        }

        @Override public int hashCode() {
            return this.value;
        }
    }

    public static class Uint64 extends Value {
        private final long value;

        public Uint64(long value)
        {
            this.value = value;
        }

        @Override public long getLong() throws IllegalStateException {
            return this.value;
        }

        @Override public void accept(VariantVisitor visitor) {
            visitor.visitLong(this.value);
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Uint64)) return false;

            Uint64 other = (Uint64)obj;
            return this.value == other.value;
        }

        @Override public int hashCode() {
            return Long.valueOf(this.value).hashCode();
        }
    }

    static class Double extends Value {
        private final double value;

        Double(double value)
        {
            this.value = value;
        }

        @Override public double getDouble() throws IllegalStateException {
            return this.value;
        }

        @Override public void accept(VariantVisitor visitor) {
            visitor.visitDouble(this.value);
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Double)) return false;

            Double other = (Double)obj;
            return this.value == other.value;
        }

        @Override public int hashCode() {
            return java.lang.Double.valueOf(this.value).hashCode();
        }
    }

    static class Bool extends Value {
        private final boolean value;

        Bool(boolean value)
        {
            this.value = value;
        }

        @Override public boolean getBoolean() throws IllegalStateException {
            return this.value;
        }

        @Override public void accept(VariantVisitor visitor) {
            visitor.visitBoolean(this.value);
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Bool)) return false;

            Bool other = (Bool)obj;
            return this.value == other.value;
        }

        @Override public int hashCode() {
            return Boolean.valueOf(this.value).hashCode();
        }
    }

    static class String extends Value {
        private final java.lang.String value;

        String(java.lang.String value)
        {
            this.value = value;
        }

        @Override public java.lang.String getString() throws IllegalStateException {
            return this.value;
        }

        @Override public void accept(VariantVisitor visitor) {
            visitor.visitString(this.value);
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof String)) return false;

            String other = (String)obj;
            return this.value.equals(other.value);
        }

        @Override public int hashCode() {
            return this.value.hashCode();
        }
    }

    private Variant() {}
}
