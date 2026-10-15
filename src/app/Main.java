package app;

import repository.ArquivoRepository;
import service.SistemaAcademicoService;
import view.TelaPrincipal;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Main — ponto de entrada da aplicação desktop do Sistema Acadêmico.
 *
 * <p>Conceito de POO demonstrado: Inicialização e Injeção de Dependências manual entre as
 * camadas de Persistência (Repository), Regras de Negócio (Service) e Apresentação (View).</p>
 *
 * <p>Colabora com: {@link ArquivoRepository}, {@link SistemaAcademicoService}, {@link TelaPrincipal}.</p>
 */
public final class Main {

    /**
     * Construtor privado para impedir instanciação da classe utilitária de inicialização.
     */
    private Main() {
    }

    /**
     * Ponto de entrada da aplicação. Inicializa repositório, serviço e lança a interface na Event Dispatch Thread.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        // [PERSISTÊNCIA] Configuração do diretório padrão de armazenamento
        Path diretorioDados = Path.of("dados");
        ArquivoRepository repositorio = new ArquivoRepository(diretorioDados);

        // [CAMADAS] Montagem do serviço com repositório injetado
        SistemaAcademicoService service = new SistemaAcademicoService(repositorio);

        try {
            // [PERSISTÊNCIA] Carga inicial do repositório (ou criação do seed)
            service.carregarDados();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Falha crítica ao carregar os dados do sistema:\n" + e.getMessage(),
                    "Erro de Inicialização",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // [CAMADAS] Inicialização da interface gráfica na thread de despacho de eventos do Swing
        SwingUtilities.invokeLater(() -> {
            TelaPrincipal tela = new TelaPrincipal(service);
            tela.setVisible(true);
        });
    }
}

