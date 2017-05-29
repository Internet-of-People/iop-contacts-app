package org.fermat.redtooth.can.impl;


class ProfileIdentity implements org.fermat.redtooth.can.ProfileIdentity {
    private final String identifier;

    ProfileIdentity(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int compareTo(org.fermat.redtooth.can.ProfileIdentity other) {
        return this.identifier.compareTo(other.getIdentifier());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof org.fermat.redtooth.can.ProfileIdentity)) return false;

        org.fermat.redtooth.can.ProfileIdentity other = (org.fermat.redtooth.can.ProfileIdentity) obj;
        return this.identifier.equals(other.getIdentifier());
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public String toString() {
        return String.format("CAN(%s)", this.identifier);
    }
}
