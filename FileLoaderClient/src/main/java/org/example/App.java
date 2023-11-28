package org.example;


import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;


public class App extends Application {

    private static final String m_application_title = "File loader";
    private static final String m_application_icon = "res/load.png";



    public static void main(String[] _args) {
        launch();
    }


    @Override
    public void start(Stage _stage) throws Exception {
        _stage.setTitle(m_application_title);
        SetupIconTo(_stage);
        _stage.show();
    }



    private void SetupIconTo(Stage _stage) {
        InputStream icon_stream = getClass().getResourceAsStream(m_application_icon);
        assert icon_stream != null;
        Image image = new Image(icon_stream);
        _stage.getIcons().add(image);
    }
}
