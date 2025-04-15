// App.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class App {
    private static final int PORTA = 12345;
    private static HashSet<PrintWriter> escritoresClientes = new HashSet<>();
    private static ServerSocket serverSocket;
    
    // Interface unificada
    private static JFrame frame;
    private static JTextField campoMensagem;
    private static JTextArea areaMensagens;
    private static JTextField campoNome;
    private static JTextArea logServidor;
    private static PrintWriter escritor;
    private static BufferedReader leitor;
    private static Socket socketCliente;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            criarInterface();
            // Iniciar o servidor em segundo plano automaticamente
            iniciarServidorBackground();
            // Conectar ao servidor local automaticamente
            conectarAoServidor("localhost");
        });
    }
    
    private static void criarInterface() {
        // Criar a interface gráfica
        frame = new JFrame("Chat em Rede Local");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        
        // Painel principal
        JPanel painelPrincipal = new JPanel(new BorderLayout());
        
        // Painel esquerdo (chat do cliente)
        JPanel painelCliente = new JPanel(new BorderLayout());
        
        // Área de histórico de mensagens
        areaMensagens = new JTextArea();
        areaMensagens.setEditable(false);
        areaMensagens.setLineWrap(true);
        JScrollPane scrollPaneChat = new JScrollPane(areaMensagens);
        scrollPaneChat.setBorder(BorderFactory.createTitledBorder("Chat"));
        
        // Painel para nome do usuário
        JPanel painelNome = new JPanel();
        painelNome.add(new JLabel("Seu nome: "));
        campoNome = new JTextField(15);
        painelNome.add(campoNome);
        
        // Painel para envio de mensagens
        JPanel painelMensagem = new JPanel();
        painelMensagem.setLayout(new BorderLayout());
        campoMensagem = new JTextField();
        JButton botaoEnviar = new JButton("Enviar");
        painelMensagem.add(campoMensagem, BorderLayout.CENTER);
        painelMensagem.add(botaoEnviar, BorderLayout.EAST);
        
        // Adicionar componentes ao painel cliente
        painelCliente.add(painelNome, BorderLayout.NORTH);
        painelCliente.add(scrollPaneChat, BorderLayout.CENTER);
        painelCliente.add(painelMensagem, BorderLayout.SOUTH);
        
        // Painel direito (log do servidor)
        logServidor = new JTextArea();
        logServidor.setEditable(false);
        JScrollPane scrollPaneLog = new JScrollPane(logServidor);
        scrollPaneLog.setBorder(BorderFactory.createTitledBorder("Log do Servidor"));
        
        // Adicionar os painéis ao painel principal
        painelPrincipal.add(painelCliente, BorderLayout.CENTER);
        painelPrincipal.add(scrollPaneLog, BorderLayout.EAST);
        
        // Adicionar o painel principal à janela
        frame.getContentPane().add(painelPrincipal);
        
        // Configurar ações
        ActionListener enviarAcao = e -> enviarMensagem();
        botaoEnviar.addActionListener(enviarAcao);
        campoMensagem.addActionListener(enviarAcao);
        
        // Mostrar a janela
        frame.setVisible(true);
    }
    
    // Inicia o servidor em segundo plano
    private static void iniciarServidorBackground() {
        adicionarLog("Iniciando servidor na porta " + PORTA + "...");
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORTA);
                adicionarLog("Servidor iniciado com sucesso!");
                
                while (true) {
                    Socket socket = serverSocket.accept();
                    String enderecoCliente = socket.getInetAddress().getHostAddress();
                    adicionarLog("Novo cliente conectado: " + enderecoCliente);
                    
                    PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
                    escritoresClientes.add(escritor);
                    
                    Thread t = new Thread(new ManipuladorCliente(socket, escritor));
                    t.start();
                }
            } catch (IOException e) {
                adicionarLog("Erro no servidor: " + e.getMessage());
            }
        }).start();
    }
    
    private static void adicionarLog(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());
            logServidor.append("[" + timestamp + "] " + mensagem + "\n");
            logServidor.setCaretPosition(logServidor.getDocument().getLength());
        });
    }
    
    // Classe para manipular as conexões dos clientes
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
                adicionarLog("Erro ao criar leitor: " + e.getMessage());
            }
        }
        
        public void run() {
            String mensagem;
            try {
                while ((mensagem = leitor.readLine()) != null) {
                    adicionarLog("Mensagem recebida: " + mensagem);
                    broadcastMensagem(mensagem);
                }
            } catch (IOException e) {
                adicionarLog("Erro ao ler mensagem: " + e.getMessage());
            } finally {
                try {
                    escritoresClientes.remove(escritor);
                    socket.close();
                    leitor.close();
                    escritor.close();
                    adicionarLog("Cliente desconectado: " + socket.getInetAddress().getHostAddress());
                } catch (IOException e) {
                    adicionarLog("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
        
        private void broadcastMensagem(String mensagem) {
            for (PrintWriter escritor : escritoresClientes) {
                escritor.println(mensagem);
            }
        }
    }
    
    private static void conectarAoServidor(String servidor) {
        try {
            socketCliente = new Socket(servidor, PORTA);
            escritor = new PrintWriter(socketCliente.getOutputStream(), true);
            leitor = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            
            // Thread para ler mensagens do servidor
            new Thread(() -> {
                String mensagem;
                try {
                    while ((mensagem = leitor.readLine()) != null) {
                        String mensagemFinal = mensagem;
                        SwingUtilities.invokeLater(() -> {
                            areaMensagens.append(mensagemFinal + "\n");
                            areaMensagens.setCaretPosition(areaMensagens.getDocument().getLength());
                        });
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> 
                        areaMensagens.append("Desconectado do servidor\n"));
                }
            }).start();
            
            areaMensagens.append("Conectado ao servidor local\n");
        } catch (IOException e) {
            areaMensagens.append("Erro ao conectar: " + e.getMessage() + "\n");
            // Tentar novamente após um pequeno intervalo
            new javax.swing.Timer(3000, event -> {
                ((javax.swing.Timer)event.getSource()).stop();
                conectarAoServidor(servidor);
            }).start();
        }
    }
    
    private static void enviarMensagem() {
        String nome = campoNome.getText().trim();
        String mensagem = campoMensagem.getText().trim();
        
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Por favor, digite seu nome!");
            return;
        }
        
        if (!mensagem.isEmpty() && escritor != null) {
            escritor.println(nome + ": " + mensagem);
            campoMensagem.setText("");
        }
    }
}