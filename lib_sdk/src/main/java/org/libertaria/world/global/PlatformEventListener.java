package org.libertaria.world.global;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 21/9/2017.
 */

public interface PlatformEventListener {
    /**
     * Receives an event raised somewhere else on the application.
     *
     * @param intentMessage the event message raised.
     * @param systemContext the context of the system on which this event was raised.
     */
    void onReceive(SystemContext systemContext, IntentMessage intentMessage);
}
