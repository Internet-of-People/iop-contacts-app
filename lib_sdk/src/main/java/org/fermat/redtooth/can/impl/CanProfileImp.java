package org.fermat.redtooth.can.impl;

import org.fermat.redtooth.can.CanProfile;
import org.fermat.redtooth.can.Variant;
import java.util.Iterator;
import java.util.Map;

class CanProfileImp implements org.fermat.redtooth.can.CanProfile {
    private final ProfileIdentity identity;
    private final String version;
    private final String displayName;
    private final Map<String, Variant> attrs;

    CanProfileImp(ProfileIdentity identity, String version, String displayName, Map<String, Variant> attrs) {
        this.identity = identity;
        this.version = version;
        this.displayName = displayName;
        this.attrs = attrs;
    }

    @Override public ProfileIdentity getIdentity() {
        return this.identity;
    }

    @Override public String getVersion() {
        return this.version;
    }

    @Override public String getDisplayName() {
        return this.displayName;
    }

    @Override public boolean contains(String key) {
        return this.attrs.containsKey(key);
    }

    @Override public org.fermat.redtooth.can.Variant get(String key) {
        return this.attrs.get(key);
    }

    @Override public int compareTo(CanProfile other) {
        int cmpIdentity = this.identity.compareTo(other.getIdentity());
        if (cmpIdentity != 0)
            return cmpIdentity;
        return this.version.compareTo(other.getVersion());
    }

    @Override public Iterator<Attribute> iterator() {
        return new AttrIterator(attrs.entrySet().iterator());
    }

    class AttrIterator implements Iterator<Attribute>{
        private final Iterator<Map.Entry<String, Variant>> entryIterator;

        public AttrIterator(Iterator<Map.Entry<String, Variant>> entryIterator) {
            this.entryIterator = entryIterator;
        }

        @Override public boolean hasNext() {
            return this.entryIterator.hasNext();
        }

        @Override public Attribute next() {
            Map.Entry<String, Variant> next = this.entryIterator.next();
            return new Attribute(next.getKey(), next.getValue());
        }

        @Override
        public void remove() {

        }
    }
}
