package org.progetto.server;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.progetto.email.Email;

import java.io.*;
import java.net.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerModel implements Runnable {

    private ServerSocket ss;
    private Socket socket;
    private ExecutorService pool;
    private final int NTHREADS = 10; //Il numero massimo possibile di client attivi è 10
    private final ListProperty<String> logList = new SimpleListProperty<>();
    private final ObservableList<String> log = FXCollections.observableList(new LinkedList<>());
    private List<ClientInfoIO> allClients;

    public ServerModel() throws IOException {
        this.logList.set(log);
        log.add("[Server] Server invoked");
        allClients = new ArrayList<>();
    }

    /**
     * Inizializza il nuovo server sulla porta 9090 e si mette in attesa dei client
     */
    @Override
    public void run() {
        try {
            ss = new ServerSocket(9090);
            log.add("[Server] Server started on port 9090");
            pool = Executors.newFixedThreadPool(NTHREADS);

            while (true) {
                socket = ss.accept();
                // Si mette in attesa di altre richieste dai client
                IOMessages handler = new IOMessages(socket);
                pool.execute(handler);
            }

        } catch (SocketException e) {
            //Generata quando viene chiuso il server
            System.out.println("[Server] Server offline ");
            e.printStackTrace();
            //closeServer();
        } catch (Exception e) {
            //Indica le altre possibili eccezioni che possono essere generate
            e.printStackTrace();

        } finally {
            closeServer();
        }
    }

    /**
     * Usato per inviare le email ai client connessi
     * Verifica il nome destinatario dell'email ricevuta e itera la lista contenente i client connessi
     * Se trova un nome uguale nella lista, gli invia l'email
     * Se il nome del destinatario letto non è connesso, mostro l'errore nel log
     *
     * @param email email in entrata
     * @throws IOException in caso di errore I/O
     */
    private synchronized void broadcast(Email email) throws IOException {
        List<String> toEliminate = new ArrayList<>(email.getDest());
        for (ClientInfoIO ci : allClients) { // Itero tutti i client connessi
            for (String dest : email.getDest()) { // Itero i nomi dei destinatari dell'oggetto Email
                if (dest.equals(ci.getName())) {
                    ci.sendObject(email); //Invio
                    ci.add(email); //Salvo l'email in un file
                    showNewLogMessage("[" + email.getMitt() + "]" + " Email sent " + time());
                    toEliminate.remove(dest);
                }
            }
        }

        if (!toEliminate.isEmpty()) { // Se ci sono destinatari nell'oggetto Email che non sono connessi, mostro errore nel log
            for (String te : toEliminate) {
                showNewLogMessage("[Server] Unable to send to " + te + " because does not exists");
            }
        }
    }


    public ListProperty<String> getLogList() {
        return logList;
    }

    /**
     * Sottoclasse usata per gestire le connessioni dei client
     */
    class IOMessages implements Runnable {
        private final Socket incoming;
        private ObjectInputStream in = null;
        private ObjectOutputStream out = null;
        private List<Email> emailList = new ArrayList<>();

        private String clientName = "";
        private ClientInfoIO client = null;


        public IOMessages(Socket incoming) {
            this.incoming = incoming;
        }

        /**
         * Genera per ogni client che si connette gli streams di entrata e di uscita
         * Ogni client appena connesso invia il proprio nome, lo salva in una variabile e imposta il nuovo nome del thread
         */
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(this.incoming.getOutputStream());
                in = new ObjectInputStream(this.incoming.getInputStream());

                clientName = (String) in.readObject(); // Appena un client si connette invia il suo nome
                client = new ClientInfoIO(clientName, out, emailList);
                // Verifica se il client esiste già nel file
                if (client.readClientsFromFile(clientName)) {
                    out.writeObject(0); // Invia un flag che indica che il client esiste già
                    return;
                } else {
                    out.writeObject(1); //flag che indica che non esistono client con lo stesso nome aperti
                    out.flush();
                    out.writeObject(client.getEmailList());
                    out.flush();
                    addClient(client); //Aggiungo il client alla lista dei client connessi
                    client.writeClientToFile(clientName); //Aggiunge il nome del client al file
                    Thread.currentThread().setName(clientName);
                }

                showNewLogMessage("[Server] New Client Connected with name: " + clientName + " at " + time());

                while (!Thread.currentThread().isInterrupted()) { // Loop finchè il thread è attivo
                    Object received = in.readObject(); // Nuovo messaggio ricevuto
                    if (received instanceof Email) {

                        if(((Email) received).isNew() || ((Email) received).isForward()) {
                            ((Email) received).setId(client.uniqueID());
                            broadcast((Email) received);
                        }
                        else if(((Email) received).isToDelete()) {
                            client.deleteEmail((Email) received);
                            showNewLogMessage("[" +clientName + "]" + " Email deleted! " + time());
                        }
                        else {
                            broadcast((Email) received);
                        }

                    }
                }
            } catch (SocketException | EOFException e) {
                //Client disconnesso
                showNewLogMessage("[Server] Client " + Thread.currentThread().getName() + " disconnected");
                if(client != null) {
                    client.deleteClientFromFile(clientName);
                }
            } catch (IOException | ClassNotFoundException e) {
                //In caso di errore di scrittura o lettura
                e.printStackTrace();
                showNewLogMessage("[" + Thread.currentThread().getName() + "]" + " Error on sending message " + time());

            } finally {
                //Chiudo la connessione del client e rimuovo il client dalla lista dei clients attivi
                closeConnection(out, in, this.incoming);
                removeClient(Thread.currentThread().getName());
            }
        }
    }

    /**
     * Usato per aggiungere il nuovo client connesso alla lista dei clients attivi
     *
     * @param client client connesso
     */
    private synchronized void addClient(ClientInfoIO client) {
        allClients.add(client);
    }

    /**
     * Usato per rimuovere un client che si è disconnesso dalla lista dei clients attivi
     *
     * @param name lo rimuovo in base al nome
     */
    private synchronized void removeClient(String name) {
        for (ClientInfoIO ci : allClients) {
            if (ci.getName().equals(name)) {
                allClients.remove(ci);
                break;
            }
        }
    }

    /**
     * Aggiungo un nuovo messaggio alla ListView che mostra il log
     *
     * @param s messaggio da mostrare
     */
    protected void showNewLogMessage(String s) {
        Platform.runLater(() -> log.add(s));
    }

    /**
     * Usata per calcolare la data e l'ora attuale fino ai secondi
     *
     * @return data e ora attuale di tipo String
     */
    protected String time() { //Ritorna l'orario attuale fino ai secondi
        return java.time.LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    public void deleteAllFileContent() {
        try (FileWriter fileWriter = new FileWriter("src\\main\\java\\org\\progetto\\allfiles\\clients.txt")) {
            fileWriter.write("");  // Scrive una stringa vuota nel file
            System.out.println("[Server] Contents of client.txt deleted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chiude la connessione di un client connesso
     *
     * @param o oggetto di output
     * @param i oggetto di input
     * @param s socket
     */
    protected void closeConnection(ObjectOutputStream o, ObjectInputStream i, Socket s) {
        try {
            o.close();
            i.close();
            s.close();
        } catch (IOException e) {
            //Errore in fase di chiusura della connessione con il client
            System.out.println("Error closing Connection: " + e.getMessage());
        }
    }

    /**
     * Chiude il server, chiudendo sia la threadpool che il socket
     */
    protected void closeServer() {
        try {
            pool.shutdown();
            ss.close();
        } catch (IOException e) {
            //Errore in fase di chiusura del Server
            System.out.println("Error closing Server: " + e.getMessage());
        }
    }

}

