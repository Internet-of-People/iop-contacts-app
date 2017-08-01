package org.libertaria.world.can;

public interface ProfileRepository {
    /**
     * Synchronous call to retrieve the latest version of a profile based on its identifier. Note,
     * that because of current IPFS limitations, this call might take several seconds to even a
     * minute. Make sure you provide wake-lock before your call and cancel the task running the call
     * based on a timeout.
     *
     * @param identifier A multihash encoded identifier of the profile
     * @return null if there is no profile on the content addressed network with that identifier, or
     * the current version if there is one.
     *
     * @throws IllegalArgumentException if the identifier is not a valid multihash encoded identifier
     * @throws RuntimeException if the network is unavailable or some other problems happened while
     * retrieving the current version
     */
    CanProfile getById(String identifier);
}
