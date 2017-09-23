package org.libertaria.world.global;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 21/9/2017.
 * <p>
 * This interface handles the events throughout the platform.
 * The implementor of this interface have the responsibility of store all
 * the registered listeners and, whenever received, raise a new event for each of them.
 */

public interface PlatformEventManager {
    /**
     * Register an event listener that should be expecting any event.
     *
     * @param eventListener to be registered
     */
    void registerEventListener(PlatformEventListener eventListener);
}
