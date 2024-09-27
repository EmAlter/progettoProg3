package org.progetto.email;

import java.io.Serializable;

public class EmailWrapper implements Serializable {
    private Email email;
    private String target;
    public EmailWrapper(Email email, String target) {
        this.email = email;
        this.target = target;
    }

    public Email getEmail() {
        return email;
    }

    public String getTarget() {
        return target;
    }
}
