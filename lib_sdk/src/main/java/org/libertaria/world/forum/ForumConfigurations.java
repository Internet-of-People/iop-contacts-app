package org.libertaria.world.forum;

import java.io.File;

/**
 * Created by mati on 22/11/16.
 */
public interface ForumConfigurations {


    void setIsRegistered(boolean isRegistered);

    boolean isRegistered();

    void setForumUser(String name, String password, String mail);

    void setApiKey(String apiKey);

    void setUrl(String url);

    void setWrapperUrl(String url);

    ForumProfile getForumUser();

    String getApiKey();

    void remove();

    String getUrl();

    String getWrapperUrl();

    void setUserImg(byte[] profImgData);

    File getUserImgFile();

    void setMail(String email);
}
