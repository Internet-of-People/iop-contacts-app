package org.fermat.redtooth.core.client.servers;

import org.fermat.redtooth.core.client.basic.IoSessionImp;
import org.fermat.redtooth.core.client.interfaces.IoSessionConf;
import org.fermat.redtooth.core.client.interfaces.ProtocolDecoder;
import org.fermat.redtooth.core.client.interfaces.ProtocolEncoder;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

import java.nio.channels.Channel;

/**
 * Created by mati on 12/05/17.
 */

public class PfSession extends IoSessionImp<IopProfileServer.Message> {

    public PfSession(long id, Channel channel, IoSessionConf ioSessionConf) {
        super(id, channel,ioSessionConf);
    }


}
