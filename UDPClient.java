package UDPConection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;

public class UDPClient extends JFrame {
    private static final int FACTOR = 4 * 2;
    private static final int WIDTH = 160 * FACTOR;
    private static final int HEIGHT = 90 * FACTOR;
    private static final int CURSOR_SIZE = 20;
    private static final int SERVER_WIDTH = 1920;
    private static final int SERVER_HEIGHT = 1080;

    private Point mousePosition = new Point(0, 0);
    private JPanel drawingPanel;
    private JButton startButton;
    private JButton stopButton;
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean telemetryActive = false;

    public UDPClient() {
        initializeUI();
        initializeNetwork();
    }

    private void initializeUI() {
        setTitle("Mouse Telemetry");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        drawingPanel = createDrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    private JPanel createDrawingPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                g2d.setColor(Color.WHITE);
                int x = mousePosition.x - CURSOR_SIZE / 2;
                int y = mousePosition.y - CURSOR_SIZE / 2;
                g2d.fillOval(x, y, CURSOR_SIZE, CURSOR_SIZE);
            }
        };
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        startButton = new JButton("Start Telemetry");
        stopButton = new JButton("Stop Telemetry");
        
        startButton.addActionListener(this::startTelemetry);
        stopButton.addActionListener(this::stopTelemetry);
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        
        return buttonPanel;
    }

    private void initializeNetwork() {
        try {
            clientSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
            serverPort = 12345;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error initializing network: " + e.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startTelemetry(ActionEvent e) {
        if (!telemetryActive) {
            telemetryActive = true;
            sendMessage("START TELEMETRY");
            new Thread(this::receiveTelemetry).start();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        }
    }

    private void stopTelemetry(ActionEvent e) {
        if (telemetryActive) {
            telemetryActive = false;
            sendMessage("STOP TELEMETRY");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            System.out.println("Sent message: " + message);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error sending message: " + e.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receiveTelemetry() {
        while (telemetryActive) {
            try {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                
                String mouseCoordinates = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String[] coordinates = mouseCoordinates.split(" ");
                int x = Integer.parseInt(coordinates[0].split(":")[1]);
                int y = Integer.parseInt(coordinates[1].split(":")[1]);
                
                System.out.println("Coordenadas del mouse: " + mouseCoordinates);
                SwingUtilities.invokeLater(() -> updateMousePosition(x, y));
            } catch (IOException e) {
                if (telemetryActive) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error receiving telemetry: " + e.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
                    telemetryActive = false;
                }
            }
        }
    }

    private void updateMousePosition(int x, int y) {
        int scaledX = (int) ((double) x / SERVER_WIDTH * WIDTH);
        int scaledY = (int) ((double) y / SERVER_HEIGHT * HEIGHT);
        mousePosition.setLocation(scaledX, scaledY);
        drawingPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UDPClient client = new UDPClient();
            client.setVisible(true);
        });
    }
}