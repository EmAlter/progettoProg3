package org.progetto.client;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.progetto.email.Email;

import java.io.*;
import java.net.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ERR {

    private Socket socket = null;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String username;
    private final String path;
    private ListProperty<Email> inbox = new SimpleListProperty<>();
    private ObservableList<Email> emailList = FXCollections.observableList(new LinkedList<>());
    private ExecutorService executor = Executors.newFixedThreadPool(1); //poolthread singolo che si occupa della connessione

    private boolean isConnected = false;
    private ClientController controller;

    /**
     * Crea il modello del Client
     *
     * @param user, indica il nome del client
     */
    public ERR(String user, ClientController controller) throws IOException {
        this.controller = controller;
        connectToServer();

        username = user;
        this.path = "src\\main\\java\\org\\progetto\\allfiles\\" + user;
        this.inbox.set(emailList);

        if (!createNewDirectory()) {
            //Se la directory esiste significa che il client è già stato avviato una volta,
            //perciò carico tutte le email già ricevute precedentemente
            readAllEmailsFromFolder(this.path);
        } else {
            //Se il Client viene creato per la prima volta
            //vengono create email casuali da aggiungere
            List<String> r = new ArrayList<>();
            r.add(username);
            addEmailToObservableListAndToFile(new Email(uniqueID(), "artoria@gmail.com", r,
                    "Hello!", "We need to discuss the car price", dateTime()));
            addEmailToObservableListAndToFile(new Email(uniqueID(), "irelia@gmail.com", r,
                    "Do not forget!", "Let's meet up at the same place", dateTime()));
        }
    }

    /**
     * A ogni client è associato un thread che verifica che la connessione con il server sia attiva
     * In caso contrario, prova a riconettersi ogni cinque secondi
     *
     */
    protected void connectToServer() {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = new Socket("127.0.0.1", 9090);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    System.out.println("Connection established!");
                    out.writeObject(username); //Invio il nome del client al server
                    out.flush();
                    new Thread(new Handler(socket)).start();
                    isConnected = true;
                    controller.showConnection(true);
                    break;
                } catch (IOException e) {
                    //In caso di errore I/O il thread "dorme" per cinque secondi e riprova la connessione
                    //Può essere generato se il server non è raggiungibile o se ci sono problemi di rete
                    System.out.println("Failed to connect to server. Retrying in 5 seconds...");
                    isConnected = false;
                    controller.showConnection(false);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException err) {
                        //Generata quando il thread viene interrotto mentre è in sleep
                        err.printStackTrace();
                    }


                }
            }
        });
    }

    protected boolean getIsConnected() {
        return this.isConnected;
    }

    public ListProperty<Email> getInbox() {
        return inbox;
    }

    /**
     * Creo una nuova directory dei file associata al client, se e solo se non esiste già
     *
     * @return true se la directory esiste, false altrimenti
     */
    public boolean createNewDirectory() {
        return new File(this.path).mkdirs();
    }

    /**
     * Scrivo tutte le informazioni dell'oggetto email in un file di testo .txt
     *
     * @param email da scrivere
     * @param path  percorso dove scrivere la email
     */
    public void writeEmailToFile(Email email, String path) {
        try {
            FileWriter fileWriter = new FileWriter(path, true);
            fileWriter.write(email.toString());
            fileWriter.flush();
            fileWriter.close();
            System.out.println("The Email was successfully written to a file");

        } catch (Exception err) {
            err.printStackTrace(); //In questo caso gli errori possibili sono molti, di conseguenza mostro lo stacktrace
        }
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
     * Il metodo permette l'eliminazione dell'email in entrata sia dalla lista che dai file
     * Riavviando il client, le email eliminate non saranno più presenti
     *
     * @param email email da eliminare
     */
    public void deleteEmail(Email email) {
        //Elimina l'email selezionata dalla lista
        emailList.remove(email);

        //Ottiene il path del file e lo elimina basandosi sull'ID
        String filePath = this.path + "\\" + this.username + email.getId() + ".txt";
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
                    addEmailToObservableListAndToFile(email);
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
     * Aggiungo l'email alla ObservableList e creo un nuovo file contenente gli attributi dell'email
     *
     * @param email
     * @throws IOException
     */
    private void addEmailToObservableListAndToFile(Email email) throws IOException {
        emailList.add(email);
        File f = new File(this.path + "\\" + this.username + email.getId() + ".txt");
        f.createNewFile();
        writeEmailToFile(email, this.path + "\\" + this.username + email.getId() + ".txt");
    }

    /**
     * Rimpiazzo l'email vecchia con in nuovi attributi ottenuti
     * (Entrare in questo metodo significa che si sta rispondendo a una email già esistente)
     *
     * @param updatedEmail
     */
    protected void updateEmail(Email updatedEmail) {
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
        String filePath = this.path + "\\" + this.username + index + ".txt";
        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("Deleted file: " + filePath);
            writeEmailToFile(updatedEmail, filePath);
        } else {
            System.out.println("Failed to delete file: " + filePath);
        }

    }

    /**
     * Serve per generare un ID unico per ogni email nuova
     *
     * @return l'ID unico generato
     */
    protected String uniqueID() {
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
    private boolean idExists(String id) {
        for (Email e : emailList) {
            if (e.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Invia l'email tramite stream
     *
     * @param email
     */
    protected synchronized void sendMessage(Object item) {
        try {
            out.flush();
            out.writeObject(item);
        } catch (IOException e) {
            System.out.println("The Client is not connected to the server!");
            connectToServer(); //Se si genera un eccezione I/O, provo a riconnettermi al server
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
     * Chiude i socket e la connessione
     */
    protected void closeConnection() {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing client");
            e.printStackTrace();
        }
    }

    /**
     * Sottoclasse che si occupa della connessione con il server tramite thread
     */
    class Handler implements Runnable {

        private Socket incoming = null;

        public Handler(Socket incoming) {
            this.incoming = incoming;
        }

        /**
         * Apre lo stream in entrata e si mette in loop in attesa di oggetti email
         */
        @Override
        public void run() {
            try {
                in = new ObjectInputStream(incoming.getInputStream());
                while (true) {
                    Email msgReceived = (Email) in.readObject();
                    //add(msgReceived);
                    emailList.add(msgReceived);
                }

            } catch (SocketException e) {
                //Il server è andato offline
                closeConnection();
                connectToServer(); // Provo a riconnetermi finchè il server non torna online
            } catch (IOException | ClassNotFoundException e) {
                //Errore di tipo I/O oppure classe non trovata
                connectToServer();
            } finally {
                closeConnection();
            }
        }

    }
}
