package org.libertaria.world.global.p2p;

public interface Bootstrappable {
    void discoveryStep();
    int getNodeCount();
}
