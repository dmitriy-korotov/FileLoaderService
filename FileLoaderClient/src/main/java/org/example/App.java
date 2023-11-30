package org.example;


import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.effect.Light;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;



public class App extends Application {

    private static final String m_application_title = "File loader";
    private static final String m_application_icon = "res/load.png";
    private static final String m_index_page = "/templates/index.fxml";

    private static TextField m_url_text_field;
    private static TextField m_save_path_text_field;
    private static Button m_browse_btn;
    private static Button m_OK_btn;
    private static Label m_log_field;
    private static final ArrayList<ProgressBar> m_progress_bars = new ArrayList<>();



    public static void main(String[] _args) {
        launch();
    }


    @Override
    public void start(Stage _stage) {
        _stage.setTitle(m_application_title);
        _stage.setMaximized(true);
        SetupIconTo(_stage);
        Parent root = LoadPageFromFXML();
        assert root != null;
        _stage.setScene(new Scene(root));
        _stage.show();
    }



    private void SetupIconTo(Stage _stage) {
        try {
            InputStream icon_stream = new FileInputStream(m_application_icon);
            Image image = new Image(icon_stream);
            _stage.getIcons().add(image);
        }
        catch (Exception _ex) {
            System.out.println("=> [ERROR] Can't load icon: " + _ex.getMessage());
        }
    }


    private Parent LoadPageFromFXML() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource(m_index_page));

            Parent root = loader.load();
            if (root == null) {
                throw new Exception("Root is null");
            }

            m_browse_btn = (Button) loader.getNamespace().get("browse_btn");
            m_url_text_field = (TextField) loader.getNamespace().get("url_field");
            m_save_path_text_field = (TextField) loader.getNamespace().get("saved_path_field");
            m_browse_btn = (Button) loader.getNamespace().get("browse_btn");
            m_log_field = (Label) loader.getNamespace().get("log");
            m_OK_btn = (Button) loader.getNamespace().get("OK");

            for (int i = 1; i < 7; i++) {
                m_progress_bars.add((ProgressBar) loader.getNamespace().get("bar" + i));
            }

            m_browse_btn.setOnAction((ActionEvent _event) -> {
                Thread.startVirtualThread(() -> {
                    var error = BrowseButtonClickHandler();

                    m_log_field.setVisible(true);
                    m_OK_btn.setVisible(true);

                    if (error.isEmpty()) {
                        m_log_field.setTextFill(Color.color(0, 1, 0));
                        m_log_field.setText("File successfully loaded!");
                        m_OK_btn.setTextFill(Color.color(0, 1, 0));
                    } else {
                        m_log_field.setTextFill(Color.color(1, 0, 0));
                        m_log_field.setText(error.get());
                        m_OK_btn.setTextFill(Color.color(1, 0, 0));
                    }
                });
            });

            m_OK_btn.setOnAction((ActionEvent _event) -> {
                m_OK_btn.setVisible(false);
                m_log_field.setVisible(false);
                for (var bar : m_progress_bars) {
                    bar.setProgress(0);
                }
            });

            return root;
        }
        catch (Exception _ex) {
            System.out.println(_ex.getMessage());
        }
        return null;
    }



    public Optional<String> BrowseButtonClickHandler() {
        String url = m_url_text_field.getText();
        if (!IsValidURL(url)) {
            return Optional.of("URL is no valid!");
        }

        String output_dir = m_save_path_text_field.getText();
        if (!IsValidOutputDirectory(output_dir)) {
            return Optional.of("Save path is no valid!");
        }

        FileLoaderClient loader = new FileLoaderClient();

        URI uri = URI.create(url);

        String host = uri.getHost();
        int port = uri.getPort();
        String filepath = uri.getPath();

        if (host == null || port == -1 || filepath == null) {
            if (host == null) {
                return Optional.of("Invalid host!");
            } else if (port == -1) {
                return Optional.of("Invalid port!");
            }else {
                return Optional.of("Invalid filepath!");
            }
        }

        try {
            FileBuilder file_builder = loader.LoadFileFrom(host, port, filepath, m_progress_bars);
            if (loader.IsLoaded()) {
                String output_path = output_dir + url.substring(url.lastIndexOf('/'));
                BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(output_path));
                for (int i = 0; i < 6; i++) {
                    byte[] bytes = file_builder.GetFilePart(i);
                    writer.write(bytes, 0, bytes.length);
                }
                writer.close();
            }
        }
        catch (IOException | InterruptedException _ex) {
            System.out.println("=> [ERROR]: " + _ex.getMessage());
            return Optional.of("Load file error: " + _ex.getMessage());
        } catch (Exception _ex) {
            return Optional.of("Load file error: " + _ex.getMessage());
        }

        return Optional.empty();
    }



    private boolean IsValidURL(String _url) {
        return _url != null;
    }



    private boolean IsValidOutputDirectory(String _dir) {
        return _dir != null;
    }

}
