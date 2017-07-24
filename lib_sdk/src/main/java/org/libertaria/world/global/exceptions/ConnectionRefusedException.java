package org.libertaria.world.global.exceptions;

/**
 * Created by mati on 15/12/16.
 */
public class ConnectionRefusedException extends Throwable {
    public ConnectionRefusedException(String s, Exception e) {
        super(s,e);
    }
}
