package org.fermat.redtooth.can;

/**
 * A profile can be updated many times throughout its lifetime. Each update gives a
 * different version of the profile. A profile always has a human readable name. Optionally it
 * might have some extra attributes based on its application.
 *
 * Profiles are compared lexicographically based on their identity and version. That means:
 * <ul>
 * <li>Comparing 2 profiles with different identities always results in the same order independently
 * of their versions.</li>
 * <li>Comparing different versions of the same profile gives a strict chronological ordering.</li>
 * </ul>
 */
public interface CanProfile extends Comparable<CanProfile>, Iterable<CanProfile.Attribute> {
    /**
     * @return The entity that is shared among all versions of a profile.
     */
    ProfileIdentity getIdentity();

    /**
     * @return Human readable version identifier of the profile.
     */
    String getVersion();

    /**
     * @return A human-readable name of the profile. If it is a human, probably it is their full
     * name in the format they like to be presented to their contacts. If it is a service, it is
     * the brand they want to build reputation on.
     */
    String getDisplayName();

    boolean contains(String key);
    org.fermat.redtooth.can.Variant get(String key);

    class Attribute {
        private final String key;
        private final Variant value;

        public Attribute(String key, org.fermat.redtooth.can.Variant value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public Variant getValue() {
            return this.value;
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(obj instanceof Attribute)) return false;

            Attribute other = (Attribute) obj;
            return this.key.equals(other.key)
                    && this.value.equals(other.value);
        }

    }
}
