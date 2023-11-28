package org.example;


import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class App
{
    private static final Scanner m_scanner = new Scanner(System.in);
    private static final String m_close_command = "close";
    private static boolean m_application_running = true;



    public static void UserCommandsHandler() throws InterruptedException {
        Thread thread = Thread.ofPlatform().daemon(true).start(() -> {
            while (m_application_running) {
                System.out.print("> ");
                String command = m_scanner.nextLine();

                if (Objects.equals(command, m_close_command)) {
                    m_application_running = false;
                }
                else {
                    System.out.println("=> [ERROR] Unknowing command");
                }
            }
        });
        thread.join();
    }



    public static void main( String[] args )  {
        System.out.println("\nPermitted commands:\n\t'close' - closing server\n");

        try {
            Server file_sender_server = new Server("127.0.0.1", 8000, 6);
            System.out.println("=> [INFO] Address: " + file_sender_server.GetAddress().toString());
            file_sender_server.Start();
            UserCommandsHandler();
            file_sender_server.Stop();
        }
        catch (IOException | InterruptedException _ex) {
            System.out.println("=> [ERROR]: " + _ex.getMessage());
        }
    }
}
