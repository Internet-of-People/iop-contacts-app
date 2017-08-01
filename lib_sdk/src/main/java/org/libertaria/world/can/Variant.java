package org.libertaria.world.can;

public interface Variant {
    byte[] getBytes() throws IllegalStateException;
    int getInt() throws IllegalStateException;
    long getLong() throws IllegalStateException;
    double getDouble() throws IllegalStateException;
    boolean getBoolean() throws IllegalStateException;
    String getString() throws IllegalStateException;

    void accept(VariantVisitor visitor);
}
