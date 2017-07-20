package iop.org.iop_sdk_android.core.service.modules;

/**
 * Created by furszy on 7/19/17.
 */

public enum ModuleId {

    PROFILES("mod_prof"),
    PAIRING("pairing"),
    CHAT("chat");

    private String id;

    ModuleId(String id) {
        this.id = id;
    }

    public static ModuleId getModuleIdById(String id){
        for (ModuleId moduleId : values()) {
            if (id.equals(moduleId.getId())){
                return moduleId;
            }
        }
        throw new IllegalArgumentException("No ModuleId with id: "+id);
    }

    public String getId() {
        return id;
    }
}
