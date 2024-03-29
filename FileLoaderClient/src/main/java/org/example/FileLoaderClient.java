package org.example;

import javafx.scene.control.ProgressBar;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;



public class FileLoaderClient {

    private final HttpClient m_client = HttpClient.newHttpClient();
    private boolean m_is_loaded = false;
    private final ThreadPoolExecutor m_pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(6);
    private final FileBuilder m_file_builder = new FileBuilder(6);
    private static ArrayList<ProgressBar> m_progress_bars;
    private static String m_connection_address;
    private int m_connection_port;



    public FileBuilder LoadFileFrom(String _address,
                                    int _port,
                                    String _remote_filepath,
                                    ArrayList<ProgressBar> _progress_bars) throws Exception {

        m_connection_address = _address;
        m_connection_port = _port;

        String url = "http://" + m_connection_address + ":" + m_connection_port + "/load";

        m_progress_bars = _progress_bars;

        System.out.println("=> [INFO] Loading file from: " + url);

        var request = HttpRequest.newBuilder(URI.create(url)).
                header("accept", "application/json").
                POST(HttpRequest.BodyPublishers.ofString("{\"file\":\"" + _remote_filepath + "\"}")).
                build();

        var response = m_client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        return HandleResponse(response);
    }



    public boolean IsLoaded() {
        return m_is_loaded;
    }



    private FileBuilder HandleResponse(HttpResponse<InputStream> _response) throws Exception {
        try (InputStream body = _response.body()) {
            JsonReader reader = Json.createReader(body);

            JsonObject obj = reader.readObject();

            String uuid = obj.getString("URL");
            List<Integer> ports = obj.getJsonArray("ports").getValuesAs(val -> Integer.valueOf(val.toString()));

            System.out.println("=> [INFO] Loading process ID: " + uuid);

            int part_number = 0;
            for (var port : ports) {
                int finalPart_number = part_number;
                m_pool.execute(() -> {
                    HttpClient file_part_loader = HttpClient.newHttpClient();

                    String part_load_url = "http://" + m_connection_address + ":" + port + "/" + uuid;
                    var request_for_part_loading = HttpRequest.newBuilder(URI.create(part_load_url)).
                            header("accept", "application/json").
                            POST(HttpRequest.BodyPublishers.ofString("{\"part\":" + finalPart_number + "}")).
                            build();

                    try {
                        var response_for_part = file_part_loader.send(request_for_part_loading, HttpResponse.BodyHandlers.ofInputStream());

                        try (InputStream file_part_body = response_for_part.body()) {

                            var content_length_opt = response_for_part.headers().firstValue("Content-length");
                            if (content_length_opt.isEmpty()) {
                                throw new Exception("No field Content-Length");
                            }
                            int size = Integer.parseInt(content_length_opt.get());

                            double progress = 0;
                            double delta_progress = 1.0 / size;
                            byte[] bytes = new byte[size];
                            int index = 0;
                            while (size > 0) {
                                progress += delta_progress;
                                m_progress_bars.get(finalPart_number).setProgress(progress);
                                file_part_body.read(bytes, index, 1);
                                index++;
                                size--;
                            }
                            m_file_builder.AddFilePart(finalPart_number, bytes);
                        }
                        catch (Exception _ex) {
                            System.out.println("=> [ERROR]: " + _ex.getMessage());
                            throw new Exception("Can't load file");
                        }
                    } catch (Exception _ex) {
                        System.out.println("=> [ERROR]: " + _ex.getMessage());
                        try {
                            throw new Exception("Can't load file");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                part_number++;
            }

            while (m_pool.getActiveCount() > 0) {
                Thread.yield();
            }

            reader.close();
        }
        catch (Exception _ex) {
            System.out.println("=> [ERROR]: " + _ex.getMessage());
            throw new Exception("Can't load file");
        }

        System.out.println("=> [INFO] File is loaded");

        m_is_loaded = true;

        return m_file_builder;
    }
}
