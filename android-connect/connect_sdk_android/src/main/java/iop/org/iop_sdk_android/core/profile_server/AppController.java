package iop.org.iop_sdk_android.core.profile_server;


import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.global.ContextWrapper;
import org.fermat.redtooth.profile_server.ModuleRedtooth;

/**
 * Created by mati on 28/03/17.
 */
public interface AppController extends ContextWrapper,IoPConnectContext {

    AppModule getModule();

    ModuleRedtooth getProfileServer();




}
