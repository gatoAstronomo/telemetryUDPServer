import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class RelayServer {
    private static final int RELAY_PORT = 12345;
    private static Map<String, InetAddress> clients = new HashMap<>();
    private static Map<String, Integer> ports = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(RELAY_PORT)) {
            System.out.println("Relay server running on port " + RELAY_PORT);
            
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();
                
                // Registro del servidor
                if (message.startsWith("REGISTER_SERVER")) {
                    handleServerRegistration(senderAddress, senderPort);
                    continue;
                }
                
                // Registro del cliente
                if (message.startsWith("REGISTER_CLIENT")) {
                    handleClientRegistration(senderAddress, senderPort, message);
                    continue;
                }
                
                // Reenvío de mensajes
                if (clients.containsKey("SERVER") && clients.containsKey("CLIENT")) {
                    if (senderAddress.equals(clients.get("SERVER"))) {
                        forwardToClient(message, socket);
                    } else {
                        forwardToServer(message, socket);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleServerRegistration(InetAddress address, int port) {
        clients.put("SERVER", address);
        ports.put("SERVER", port);
        System.out.println("Server registered: " + address + ":" + port);
    }

    private static void handleClientRegistration(InetAddress address, int port, String message) {
        String[] parts = message.split(":");
        if (parts.length > 1) {
            clients.put(parts[1], address);
            ports.put(parts[1], port);
            System.out.println("Client " + parts[1] + " registered: " + address + ":" + port);
        }
    }

    private static void forwardToServer(String message, DatagramSocket socket) throws Exception {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(
            data, 
            data.length, 
            clients.get("SERVER"), 
            ports.get("SERVER")
        );
        socket.send(packet);
    }

    private static void forwardToClient(String message, DatagramSocket socket) throws Exception {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(
            data, 
            data.length, 
            clients.get("CLIENT"), 
            ports.get("CLIENT")
        );
        socket.send(packet);
    }
}