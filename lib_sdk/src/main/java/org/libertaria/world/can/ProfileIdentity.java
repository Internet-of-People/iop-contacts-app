package org.libertaria.world.can;

/**
 * A profile is the building block of the internet of people. It might be an identity of a human, a
 * brand of a service or a principal assigned to a device. It is the finest-grain entity that can
 * authenticate itself, therefore it can authorize allowed actions in different applications.
 */
public interface ProfileIdentity extends Comparable<ProfileIdentity> {
    /**
     * @return Stable identifier of the profile that remains the same throughout the lifetime of the
     * profile
     */
    String getIdentifier();
}

