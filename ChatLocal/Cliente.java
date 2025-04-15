// Cliente.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Cliente {
    private JFrame frame;
    private JTextField campoMensagem;
    private JTextArea areaMensagens;
    private JTextField campoNome;
    private PrintWriter escritor;
    private BufferedReader leitor;
    private Socket socket;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente().iniciar());
    }
    
    public void iniciar() {
        // Criar a interface gráfica
        frame = new JFrame("Chat em Rede Local");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        
        // Área de histórico de mensagens
        areaMensagens = new JTextArea();
        areaMensagens.setEditable(false);
        areaMensagens.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(areaMensagens);
        
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
        
        // Adicionar componentes à janela
        frame.getContentPane().add(painelNome, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(painelMensagem, BorderLayout.SOUTH);
        
        // Mostrar a janela
        frame.setVisible(true);
        
        // Configurar ações
        ActionListener enviarAcao = e -> enviarMensagem();
        botaoEnviar.addActionListener(enviarAcao);
        campoMensagem.addActionListener(enviarAcao);
        
        // Conectar ao servidor
        conectarAoServidor();
    }
    
    private void conectarAoServidor() {
        String servidor = JOptionPane.showInputDialog(
            frame, 
            "Digite o endereço IP do servidor:", 
            "Conectar ao Servidor", 
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (servidor == null || servidor.trim().isEmpty()) {
            servidor = "localhost"; // Padrão se não especificado
        }
        
        try {
            socket = new Socket(servidor, 12345);
            escritor = new PrintWriter(socket.getOutputStream(), true);
            leitor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Thread para ler mensagens do servidor
            new Thread(() -> {
                String mensagem;
                try {
                    while ((mensagem = leitor.readLine()) != null) {
                        String mensagemFinal = mensagem;
                        SwingUtilities.invokeLater(() -> {
                            areaMensagens.append(mensagemFinal + "\n");
                            // Auto-rolagem para a última mensagem
                            areaMensagens.setCaretPosition(areaMensagens.getDocument().getLength());
                        });
                    }
                } catch (IOException e) {
                    System.out.println("Desconectado do servidor");
                }
            }).start();
            
            areaMensagens.append("Conectado ao servidor " + servidor + "\n");
        } catch (IOException e) {
            areaMensagens.append("Erro ao conectar: " + e.getMessage() + "\n");
        }
    }
    
    private void enviarMensagem() {
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