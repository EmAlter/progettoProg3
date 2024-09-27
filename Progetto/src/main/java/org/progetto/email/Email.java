package org.progetto.email;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Oggetto che rappresenta un'email con attributi:
 * - id indica l'ID della email
 * - mitt indica il mittente della email
 * - dest indica i destinatari della email
 * - argument indica l'oggetto della email
 * - txt indica il testo della email
 * - date indica la data d'invio della email
 * - forward indica se la email inviata è stata inoltrata da una esistente
 * - isNew indica una email appena ricevuta
 */
public class Email implements Serializable {
    private String id;
    private String mitt;
    private List<String> dest;
    private String argument;
    private String txt;
    private String date;
    private boolean forward = false;
    private boolean isNew = true;
    private boolean toDelete = false;
    private boolean update = false;

    public Email(String id, String mitt, List<String> dest, String argument, String txt, String date) {
        this.id = id;
        this.mitt = mitt;
        this.dest = new ArrayList<>(dest);
        this.argument = argument;
        this.txt = txt;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getMitt() {
        return mitt;
    }

    public List<String> getDest() {
        return dest;
    }

    public String getArgument() {
        return argument;
    }

    public String getTxt() {
        return txt;
    }

    public String getDate() {
        return date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public void setMitt(String mitt) {
        this.mitt = mitt;
    }

    public void setDest(List<String> dest) {
        this.dest = dest;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }


    //I metodi successivi sono le get dei flags: new, update, forward, delete
    public boolean isForward() {
        return forward;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isToDelete() {
        return toDelete;
    }

    public boolean isToUpdate() {
        return update;
    }

    //I metodi successivi sono le set dei flags: new, update, forward, delete
    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void setDelete(boolean delete) {
        toDelete = delete;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }


    @Override
    public String toString() {
        return this.id + "&"
                + this.mitt + "&"
                + String.join(";", this.dest) + "&"
                + this.argument + "&"
                + this.date + "&"
                + this.txt;

    }
}
