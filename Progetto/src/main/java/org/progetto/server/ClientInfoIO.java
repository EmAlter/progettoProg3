package org.progetto.server;

import javafx.application.Platform;
import org.progetto.email.Email;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientInfoIO {
    private final String name;
    private final ObjectOutputStream out;
    private List<Email> emailList;
    private final String path;
    private final String allClientsPath = "src\\main\\java\\org\\progetto\\allfiles\\clients.txt";

    public ClientInfoIO(String name, ObjectOutputStream out, List<Email> emailList) throws IOException {
        this.name = name;
        this.out = out;
        this.emailList = emailList;
        this.path = "src\\main\\java\\org\\progetto\\allfiles\\" + name;

        if (!createNewDirectory()) {
            readAllEmailsFromFolder(this.path);
        } else {
            //Se il Client viene creato per la prima volta
            //vengono create email casuali da aggiungere
            List<String> r = new ArrayList<>();
            r.add(name);

            addEmailToToFile(new Email(uniqueID(), "artoria@gmail.com", r,
                    "Hello!", "We need to discuss the car price", dateTime()));
            addEmailToToFile(new Email(uniqueID(), "irelia@gmail.com", r,
                    "Do not forget!", "Let's meet up at the same place", dateTime()));
        }

    }


    protected boolean createNewDirectory() {
        return new File(this.path).mkdirs();
    }

    public String getName() {
        return name;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public List<Email> getEmailList() {
        return emailList;
    }

    public String getPath() {
        return path;
    }

    /**
     * Crea un array contenente tutti files che sono nella directory e li itera leggendoli uno a uno
     *
     * @param folderPath il path di dove si trovano i files
     */
    public void readAllEmailsFromFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                readEmailFromFile(file.getAbsolutePath());
            }
        }
    }

    /**
     * Leggo da un file, creato precedentemente, tutti gli attributi dell'oggetto email
     * Creo il nuovo oggetto email con gli attributi ottenuti e lo aggiungo alla lista
     *
     * @param path indica il percorso del file
     */
    public void readEmailFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] emailData = line.split("&"); //simbolo con la quale sono divisi gli attributi

                String id = emailData[0];
                String mitt = emailData[1];
                List<String> dest = new ArrayList<>(List.of(emailData[2].split(";"))); //simbolo con la quale sono divisi i destinatari
                String argument = emailData[3];
                String date = emailData[4];
                String txt = emailData[5];

                //Una volta raggiunto txt controlla che non sia scritto anche su nuove linee
                //in caso positivo legge ogni linea e la aggiunge a txt
                //questo serve per le email che contengono "reply"
                line = br.readLine();
                while (line != null) {
                    txt += "\n" + line;
                    line = br.readLine();
                }


                Email email = new Email(id, mitt, dest, argument, txt, date);
                email.setNew(false);
                emailList.add(email);
            }

        } catch (Exception err) {
            err.printStackTrace(); //Gli errori possono essere molti, di conseguenza mostro lo stacktrace

        }
    }

    /**
     * Aggiungo l'email alla ObservableList e creo un nuovo file contenente gli attributi dell'email
     *
     * @param email
     * @throws IOException
     */
    private synchronized void addEmailToToFile(Email email) throws IOException {
        emailList.add(email);
        File f = new File(this.path + "\\" + this.name + email.getId() + ".txt");
        f.createNewFile();
        writeEmailToFile(email, this.path + "\\" + this.name + email.getId() + ".txt");
    }


    /**
     * Scrive le informazioni di un'email in nuovo file
     *
     * @param email oggetto Email da scrivere
     * @param path  percorso nella quale creare il nuovo file
     */
    public synchronized void writeEmailToFile(Email email, String path) {
        try (FileWriter fileWriter = new FileWriter(path, true);) {
            fileWriter.write(email.toString());
            fileWriter.flush();
            System.out.println("The Email was successfully written to a file");

        } catch (Exception err) {
            err.printStackTrace(); //In questo caso gli errori possibili sono molti, di conseguenza mostro lo stacktrace
        }
    }

    /**
     * Il metodo permette l'eliminazione dell'email in entrata sia dalla lista che dai file
     * Riavviando il client, le email eliminate non saranno più presenti
     *
     * @param email email da eliminare
     */
    public void deleteEmail(Email email) {
        //Elimina l'email selezionata dalla lista
        emailList.remove(email);

        //Ottiene il path del file e lo elimina basandosi sull'ID
        String filePath = this.path + "\\" + this.name + email.getId() + ".txt";
        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("Deleted file: " + filePath);
        } else {
            System.out.println("Failed to delete file: " + filePath);
        }


    }

    /**
     * Verifico che l'email non esista già, se non esiste la aggiungo, altrimenti la aggiorno
     *
     * @param email
     */
    protected synchronized void add(Email email) {
        Platform.runLater(() -> {
            try {
                if (!idExists(email.getId())) {
                    addEmailToToFile(email);
                } else {
                    updateEmail(email);
                }
            } catch (IOException e) {
                System.err.println("Error while reading or writing in the file");
                e.printStackTrace();
            }
        });
    }

    /**
     * Rimpiazzo l'email vecchia con i nuovi attributi ottenuti
     * (Entrare in questo metodo significa che si sta rispondendo a una email già esistente)
     *
     * @param updatedEmail nuova email
     */
    protected synchronized void updateEmail(Email updatedEmail) {
        int index = -1;
        for (int i = 0; i < emailList.size(); i++) {
            if (emailList.get(i).getId().equals(updatedEmail.getId())) {
                index = i;
                break;
            }
        }
        // Se l'oggetto esiste già, sostituiscilo con quello nuovo
        if (index != -1) {
            emailList.set(index, updatedEmail);
        }

        // Riscrivo il file contente l'email con i nuovi dati
        String filePath = this.path + "\\" + this.name + updatedEmail.getId() + ".txt";
        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("Deleted file: " + filePath);
            writeEmailToFile(updatedEmail, filePath);
        } else {
            System.out.println("Failed to delete file: " + filePath);
        }

    }


    /**
     * Genera una stringa che indica la data e ora corrente
     *
     * @return la stringa generata
     */
    protected String dateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd'/'MM'/'yyyy 'at' HH':'mm");
        return java.time.LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(formatter);
    }


    /**
     * Usato per inviare un'email
     *
     * @param email email che devo inviare
     */
    public synchronized void sendObject(Email email) {
        try {
            out.flush();
            out.writeObject(email);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Verifica che 'id' non esista già
     *
     * @param id di tipo String
     * @return true se l'ID esiste già tra le email nella lista, false altrimenti
     */
    public boolean idExists(String id) {
        for (Email e : emailList) {
            if (e.getId().equals(id)) {
                return true;
            }
        }
        return false;
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
     * Legge il nome di tutti i clients connessi nel file clients.txt e verifica se uno di essi è uguale al parametro
     *
     * @param name nome da verificare se esiste già
     * @return true se esiste, false altrimenti
     */
    protected synchronized boolean readClientsFromFile(String name) {
        try (FileReader reader = new FileReader(allClientsPath);
             BufferedReader br = new BufferedReader(reader)) {
            //Non ho bisogno di chiudere reader e br, java lo fa in automatico

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(name)) {
                    return true;
                }
            }
        } catch (IOException e) {
            //Generata se non si riesce ad accedere al file indicato in path
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Scrive il nome del client connesso nel file clients.txt
     *
     * @param name il nome del client
     */
    protected synchronized void writeClientToFile(String name) {
        try (FileWriter writer = new FileWriter(allClientsPath, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

            bufferedWriter.write(name + "\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            //Generata se non si riesce ad accedere al file indicato in path
            e.printStackTrace();
        }
    }

    /**
     * Elimina il nome del client disconnesso dal file clients.txt
     *
     * @param name del client disconnesso
     */
    protected synchronized void deleteClientFromFile(String name) {

        File fileToUpdate = new File(allClientsPath);
        File tempFile = new File("temp.txt"); //crea un file temporaneo

        try (FileReader reader = new FileReader(fileToUpdate);
             BufferedReader br = new BufferedReader(reader);
             FileWriter writer = new FileWriter(tempFile);
             BufferedWriter bw = new BufferedWriter(writer);) {

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains(name)) {
                    bw.write(line + System.lineSeparator());
                }
            }

            br.close();
            bw.close();
            reader.close();
            writer.close();

            if (!fileToUpdate.delete()) { //elimino il file originale
                //Se non riesco a eliminare il file, genero un eccezione
                throw new RuntimeException("Impossibile eliminare il file originale");
            }
            if (!tempFile.renameTo(fileToUpdate)) { //sostituisco il file originale con temp.txt
                //Se non riesco a eliminare il file, genero un eccezione
                throw new RuntimeException("Impossibile rinominare il file temporaneo");
            }
        } catch (IOException e) {
            //Genero un eccezione in caso di scrittura o lettura
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifica che in una List esista una stringa
     *
     * @param r la lista sulla quale iterare
     * @return true se la stringa esiste nella lista, false altrimenti
     */

    protected boolean senderExists(List<String> r) {
        for (String s : r) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
