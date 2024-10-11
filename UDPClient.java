package UDPConection;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient extends JFrame {
    private static final int FACTOR = 4*2;
    private static final int WIDTH = 160*FACTOR;
    private static final int HEIGHT = 90 *FACTOR;
    private static final int CURSOR_SIZE = 20;
    private Point mousePosition = new Point(0, 0);
    private static final int SERVER_WIDTH = 1920;  // Asumimos una resolución de 1920x1080
    private static final int SERVER_HEIGHT = 1080; // Ajusta estos valores si son diferentes

    public UDPClient() {
        setTitle("Mouse Telemetry");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo negro
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Puntero blanco
                g2d.setColor(Color.WHITE);
                int x = mousePosition.x - CURSOR_SIZE / 2;
                int y = mousePosition.y - CURSOR_SIZE / 2;
                g2d.fillOval(x, y, CURSOR_SIZE, CURSOR_SIZE);
            }
        };
        add(panel);
    }

    public void updateMousePosition(int x, int y) {
        // Escalar las coordenadas del servidor a nuestra ventana
        int scaledX = (int) ((double) x / SERVER_WIDTH * WIDTH);
        int scaledY = (int) ((double) y / SERVER_HEIGHT * HEIGHT);
        mousePosition.setLocation(scaledX, scaledY);
        repaint();
    }

    public static void main(String[] args) throws Exception {
        UDPClient client = new UDPClient();
        client.setVisible(true);

        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 12345;

        // Enviar la solicitud para iniciar la telemetría
        String startTelemetryMessage = "START TELEMETRY";
        byte[] sendData = startTelemetryMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);

        System.out.println("Telemetría iniciada. Esperando coordenadas del servidor...");

        // Bucle para recibir las coordenadas del mouse
        while (true) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            // Procesar las coordenadas del mouse
            String mouseCoordinates = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String[] coordinates = mouseCoordinates.split(" ");
            int x = Integer.parseInt(coordinates[0].split(":")[1]);
            int y = Integer.parseInt(coordinates[1].split(":")[1]);

            // Imprimir las coordenadas por consola
            System.out.println("Coordenadas del mouse: " + mouseCoordinates);

            // Actualizar la posición del mouse en la ventana
            SwingUtilities.invokeLater(() -> client.updateMousePosition(x, y));
        }
    }
}