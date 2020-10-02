package sample;

import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXTextField;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;


public class LoginController extends Application implements Initializable {

    public JFXTextField username;
    public JFXPasswordField password;
    public JFXTextField port;
    public JFXTextField host;
    public JFXSnackbar jfxSnackbar;
    public AnchorPane anchorPane;

    private Socket socket = null;
    Text toast;

    private BufferedWriter writer;
    private BufferedInputStream reader;

    public void login() throws IOException, InterruptedException {

        ValidationSupport validationSupport = new ValidationSupport();

        if (username.getText().isEmpty()) {
            username.getStyleClass().add("wrong-credentials");
            validationSupport.registerValidator(username, Validator.createEmptyValidator("Username is Required"));
        }
        if (password.getText().isEmpty()) {
            password.getStyleClass().add("wrong-credentials");
            validationSupport.registerValidator(password, Validator.createEmptyValidator("password is Required"));
        }
        if (port.getText().isEmpty()) {
            port.getStyleClass().add("wrong-credentials");
            validationSupport.registerValidator(port, Validator.createEmptyValidator("password is Required"));
        }
        if (host.getText().isEmpty()) {
            host.getStyleClass().add("wrong-credentials");
            validationSupport.registerValidator(host, Validator.createEmptyValidator("password is Required"));
        }

        if(!username.getText().isEmpty() && !password.getText().isEmpty()
                && !host.getText().isEmpty() && !port.getText().isEmpty()){
            if (socket != null)
                throw new IOException("La connexion au FTP est déjà activée");

            socket = new Socket(host.getText(), Integer.parseInt(port.getText()));

            reader = new BufferedInputStream(socket.getInputStream());
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String response = readd();

            if (!response.startsWith("220")) {
                toast = new Text("     Erreur de connexion au serveur FTP.     ");
                toast.setStyle("-fx-fill: white ;\n" +
                        "     -fx-font-size: 11;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
                socket = null;
                return;
            }

            send("USER " + username.getText());
            response = readd();
            if(!response.startsWith("331")){
                toast = new Text("Erreur de connexion avec le compte utilisateur:\n\t" + response);
                toast.setStyle("-fx-fill: white ;\n" +
                        "     -fx-font-size: 11;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
                socket = null;
                return;
            }

            send("PASS " + password.getText());
            response = readd();
            if(!response.startsWith("230")){
                toast = new Text("Erreur de connexion avec le compte utilisateur:\n\t" + response);
                toast.setStyle("-fx-fill: white ;\n" +
                        "     -fx-font-size: 11;\n" +
                        "     -fx-font-weight: bold;");
                jfxSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(toast));
                socket = null;
                return;
            }
            //Nous sommes maintenant connectés
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../sample/FtpController.fxml"));
            Parent root = loader.load();
            FTP ftp = loader.getController();
            ftp.fonction(socket ,username.getText(), host.getText(), port.getText());

            Stage stage = new Stage(StageStyle.DECORATED);
            stage.setTitle("Client FTP");
            stage.setScene(new Scene(root));
            stage.show();
        }
    }

    private String readd() throws IOException{
        String respons;
        int stream;
        byte[] b = new byte[4096];
        stream = reader.read(b);
        respons = new String(b, 0, stream);
        return respons;
    }

    private void send(String command) throws IOException{
        command += "\r\n";
        writer.write(command);
        writer.flush();
    }

    @Override
    public void start(Stage primaryStage) {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}

