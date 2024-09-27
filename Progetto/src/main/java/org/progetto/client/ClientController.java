package org.progetto.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.progetto.email.Email;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientController {

    @FXML
    private Label username;
    @FXML
    private Label IDlbl;
    @FXML
    private Label showError;
    @FXML
    private Label lblConnection;
    @FXML
    private Circle circleConnection;
    @FXML
    private ListView<Email> msgList;
    @FXML
    private Label msgFrom;
    @FXML
    private Label msgTo;
    @FXML
    private Label msgArgument;
    @FXML
    private Label showErrorNewMsg;
    @FXML
    private TextArea msgTxt;
    @FXML
    private TextField newmsgTo;
    @FXML
    private TextField newmsgArgument;
    @FXML
    private TextArea newmsgTxt;
    @FXML
    private Button deleteButton;
    @FXML
    private Button allReply;
    @FXML
    private Button singleReply;
    @FXML
    private Button newmsgButton;
    @FXML
    private Button forwardButton;
    @FXML
    private TabPane mainPane;
    @FXML
    private AnchorPane loginPane;
    @FXML
    private TextField usr;
    @FXML
    private Label errorLabel;
    @FXML
    private TextArea replyText;

    private ClientModel model;
    private Email selectedEmail;
    private Email emptyEmail;
    private int isConnected = 0;

    /**
     * Inizializzo il controller
     *
     * @param stage indica lo stage sulla quale inizializzare il controller
     */
    @FXML
    public void initialize(Stage stage) {
        mainPane.setVisible(false);
        closeConnectionThroughX(stage);
    }

    /*
    Se chiudo la finestra del client con la 'X' interrompo tutto

          @param stage indica lo stage del client corrente
         */
    public void closeConnectionThroughX(Stage stage) {
        stage.setOnCloseRequest(event -> {
            try {
                Platform.exit();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Quando clicco il bottone Enter verifico diverse cose:
     * - se il nome con la quale voglio creare il client esiste già
     * - se il nome rispetta il pattern giusto
     * Se il pattern è giusto e il nome non esiste già, allora creo il client
     */
    @FXML
    public void onEnterButtonClick() {

        if (checkEmail(usr.getText())) {
            try {
                model = new ClientModel(usr.getText());
                model.isConnectedProperty().addListener((observable, oldValue, newValue) -> {
                    isConnected = (int) newValue;
                    handleChange();
                });

                if(model.getIsConnectedProperty() == isConnected) {
                    handleChange();
                }

            } catch (IOException e) {
                //Viene generata se ci sono errori in fase di creazione del model
                e.printStackTrace();
            }
            username.setText(usr.getText());
        } else {
            errorLabel.setText("The pattern must be: myname@domain.com (no uppercase letters)");

        }
    }

    private void handleChange() {
        if (isConnected == 2) {
            if (mainPane.isVisible()) {
                showConnection(true);
            } else {
                selectedEmail = null;
                msgList.itemsProperty().bind(model.getInbox()); //binding della lista delle email
                view();
                msgList.setOnMouseClicked(this::clickEmailFromList);
                emptyEmail = new Email("0000", "", new ArrayList<>(), "", "", "");
                showSelectedEmail(emptyEmail);
                msgTxt.setEditable(false);
                replyText.setPromptText("Enter your reply message or the receivers you want to forward here");
                loginPane.setVisible(false);
                mainPane.setVisible(true);
                showConnection(true);
            }
        } else if (isConnected == 1) {
            if (!mainPane.isVisible()) {
                errorLabel.setText("Client with same name already connected");
                //model.closeConnection();
            } else {
                showConnection(false);
            }
        } else {
            errorLabel.setText("SERVER NOT ONLINE");
        }
    }


    /**
     * Verifica che la stringa rispetti il pattern myname@domain.com
     *
     * @param username la stringa che indica il nome ottenuta per creare un nuovo client
     * @return true se la stringa rispetta il pattern
     */
    protected boolean checkEmail(String username) {
        Pattern pattern = Pattern.compile("^[a-z0-9]+(\\.[a-z0-9]+)*@[a-z0-9.-]+\\.[a-z]{2,}$");
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    /**
     * Imposta lblConnection e circleConnection dipendentemente dalla connessione del client
     *
     * @param conn true se il client è connesso, false altrimenti
     */
    protected void showConnection(boolean conn) {
        if (conn) {
            circleConnection.setFill(Color.GREEN);
            Platform.runLater(() -> {
                lblConnection.setText("CONNECTED");
                setErrorMessage("", showError);
                setErrorMessage("", showErrorNewMsg);
            });
        } else {
            circleConnection.setFill(Color.RED);
            Platform.runLater(() -> {
                lblConnection.setText("NOT CONNECTED");
            });
        }
    }

    /**
     * Verifica che il testo inserito non sia vuoto o contenente il carattere &
     *
     * @param s stringa da verificare
     * @return true se la stringa è vuota o se contiene &
     */
    private boolean checkCharacter(String s) {
        return s.contains("&") || s.isEmpty();
    }

    /**
     * Aggiorna la ListView ogni volta che un elemento viene tolto o aggiunto
     * Se la email non è mai stata "aperta", il testo nella ListView è in grassetto
     */
    protected void view() {
        //Mostra nella ListView l'oggetto dell'email
        msgList.setCellFactory(listView -> new ListCell<>() {
            @Override
            public void updateItem(Email email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) {
                    Platform.runLater(() -> {
                        setText(null);
                        setStyle("");
                    });
                } else {
                    Platform.runLater(() -> {
                        setText(email.getArgument()); //testo che viene mostrato per ogni elemento della lista
                        if (email.isNew()) {
                            setStyle("-fx-font-weight: bold"); //imposto il testo della listview in grassetto
                        } else {
                            setStyle("");
                        }
                    });
                }
            }
        });
    }

    /**
     * Quando clicco il bottone New Message nella view
     * Verifico che i campi destinatario, oggetto e testo non siano vuoti, altrimenti mostro errore
     * Verifico che il campo destinatario contenga una stringa valida
     */
    @FXML
    public void onNewMessageButtonClick() {
        showErrorNewMsg.setText("");

        if (model.getIsConnectedProperty() != 2) {
            setErrorMessage("CLIENT IS NOT CONNECTED", showErrorNewMsg);
            return;
        }

        if (!checkCharacter(newmsgTxt.getText()) && !checkCharacter(newmsgArgument.getText()) && !checkCharacter(newmsgTo.getText())) {
            List<String> receivers = new ArrayList<>(splitReceivers(newmsgTo.getText()));
            if (iterateListForPattern(receivers)) {
                Email email = new Email("", username.getText(), receivers, newmsgArgument.getText(), newmsgTxt.getText(), model.dateTime());
                model.sendMessage(email);
                receivers.clear();

                newmsgTo.clear();
                newmsgArgument.clear();
                newmsgTxt.clear();
            } else {
                setErrorMessage("Typing error on writing the receivers.", showErrorNewMsg);
            }
        } else {
            setErrorMessage("Receivers, Argument and Text fields cannot be empty or contain the '&' character", showErrorNewMsg);
        }
    }

    /**
     * Quando clicco il bottone Reply/All nella view
     * Rispondo alla email ricevuta e la invio al mittente e a tutti i destinatari
     */
    @FXML
    public void onReplyAllButtonClick() {
        Email emailToReplyAll = selectedEmail;

        if (model.getIsConnectedProperty() != 2) {
            setErrorMessage("CLIENT IS NOT CONNECTED", showError);
            return;
        }

        if (emailToReplyAll == null) { //se clicco il bottone senza che nessuna email sia selezionata nella view
            setErrorMessage("Select an email to reply to", showError);
            return;
        }

        if(emailToReplyAll.isForward()) {
            setErrorMessage("You cannot reply to a forwarded email", showError);
            return;
        }
        emailToReplyAll.setNew(false);
        emailToReplyAll.setUpdate(true);
        showError.setText("");

        List<String> allReceivers = new ArrayList<>(splitReceivers(msgTo.getText()));
        allReceivers.add(emailToReplyAll.getMitt());

        if (!model.senderExists(allReceivers)) { //se il mittente esiste già tra i destinatari, non serve aggiungerlo
            allReceivers.add(username.getText());
        }
        if (!checkCharacter(replyText.getText())) {
            String newContent = "\n" + "--------------------------------------" + "\n"
                    + model.dateTime() + "\n"
                    + username.getText() + " says:" + "\n"
                    + replyText.getText();


            emailToReplyAll.setMitt(username.getText()); //modifica il mittente
            emailToReplyAll.setDest(allReceivers); //modifica i destinatari
            emailToReplyAll.setTxt(emailToReplyAll.getTxt() + newContent); //modifica il testo

            emailToReplyAll.setUpdate(true);
            model.sendMessage(emailToReplyAll);
            replyText.setText("");
            showSelectedEmail(emailToReplyAll);
        } else {
            setErrorMessage("Reply text cannot be empty or contains the '&' character", showError);
        }
        allReceivers.clear();
    }


    /**
     * Quando clicco il bottone Reply nella view
     * Rispondo alla email ricevuta e la invio SOLO al mittente
     */

    @FXML
    protected void onSingleReplyButtonClick() {
        Email emailToReply = selectedEmail;

        if (model.getIsConnectedProperty() != 2) {
            setErrorMessage("CLIENT IS NOT CONNECTED", showError);
            return;
        }

        if (emailToReply == null) {
            setErrorMessage("Select an email to reply to", showError);
            return;
        }

        if (emailToReply.getMitt().equals(username.getText())) {
            setErrorMessage("You cannot reply to an email you sent!", showError);
            return;
        }

        if (checkCharacter(replyText.getText())) {
            setErrorMessage("Reply text cannot be empty or contain the '&' character", showError);
            return;
        }

        if(emailToReply.isForward()) {
            setErrorMessage("You cannot reply to a forwarded email", showError);
            return;
        }

        showError.setText("");


        String newContent = "\n" + "--------------------------------------" + "\n"
                + model.dateTime() + "\n"
                + username.getText() + " says:" + "\n"
                + replyText.getText();

        List<String> receiver = new ArrayList<>(splitReceivers(msgTo.getText()));

        if(!model.senderExists(receiver)) {
            receiver.add(username.getText());
        }

        if (receiver.size() > 2) {
            receiver.clear();
            String newArgument = "REPLY: " + emailToReply.getArgument();
            receiver.add(emailToReply.getMitt());
            receiver.add(username.getText());
            Email newReply = new Email("000", username.getText(), receiver, newArgument, emailToReply.getTxt() + newContent, model.dateTime());
            model.sendMessage(newReply);
            replyText.setText("");
            showSelectedEmail(emailToReply);
            return;
        }

        receiver.clear();
        receiver.add(emailToReply.getMitt());
        receiver.add(username.getText());


        emailToReply.setMitt(username.getText());
        emailToReply.getDest().clear();
        emailToReply.setDest(receiver);

        emailToReply.setTxt(emailToReply.getTxt() + newContent);
        emailToReply.setUpdate(true);
        model.sendMessage(emailToReply);

        replyText.setText("");
        //showSelectedEmail(emailToReply);

        receiver.clear();
    }

    /**
     * Quando clicco il bottone Forward nella view
     * La email di cui fare forward e quella che sto visualizzando nella view
     * Ottengo il nome dei destinatari dalla TextArea replyText, se scrivo testo non valido mostra errore
     * Modifico alcuni attributi
     */
    @FXML
    protected void onForwardButtonClick() {
        Email emailToForward = selectedEmail;

        if (model.getIsConnectedProperty() != 2) {
            setErrorMessage("CLIENT IS NOT CONNECTED", showError);
            return;
        }
        if (emailToForward == null) { //se clicco il bottone senza che nessuna email sia selezionata nella view
            setErrorMessage("Select an email to forward", showError);
            return;
        }

        showError.setText("");

        String text = "This is the email originally sent from: " + emailToForward.getMitt()
                + " to " + username.getText() + "\n"
                + " in date " + emailToForward.getDate() + "\n"
                + "[..." + "\n"
                + emailToForward.getTxt() + "\n"
                + "...]";

        //Leggo tutti i destinatari dal campo replyText e li salvo a uno a uno in una lista
        List<String> receiversToSend = new ArrayList<>(splitReceivers(replyText.getText()));

        if (iterateListForPattern(receiversToSend)) {
            emailToForward.setMitt(username.getText()); //modifica il mittente
            emailToForward.setDest(receiversToSend); //modifica i destinatari
            if (!emailToForward.isForward()) {
                emailToForward.setArgument("Forwarded: " + emailToForward.getArgument()); //modifica l'oggetto dell'email
                emailToForward.setTxt(text); //modifica il testo dell'email
            }

            emailToForward.setForward(true);
            emailToForward.setNew(true);  //sto inviando una nuova email

            model.sendMessage(emailToForward);
            replyText.setText("");
        } else {
            setErrorMessage("Typing error on writing the senders.", showError);
        }
    }

    /**
     * Itera un List per verificare che vengano rispettate delle specifiche
     *
     * @param l la lista da iterare
     * @return true se tutti gli elementi rispettano le specifiche, false se un solo elemento non le rispetta
     */
    private boolean iterateListForPattern(List<String> l) {
        for (String s : l) {
            if (!checkEmail(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Divide ogni elemento della stringa e lo assegna a una List
     *
     * @param r la stringa che deve essere divisa
     * @return ritorna la List contenente tutti gli elementi
     */
    private List<String> splitReceivers(String r) {
        List<String> dest = new ArrayList<>();
        String[] parts = r.split(";"); //ogni elemento è diviso da ";"
        for (String part : parts) {
            dest.add(part);
        }
        return dest;
    }

    /**
     * Verifica che in una List esista una stringa
     *
     * @param r la lista sulla quale iterare
     * @return true se la stringa esiste nella lista, false altrimenti
     */
    private boolean senderExists(List<String> r) {
        for (String s : r) {
            if (s.equals(msgFrom.getText())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evento generato quando clicco un oggetto della ListView
     *
     * @param mouseEvent generato dal click del mouse
     */
    protected void clickEmailFromList(MouseEvent mouseEvent) {
        selectedEmail = msgList.getSelectionModel().getSelectedItem();
        showSelectedEmail(selectedEmail);
    }

    /**
     * Mostra gli attributi dell'email nella view
     *
     * @param email email selezionata dalla view
     */
    protected void showSelectedEmail(Email email) {
        //Serve per non dare errore nel caso la lista sia vuota
        if (selectedEmail != null) {
            //l'ID settato a 0000 serve solo come flag per indicare che si tratta di un'email vuota
            if (email.getId().equals("0000")) {
                IDlbl.setText("");
            } else {
                IDlbl.setText(String.valueOf(email.getId()));
            }
            msgFrom.setText(email.getMitt());
            msgTo.setText(String.join(";", email.getDest()));
            msgArgument.setText(email.getArgument());
            msgTxt.setText(email.getDate() + "\n" + email.getTxt() + "\n");
            email.setNew(false);
        }
    }

    /**
     * Quando clicco il bottone Delete nella view
     * Richiama il metodo nel model che elimina l'oggetto email
     * Eliminato l'oggetto mostra una email vuota
     * Se il bottone viene cliccato senza selezionare una email dalla view scrive errore
     */
    @FXML
    protected void deleteSelectedEmail() {
        if (selectedEmail != null) {
            model.deleteEmail(selectedEmail);
            showSelectedEmail(emptyEmail);
        } else {
            setErrorMessage("Select and email to delete", showError);
        }
    }

    /**
     * Metodo che imposta il testo del Label showError che mostra gli errori
     *
     * @param text  testo dell'errore da mostrare
     * @param label Label nella quale scrivere il messaggio
     */
    private void setErrorMessage(String text, Label label) {
        label.setText(text);
    }

}
