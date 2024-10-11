package UDPConection;

import java.awt.MouseInfo;
import java.awt.Point;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UDPServer {
    private static final int PORT = 12345;
    private static final long TELEMETRY_INTERVAL = 10; // 10 ms

    private DatagramSocket serverSocket;
    private AtomicBoolean telemetryActive = new AtomicBoolean(false);
    private InetAddress clientAddress;
    private int clientPort;
    private ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        new UDPServer().start();
    }

    public void start() {
        try {
            serverSocket = new DatagramSocket(PORT);
            System.out.println("Servidor UDP escuchando en el puerto " + PORT);
            
            while (true) {
                handleIncomingPackets();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    private void handleIncomingPackets() throws IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);

        String request = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
        
        if ("START TELEMETRY".equalsIgnoreCase(request)) {
            startTelemetry(receivePacket.getAddress(), receivePacket.getPort());
        } else if ("STOP TELEMETRY".equalsIgnoreCase(request)) {
            stopTelemetry();
        }
    }

    private void startTelemetry(InetAddress address, int port) {
        clientAddress = address;
        clientPort = port;
        telemetryActive.set(true);
        System.out.println("Telemetría iniciada para el cliente: " + clientAddress + ":" + clientPort);
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::sendTelemetryData, 0, TELEMETRY_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void stopTelemetry() {
        telemetryActive.set(false);
        System.out.println("Telemetría detenida para el cliente: " + clientAddress + ":" + clientPort);
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void sendTelemetryData() {
        if (!telemetryActive.get()) {
            return;
        }

        try {
            Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            String coordinates = "X:" + mousePosition.x + " Y:" + mousePosition.y;
            byte[] sendData = coordinates.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Error al enviar datos de telemetría: " + e.getMessage());
        }
    }
}