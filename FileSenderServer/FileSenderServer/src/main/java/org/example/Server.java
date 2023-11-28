package org.example;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.json.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.example.Util.Pair;



public class Server {

    private final HttpServer m_http_server;
    private static int m_threads_count = 1;
    private static HashMap<String, SplitFile> m_clients_files;
    private static final String m_filename_field_name = "file";



    public Server(String _hostname, int _port, int _threads_count) throws IOException, IllegalArgumentException {
        if (_threads_count < 1) {
            throw new IllegalArgumentException("Threads count must be grater then zero");
        }

        m_http_server = HttpServer.create(new InetSocketAddress(_hostname, _port), 0);
        HttpContext context = m_http_server.createContext("/load");
        context.setHandler(Server::LoadFileRequestHandler);

        m_threads_count = _threads_count;

        m_clients_files = new HashMap<>();
    }



    public InetSocketAddress GetAddress() {
        return m_http_server.getAddress();
    }



    public void Start() {
        m_http_server.start();
        System.out.println( "=> [INFO] Server running..." );
    }



    public void Stop() {
        m_http_server.stop(0);
        System.out.println( "=> [INFO] Server stopped" );
    }



    private static void LoadFileRequestHandler(HttpExchange _exchange) {
        System.out.println("=> [INFO] Loading file request handler running...");

        String method = _exchange.getRequestMethod();

        if (!Objects.equals(method, "GET")) {
            System.out.println("=> [WARN] Incorrect request method: " + method);

            try {
                IncorrectRequestMethodHandler(_exchange);
            }
            catch (IOException _ex) {
                System.out.println("=> [ERROR]: " + _ex.getMessage());
            }
            return;
        }

        try {
            ProcessBody(_exchange);
        }
        catch (IOException _ex) {
            System.out.println("=> [ERROR]: " + _ex.getMessage());
        }
    }



    private static void IncorrectRequestMethodHandler(HttpExchange _exchange) throws IOException {
        StringBuilder response_body = new StringBuilder();
        response_body.append("This method is not supported on the server.\n");
        response_body.append("Permitted methods:\n");
        response_body.append("\tGET");

        _exchange.sendResponseHeaders(400, response_body.length());

        OutputStream body = _exchange.getResponseBody();
        body.write(response_body.toString().getBytes());
        body.close();
    }



    private static void ProcessBody(HttpExchange _exchange) throws IOException {
        System.out.println("=> [INFO] Processing request body...");

        try (InputStream body = _exchange.getRequestBody()) {
            JsonReader reader = Json.createReader(body);

            JsonObject object = reader.readObject();

            String filepath = object.getString(m_filename_field_name);

            if (!new File(filepath).exists()) {
                StringBuilder response = new StringBuilder();
                response.append("=> [WARN] File: '");
                response.append(filepath);
                response.append("' is ont exists");

                System.out.println(response);

                _exchange.sendResponseHeaders(400, response.length());
                OutputStream response_body = _exchange.getResponseBody();
                response_body.write(response.toString().getBytes());
                response_body.close();
                return;
            }

            Pair<ArrayList<Integer>, String> pair = OpenParallelFilePartsSendersForClient(filepath);
            JsonArrayBuilder array_builder = Json.createArrayBuilder();
            for (var port : pair.getFirst()) {
                array_builder.add(port);
            }

            JsonObject response = Json.createObjectBuilder().
                                        add("URL", pair.getSecond()).
                                        add("ports", array_builder.build()).build();

            _exchange.sendResponseHeaders(200, response.size());
            OutputStream response_body = _exchange.getResponseBody();
            response_body.write(response.toString().getBytes());
            response_body.close();
        }
    }



    private static Pair<ArrayList<Integer>, String>
    OpenParallelFilePartsSendersForClient(String _filepath) throws IOException {
        System.out.println("=> [INFO] Opening servers for parallel file loading...");

        String client_uuid = UUID.randomUUID().toString();

        SplitFile file = new SplitFile(_filepath, m_threads_count);

        m_clients_files.put(client_uuid, file);


        ArrayList<Integer> ports = new ArrayList<>();

        for (int i = 0; i < m_threads_count; i++) {
            HttpServer file_part_sender = HttpServer.create(new InetSocketAddress(0), 0);
            HttpContext context = file_part_sender.createContext("/" + client_uuid);
            context.setHandler(Server::SendFilePart);

            ports.set(i, file_part_sender.getAddress().getPort());

            Thread.ofPlatform().daemon(true).start(() -> {
                Server.RunFilePartSender(file_part_sender);
            });
        }

        return new Pair<>(ports, client_uuid);
    }



    private static void RunFilePartSender(HttpServer _acceptor) {
        _acceptor.start();
    }



    private static void SendFilePart(HttpExchange _exchange) {
        System.out.println("=> [INFO] Send file part request handler running...");

        try (JsonReader reader = Json.createReader(_exchange.getRequestBody())) {
            int number_of_part = reader.readObject().getInt("part");

            System.out.println("=> [INFO] Requested part of file: " + number_of_part);

            SplitFile file = m_clients_files.get(_exchange.getRequestURI().toString().substring(1));
            String data = file.GetPart(number_of_part);

            JsonObject response = Json.createObjectBuilder().add("data", data).build();

            _exchange.sendResponseHeaders(200, response.size());
            OutputStream body = _exchange.getResponseBody();
            body.write(response.toString().getBytes());
            body.close();

            _exchange.getHttpContext().getServer().stop(0);

            System.out.println("=> [INFO] Server on address" + _exchange.getLocalAddress() + "' closed");

        } catch (IOException _ex) {
            System.out.println("=> [ERROR]: " + _ex.getMessage());
            throw new RuntimeException(_ex);
        }
    }
}
