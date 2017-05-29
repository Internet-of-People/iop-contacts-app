package org.fermat.redtooth.forum;

/**
 * Created by mati on 23/11/16.
 */

public class ForumProfile {

    private long forumId;
    private String name;
    private String username;
    private String password;
    private String email;

    public ForumProfile(long forumId, String name, String username) {
        this.forumId = forumId;
        this.name = name;
        this.username = username;
    }

    public ForumProfile(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public ForumProfile(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public long getForumId() {
        return forumId;
    }

    public String getName() {
        return name;
    }

    public void setForumId(long forumId) {
        this.forumId = forumId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
