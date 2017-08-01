package org.libertaria.world.core.interfaces;

import org.libertaria.world.profile_server.model.Profile;

import java.io.Serializable;

/**
 * Created by mati on 17/05/17.
 *
 * // todo: class to use when the user need to do something on the redtooth. shared with upper layers. (android -> intents).
 * Each connection represents one profile.
 *
 */

public interface RedtoothConnection extends Serializable{

    /** Profile connection id */
    long getConnectionId();
    /** Each connection is mapped with one profile */
    Profile getProfile();
    /** If the connection is ready */
    boolean isConnected();

}
