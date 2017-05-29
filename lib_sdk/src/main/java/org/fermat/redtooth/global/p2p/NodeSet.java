package org.fermat.redtooth.global.p2p;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Random;

public class NodeSet<T> {
    private final Class<T> clazz;
    private final Random random = new Random();
    private final HashSet<T> nodeSet = new HashSet<>();
    private T[] nodeArray = null;

    public NodeSet(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void add(T node) {
        nodeSet.add(node);
        nodeArray = null;
    }

    public int getCount() {
        return nodeSet.size();
    }

    public T getRandomNode() {
        cacheArray();

        int chosen = random.nextInt(nodeSet.size());
        return nodeArray[chosen];
    }

    @SuppressWarnings("unchecked")
    private void cacheArray() {
        if (nodeArray != null) return;

        nodeArray = (T[]) Array.newInstance(this.clazz, getCount());
        nodeSet.toArray(nodeArray);
    }
}