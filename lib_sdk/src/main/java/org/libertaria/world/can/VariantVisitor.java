package org.libertaria.world.can;

public interface VariantVisitor {
    void visitBytes(byte[] value);
    void visitInt(int value);
    void visitLong(long value);
    void visitDouble(double value);
    void visitBoolean(boolean value);
    void visitString(String value);
}
