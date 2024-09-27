package org.progetto.email;

import org.progetto.client.CM;

import java.util.List;
import java.util.Random;

public class IDChecker {

    private List<Email> list;
    private CM model;

    public IDChecker(List<Email> list, CM model) {
        this.list = list;
        this.model = model;
    }

    public void addElement(Email email) {
        list.add(email);
    }

    /**
     * Serve per generare un ID unico per ogni email nuova
     *
     * @return l'ID unico generato
     */
    public String uniqueID() {
        String newID;
        do {
            newID = generateID();
        } while (idExists(newID)); //Se l'ID già esiste, ne genero un altro
        return newID;
    }

    /**
     * Genera un ID di quattro caratteri alfanumerici
     *
     * @return l'ID di quattro caratteri alfanumerici
     */
    private String generateID() {
        String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        char[] idChars = new char[4];
        for (int i = 0; i < 4; i++) {
            idChars[i] = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));
        }
        return new String(idChars);
    }

    /**
     * Verifica che 'id' non esista già
     *
     * @param id
     * @return true se l'ID esiste già tra le email nella lista, false altrimenti
     */
    public boolean idExists(String id) {
        for (Email e : list) {
            if (e.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
