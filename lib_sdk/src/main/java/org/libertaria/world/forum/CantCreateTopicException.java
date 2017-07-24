package org.libertaria.world.forum;

/**
 * Created by mati on 01/12/16.
 */
public class CantCreateTopicException extends Throwable {

    public CantCreateTopicException() {
    }

    public CantCreateTopicException(String message) {
        super(message);
    }
}
