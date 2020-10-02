package sample;

import com.jfoenix.controls.*;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

public class FTP implements Initializable {
    public Label username;
    public Label host;
    public Label port;
    public JFXSnackbar jfxSnackbar;
    public AnchorPane anchorPane1;
    public JFXTreeView jfxTreeView;
    public Pane pane;
    public Label chemin;
    public JFXListView jfxListeView;

    private Socket socketFtp, dataSocket;
    private BufferedInputStream bufferedInputStreamData = null;
    private BufferedWriter bufferedWriterData = null;

    private  String dataIP;
    private  int dataPort;
    private Text toast;
    private File file;

    ObservableList<String > list = FXCollections.observableArrayList() ;


    public  void fonction(Socket socket, String loginUserName, String loginHost, String loginPort) throws IOException, InterruptedException {
        this.socketFtp = socket ;
        username.setText(loginUserName);
        host.setText(loginHost);
        port.setText(loginPort);
        toast = new Text("     Connexion réussie     " );
        toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                "     -fx-font-size: 12;\n" +
                "     -fx-font-weight: bold;");
        jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
        appliquerChangement();
    }

    private void appliquerChangement() throws InterruptedException, IOException {
        jfxListeView.getItems().clear();
        send("TYPE ASCII");
        readd() ;
        créationSocket();
        send("NLST");
        String listeFichier = readData() ;
        dataSocket.close();
        bufferedInputStreamData.close();
        bufferedWriterData.close();
        Thread.sleep(100);
        readd() ;
        Thread.sleep(500);
        StringTokenizer files = new StringTokenizer(listeFichier ,"\n") ;
        while (files.hasMoreTokens()) {
            String nomFichier = files.nextToken() ;
            list.add(nomFichier) ;
            jfxListeView.setItems(list);
            jfxListeView.setCellFactory(param -> new Element(){
            });
        }
        Thread.sleep(100);
        voirChemin(listeFichier);
    }

    private void voirChemin(String listeFichier) throws IOException {
        send("PWD");
        String réponse = readd() ;
        int debut = réponse.indexOf((char)34);
        int fin = réponse.indexOf((char)34,debut+1);
        String var = réponse.substring(debut+1,fin);

        Text icon = GlyphsDude.createIcon(FontAwesomeIconName.FOLDER_OPEN,"1em");
        icon.setFill(Color.valueOf("#ffb200"));
        chemin.setText(var + "  ");
        String lastToken = var ;
        if (var.length()!=1){
            StringTokenizer buffer = new StringTokenizer(var ,"/") ;
            while (buffer.hasMoreTokens()){
                lastToken = buffer.nextToken() ;
            }
        }
        TreeItem root = new TreeItem(lastToken);
        root.setGraphic(icon);

        StringTokenizer files = new StringTokenizer(listeFichier ,"\n") ;
        while (files.hasMoreTokens()){
            String nomFichier = files.nextToken() ;
            if (!nomFichier.contains(".")){
                Text label = GlyphsDude.createIcon(FontAwesomeIconName.FOLDER,"1em");
                label.setFill(Color.valueOf("#ffb200"));
                TreeItem treeItem = new TreeItem(nomFichier.trim()) ;
                treeItem.setGraphic(label);
                root.getChildren().add(treeItem) ;
            }
        }
        root.setExpanded(true);
        jfxTreeView.setRoot(root);
        VBox vbox = new VBox(jfxTreeView);

        vbox.setPrefSize(159,458);
        vbox.setLayoutY(1.0);
        pane.getChildren().add(vbox) ;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void TélechargerUnFichier() throws IOException, InterruptedException {
        TextInputDialog textInputDialog = new TextInputDialog("le chemin de fichier");
        textInputDialog.setTitle("Télecharger un fichier");

        Pane pane = new Pane() ;
        pane.setPrefSize(403,67);

        TextField Fichier = textInputDialog.getEditor() ;
        Fichier.setStyle("-jfx-unfocus-color: #004d40; -jfx-focus-color: #004d40;");
        Fichier.setPrefHeight(25);
        Fichier.setPrefWidth(195);
        Fichier.setLayoutX(105);
        Fichier.setLayoutY(14);

        JFXButton jfxButton = new JFXButton("Parcourir..") ;
        jfxButton.setTextFill(Paint.valueOf("#004d40"));
        jfxButton.setButtonType(JFXButton.ButtonType.RAISED);
        jfxButton.setStyle("-fx-border-color: #004d40; -fx-border-radius: 20px; -fx-background-radius: 20px;" +
                " -fx-font-size: 12; -fx-font-weight: bold;");
        jfxButton.setPrefHeight(25);
        jfxButton.setLayoutX(317);
        jfxButton.setLayoutY(14);

        Label label = new Label("Emplacement :");
        label.setStyle("-fx-fill: #004d40; -fx-font-size: 12; -fx-font-weight: bold;");
        label.setLayoutX(14);
        label.setLayoutY(19);

        pane.getChildren().addAll(label, Fichier, jfxButton) ;
        textInputDialog.setHeaderText(null);
        textInputDialog.getDialogPane().setContent(pane);

        jfxButton.setOnMouseClicked(event ->
        {
            FileChooser fileChooser = new FileChooser() ;
            file = fileChooser.showOpenDialog(anchorPane1.getScene().getWindow()) ;
            Fichier.setText(file.getAbsolutePath());

        });

        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent()) {
            send("TYPE ASCII");
            readd() ;
            créationSocket();
            OutputStream outputStream = dataSocket.getOutputStream();
            send("STOR "+file.getName());
            readd() ;
            byte[] bytes = new byte[16 * 1024];
            InputStream in = new FileInputStream(file);
            int count;
            while ((count = in.read(bytes)) > 0)
                outputStream.write(bytes, 0, count);
            dataSocket.close();
            outputStream.close();
            if (readd().startsWith("226")) {
                toast = new Text("     fichier téléchargé avec succès     " );
                toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                        "     -fx-font-size: 12;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
            }
        }
        appliquerChangement();
    }

    public void AjouterUnNouveauFicher() throws IOException, InterruptedException {
        TextInputDialog textInputDialog = new TextInputDialog("Nouveau Fichier");
        textInputDialog.setTitle("Ajouter un nouveau fichier");
        textInputDialog.setHeaderText(null);

        TextField nomFichier = textInputDialog.getEditor() ;

        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent()){
            créationSocket();
            send("STOR "+nomFichier.getText()+".txt");
            if (readd().startsWith("150")) {
                toast = new Text("     Fichier créé avec succès     " );
                toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                        "     -fx-font-size: 12;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
            }
            dataSocket.close();
            bufferedInputStreamData.close();
            bufferedWriterData.close();
            readd() ;
        }
        appliquerChangement();
    }

    public void AjouterUnNouveauDossier() throws IOException, InterruptedException {
        TextInputDialog textInputDialog = new TextInputDialog("Nouveau Dossier");
        textInputDialog.setTitle("Ajouter un nouveau Dossier");
        textInputDialog.setHeaderText(null);

        TextField nomDossier = textInputDialog.getEditor() ;

        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent()) {
            send("MKD " + nomDossier.getText());
            if (readd().startsWith("257") ) {
                toast = new Text("     Dossier créé avec succès     " );
                toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                        "     -fx-font-size: 12;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
            }
        }
        appliquerChangement();
    }

    public void Déconnexion() throws IOException {
        send("QUIT");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../sample/login.fxml"));
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle("Client FTP");
        stage.setScene(new Scene(loader.load()));
        stage.show();
        Platform.exit();

    }

    public void créationSocket() throws IOException {
        send("PASV");
        String reponse = readd() ;
        String ip;
        int port, debut = reponse.indexOf('('), fin = reponse.indexOf(')', debut + 1);
        if (debut > 0) {
            String dataLink = reponse.substring(debut + 1, fin);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
            ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "."
                    + tokenizer.nextToken() + "." + tokenizer.nextToken();
            port = Integer.parseInt(tokenizer.nextToken()) * 256
                    + Integer.parseInt(tokenizer.nextToken());
            dataIP = ip;
            dataPort = port;
        }
        dataSocket = new Socket(dataIP,dataPort) ;
        bufferedWriterData = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
        bufferedInputStreamData = new BufferedInputStream(dataSocket.getInputStream());
    }

    private String readd() throws IOException{
        String respons;
        int stream;
        byte[] b = new byte[4096];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socketFtp.getInputStream());
        stream = bufferedInputStream.read(b);
        respons = new String(b, 0, stream);
        return respons;
    }

    private String readData() throws IOException {
        String response = "";
        byte[] b = new byte[1024];
        int stream;
        while((stream = bufferedInputStreamData.read(b)) != -1) {
            response  = new String(b, 0, stream);
        }
        return response;
    }

    private void send(String command) throws IOException {
        command += "\r\n";
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socketFtp.getOutputStream()));
        bufferedWriter.write(command);
        bufferedWriter.flush();
    }

    public void dossierSélectionné() throws IOException, InterruptedException {
        if (jfxTreeView.getSelectionModel().getSelectedItem() !=null)
        {
            TreeItem<String> item = (TreeItem<String>) jfxTreeView.getSelectionModel().getSelectedItem();
            send("CWD "+ item.getValue());
            readd() ;
            appliquerChangement();
        }
    }

    public void retour() throws IOException, InterruptedException {
        send("CDUP");
        String réponse = readd() ;
        if (réponse.startsWith("200")) {}
        appliquerChangement();
    }

    class Element extends ListCell<String>
    {
        HBox hBox = new HBox() ;

        Text télécharger = GlyphsDude.createIcon(FontAwesomeIconName.ARROW_CIRCLE_DOWN,"1.5em") ;
        Text supprimer = GlyphsDude.createIcon(FontAwesomeIconName.TRASH,"1.5em") ;
        Text dossier =  GlyphsDude.createIcon(FontAwesomeIconName.FOLDER_OPEN,"1.5em") ;
        Text fichier =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_TEXT,"1.5em") ;
        Text image =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_IMAGE_ALT,"1.5em") ;
        Text pdf =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_PDF_ALT,"1.5em") ;
        Text word =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_WORD_ALT,"1.5em") ;
        Text powerPiont =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_POWERPOINT_ALT,"1.5em") ;
        Text vidéo =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_VIDEO_ALT,"1.5em") ;
        Text mp3 =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_AUDIO_ALT,"1.5em") ;
        Text autre =  GlyphsDude.createIcon(FontAwesomeIconName.FILE_ALT,"1.5em") ;

        JFXButton button1 = new JFXButton("") ;
        JFXButton button2 = new JFXButton("") ;

        Pane pane1 = new Pane() ;
        Label label = new Label("") ;
        Label label2 = new Label("") ;
        Label label3 = new Label("") ;
        Label label4 = new Label("") ;
        Label label41 = new Label("") ;
        Label label42 = new Label("") ;
        Label label5 = new Label("") ;
        Label label6 = new Label("") ;
        Label label7 = new Label("") ;

        public Element()
        {
            super();
            télécharger.setFill(Color.valueOf("#004d40"));
            supprimer.setFill(Color.valueOf("#004d40"));
            dossier.setFill(Color.valueOf("#ffb200"));
            fichier.setFill(Color.valueOf("#afcada"));
            image.setFill(Color.valueOf("#004d40"));
            pdf.setFill(Color.RED);
            word.setFill(Color.valueOf("#1565c0"));
            powerPiont.setFill(Color.RED);
            vidéo.setFill(Color.valueOf("#004d40"));
            mp3.setFill(Color.valueOf("#004d40"));
            autre.setFill(Color.valueOf("#004d40"));

            button1.setGraphic(télécharger);
            button2.setGraphic(supprimer);
            hBox.setSpacing(10);
            hBox.setAlignment(Pos.CENTER_LEFT);

            label2.setTextFill(Color.valueOf("#004d40"));
            label3.setTextFill(Color.valueOf("#004d40"));
            label4.setTextFill(Color.valueOf("#004d40"));
            label41.setTextFill(Color.valueOf("#004d40"));
            label42.setTextFill(Color.valueOf("#004d40"));
            label5.setTextFill(Color.valueOf("#004d40"));
            label6.setTextFill(Color.valueOf("#004d40"));
            label7.setTextFill(Color.valueOf("#004d40"));

            hBox.setHgrow(pane1, Priority.ALWAYS);
            button1.setOnAction(event -> {
                try {
                    Télechargement(getItem());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            button2.setOnAction(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Dialogue De Confirmation");
                alert.setHeaderText(null);
                alert.setContentText("Êtes-vous sûr de vouloir supprimer ce fichier?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    String item = getItem() ;
                    if (item.contains(".")){
                        try {
                            supprimerFichier(getItem());
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        try {
                            supprimerDossier(getItem());
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    alert.close();
                } else
                    alert.close();
            });
        }
        public void updateItem(String name ,boolean empty)
        {
            super.updateItem(name,empty);
            setText(null);
            setGraphic(null);

            if (name !=null && !empty) {
                hBox.getChildren().clear();
                label.setText(name.trim());
                if (!name.contains(".")) {
                    hBox.getChildren().clear();
                    hBox.getChildren().addAll(dossier,label ,pane1 , button2);
                } else {
                    if (name.trim().equals(".") || name.trim().equals("..")) {
                    } else {
                        int indexDebut = name.lastIndexOf(".") + 1;
                        String extension = name.substring(indexDebut), size = null, Time = null;
                        Double Size;
                        try {
                            Time = dateEtTemps(name);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            size = TailleFicher(name);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        assert size != null;
                        Size = Double.parseDouble(size.trim());
                        if (name.contains(".txt") || name.contains(".pdf")
                                || name.contains(".png") || name.contains(".jpg")
                                || name.contains(".mp4") || name.contains(".mp3")
                                || name.contains(".docx") || name.contains(".pptx")) {
                            if (name.contains(".txt")) {
                                label2.setText("\t" + Size / 1000 + " Ko" + "\t" + Time +"\t");
                                hBox.getChildren().addAll(fichier, label, label2, pane1, button1, button2);
                            }
                            if (name.contains(".png") || name.contains(".jpg")) {
                                label3.setText(Size / 1000 + " Ko" + "  " + Time);
                                hBox.getChildren().addAll(image, label, label3, pane1, button1, button2);
                            }
                            if (name.contains(".pdf")) {
                                label4.setText(Size / 1000 + " Ko" + "  " + Time);
                                hBox.getChildren().addAll(pdf, label, label4, pane1, button1, button2);
                            }
                            if (name.contains(".docx")) {
                                label41.setText(Size / 1000 + " Ko" + "  " + Time);
                                hBox.getChildren().addAll(word, label, label41, pane1, button1, button2);
                            }
                            if (name.contains(".pptx")) {
                                label42.setText(Size / 1000 + " Ko" + "  " + Time);
                                hBox.getChildren().addAll(powerPiont, label, label42, pane1, button1, button2);
                            }
                            if (name.contains(".mp4")) {
                                label6.setText(Size / 1000 + " Ko" + "  " + Time);
                                hBox.getChildren().addAll(vidéo, label, label6, pane1, button1, button2);
                            }
                            if (name.contains(".mp3")) {
                                label6.setText(Size / 1000 + " Ko" + "  " + Time);
                                hBox.getChildren().addAll(mp3, label, label6, pane1, button1, button2);
                            }
                        } else {
                            label7.setText(Size / 1000 + " Ko" + "  " + Time);
                            hBox.getChildren().addAll(autre, label, label7, pane1, button1, button2);
                        }
                    }
                }
                setGraphic(hBox);
            }
        }
    }

    private void supprimerFichier(String item) throws IOException, InterruptedException {
        send("DELE "+item);
        String reply = readd() ;
        if (reply.startsWith("250")){
            toast = new Text("     Fichier supprimé avec succès     " );
            toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                    "     -fx-font-size: 12;\n" +
                    "     -fx-font-weight: bold;");
            jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
            appliquerChangement();
        }
    }

    private void supprimerDossier(String item) throws IOException, InterruptedException {
        send("RMD "+item);
        String reply = readd() ;
        if (reply.startsWith("550") ) {
            toast = new Text("     Dossier non vide     " );
            toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                    "     -fx-font-size: 12;\n" +
                    "     -fx-font-weight: bold;");
            jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
        }else{
            if (reply.startsWith("250")){
                toast = new Text("     Dossier supprimé avec succès     " );
                toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                        "     -fx-font-size: 12;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
                appliquerChangement();
            }
        }
        appliquerChangement();
    }

    private String TailleFicher(String name) throws IOException {
        send("SIZE "+name);
        String size = readd() ;
        return   size.substring(4);
    }

    private String dateEtTemps(String name) throws IOException {
        send("MDTM " + name);
        String reponse = readd() ;

        String annee = reponse.substring(4,8) ;
        String moin = reponse.substring(8,10) ;
        String jour = reponse.substring(10,12) ;

        String h = reponse.substring(12,14) ;
        String m = reponse.substring(14,16);
        String s = reponse.substring(16,18);

        return annee+"/"+moin+"/"+jour+" "+h+":"+m+":"+s;
    }

    private void Télechargement(String item) throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.showSaveDialog(null);

        String path = String.valueOf(fileChooser.getSelectedFile());
        send("TYPE ASCII");
        readd() ;
        créationSocket();
        InputStream inputStream = dataSocket.getInputStream();

        send("RETR "+item);
        readd() ;

        byte[] bytes = new byte[16 * 1024];

        File f = new File( path+(char)47+item.trim()) ;
        OutputStream out = new FileOutputStream(f);

        int count;
        while ((count = inputStream.read(bytes)) > 0) {
            out.write(bytes, 0, count);
        }
        dataSocket.close();
        inputStream.close();
        if (readd().startsWith("226")) {
            toast = new Text("     fichier téléchargé avec succès     " );
            toast.setStyle("-fx-fill: #ffddc1 ;\n" +
                    "     -fx-font-size: 12;\n" +
                    "     -fx-font-weight: bold;");
            jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
        }
    }

}
