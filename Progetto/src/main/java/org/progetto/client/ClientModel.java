package org.progetto.client;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.progetto.email.Email;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientModel {
    private Socket socket = null;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String username;
    private ListProperty<Email> inbox = new SimpleListProperty<>();
    private ObservableList<Email> emailList = FXCollections.observableList(new LinkedList<>());
    private ExecutorService executor = Executors.newFixedThreadPool(1); //poolthread singolo che si occupa della connessione
    private int alreadyExist = 1;

    private IntegerProperty isConnectedProperty;

    /**
     * Crea il modello del Client
     *
     * @param user, indica il nome del client
     */
    public ClientModel(String user) throws IOException {
        isConnectedProperty = new SimpleIntegerProperty(0);
        connectToServer();
        username = user;
        this.inbox.set(emailList);

    }

    /**
     * Verifica che il server sia online
     *
     * @return true se il server è online, false altrimenti
     */
    protected boolean isServerReachable() {
        String serverAddress = "127.0.0.1";
        int serverPort = 9090;
        int timeout = 5000; // Timeout per il ping in millisecondi

        try {
            socket = new Socket();
            InetSocketAddress address = new InetSocketAddress(serverAddress, serverPort);
            socket.connect(address, timeout); //Verifico se il server è online
            return true; // Il server è raggiungibile
        } catch (ConnectException e) {
            return false;
        } catch (IOException e) {
            return false; // Il server non è raggiungibile
        }
    }

    /**
     * A ogni client è associato un thread che verifica che la connessione con il server sia attiva
     * In caso contrario, prova a riconettersi ogni tre secondi
     */
    protected void connectToServer() {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    //socket = new Socket("127.0.0.1", 9090);
                    if (isServerReachable()) {

                        out = new ObjectOutputStream(socket.getOutputStream());
                        in = new ObjectInputStream(socket.getInputStream());
                        sendMessage(username); //Invio il nome del client al server

                        alreadyExist = (Integer) in.readObject(); //Salvo la risposta del server sull'esistenza del client


                        if (alreadyExist == 0) { //Un client con lo stesso nome esiste
                            setIsConnectedProperty(1);
                        } else {
                            setIsConnectedProperty(2);
                            emailList.clear();
                            new Thread(new ClientModel.Handler(socket)).start();
                            System.out.println("Connection established!");
                        }
                        break;
                    } else {
                        if (isConnectedProperty.get() == 0) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        System.out.println("Failed to connect to server. Retrying in 3 seconds...");
                        setIsConnectedProperty(1);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException err) {
                            //Generata quando il thread viene interrotto mentre è in sleep
                            err.printStackTrace();
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    //In caso di errore I/O il thread "dorme" per tre secondi e riprova la connessione
                    //Può essere generato se il server non è raggiungibile o se ci sono problemi di rete

                    /*System.out.println("Failed to connect to server. Retrying in 3 seconds...");
                    setIsConnectedProperty(1);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException err) {
                        //Generata quando il thread viene interrotto mentre è in sleep
                        err.printStackTrace();
                    }*/
                }
            }
        });
    }


    public ListProperty<Email> getInbox() {
        return inbox;
    }

    public IntegerProperty isConnectedProperty() {
        return isConnectedProperty;
    }

    protected int getIsConnectedProperty() {
        return isConnectedProperty.get();
    }

    protected void setIsConnectedProperty(int isConnected) {
        Platform.runLater(() -> {
            isConnectedProperty.set(isConnected);
        });

    }

    /**
     * Invia l'oggetto tramite stream
     *
     * @param item
     */
    protected synchronized void sendMessage(Object item) {
        try {
            out.writeObject(item);
            out.flush();
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

    protected synchronized void deleteEmail(Email email) {
        Platform.runLater(() -> {
            emailList.remove(email);
        });
        email.setDelete(true);
        sendMessage(email);
    }

    protected synchronized void updateEmail(Email email) {
        int index = -1;
        for (int i = 0; i < emailList.size(); i++) {
            if (emailList.get(i).getId().equals(email.getId())) {
                index = i;
                break;
            }
        }
        // Se l'oggetto esiste già, sostituiscilo con quello nuovo
        if (index != -1) {
            emailList.set(index, email);
            System.out.println("Email successfully updated");
        } else {
            emailList.add(email);
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
         * Apre lo stream in entrata e si mette in loop in attesa di oggetti
         */
        @Override
        public void run() {
            try {
                while (true) {
                    Object received = in.readObject();

                    if (received instanceof Email) {

                        if (((Email) received).isNew() || ((Email) received).isForward()) {
                            emailList.add((Email) received);

                        } else if (((Email) received).isToUpdate()) {
                            updateEmail((Email) received);
                            ((Email) received).setUpdate(false);
                        }

                    } else if (received instanceof List<?>) {
                        emailList.addAll((List<Email>) received);
                    }

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

    protected boolean senderExists(List<String> r) {
        for (String s : r) {
            if (s.equals(username)) {
                return true;
            }
        }
        return false;
    }

}
