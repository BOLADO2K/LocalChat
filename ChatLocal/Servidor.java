// Servidor.java
import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static final int PORTA = 12345;
    private static HashSet<PrintWriter> escritoresClientes = new HashSet<>();
    
    public static void main(String[] args) {
        System.out.println("Servidor de chat iniciado na porta " + PORTA);
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + socket);
                
                PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
                escritoresClientes.add(escritor);
                
                Thread t = new Thread(new ManipuladorCliente(socket, escritor));
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
    
    static class ManipuladorCliente implements Runnable {
        private Socket socket;
        private PrintWriter escritor;
        private BufferedReader leitor;
        
        public ManipuladorCliente(Socket socket, PrintWriter escritor) {
            this.socket = socket;
            this.escritor = escritor;
            try {
                this.leitor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Erro ao criar leitor: " + e.getMessage());
            }
        }
        
        public void run() {
            String mensagem;
            try {
                while ((mensagem = leitor.readLine()) != null) {
                    System.out.println("Mensagem recebida: " + mensagem);
                    broadcastMensagem(mensagem);
                }
            } catch (IOException e) {
                System.out.println("Erro ao ler mensagem: " + e.getMessage());
            } finally {
                try {
                    escritoresClientes.remove(escritor);
                    socket.close();
                    leitor.close();
                    escritor.close();
                } catch (IOException e) {
                    System.out.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
        
        private void broadcastMensagem(String mensagem) {
            for (PrintWriter escritor : escritoresClientes) {
                escritor.println(mensagem);
            }
        }
    }
}