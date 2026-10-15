package view;

import model.Aluno;
import model.Disciplina;
import model.Matricula;
import service.MatriculaException;
import service.SistemaAcademicoService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * TelaPrincipal — interface gráfica Swing para interação do usuário com o sistema acadêmico.
 *
 * <p>Conceito de POO demonstrado: Separação de Camadas (Model-View-Service), onde a camada
 * visual é estritamente consumidora de serviços e observadora do modelo, sem manipular regras de
 * negócio ou acessar diretamente o subsistema de persistência.</p>
 *
 * <p>Colabora com: {@link SistemaAcademicoService}, {@link Aluno}, {@link Disciplina}, {@link Matricula}.</p>
 */
public final class TelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final SistemaAcademicoService service;
    private final JComboBox<Aluno> comboAlunos;
    private final JComboBox<Disciplina> comboDisciplinas;
    private final DefaultListModel<Disciplina> modeloDisciplinasAtivas;
    private final DefaultTableModel modeloOcupacao;
    private final JLabel barraStatus;

    /**
     * Constrói a janela principal do sistema acadêmico vinculada ao serviço de negócios.
     *
     * @param service a fachada da camada de serviço (não pode ser nula)
     */
    public TelaPrincipal(SistemaAcademicoService service) {
        super("Sistema Acadêmico — Matrículas");
        this.service = Objects.requireNonNull(service, "O serviço acadêmico não pode ser nulo.");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(940, 560);
        setLocationRelativeTo(null);

        this.comboAlunos = new JComboBox<>();
        this.comboDisciplinas = new JComboBox<>();
        this.modeloDisciplinasAtivas = new DefaultListModel<>();

        // Tabela de ocupação com células não editáveis
        this.modeloOcupacao = new DefaultTableModel(
                new Object[]{"Código", "Nome", "Ocup.", "Lim.", "Situação"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.barraStatus = new JLabel(" Sistema inicializado com sucesso.");

        montarLayout();
        atualizarTela();
    }

    /**
     * Monta a estrutura de componentes visuais e conecta os tratadores de eventos.
     */
    private void montarLayout() {
        // Painel Superior: Seleções e Botões de Ação
        JPanel painelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        painelSuperior.add(new JLabel("Aluno:"));
        painelSuperior.add(comboAlunos);

        painelSuperior.add(new JLabel("Disciplina:"));
        painelSuperior.add(comboDisciplinas);

        JButton btnMatricular = new JButton("Matricular");
        JButton btnCancelar = new JButton("Cancelar matrícula");
        JButton btnNovoAluno = new JButton("+ Novo Aluno");
        JButton btnNovaDisciplina = new JButton("+ Nova Disciplina");

        painelSuperior.add(btnMatricular);
        painelSuperior.add(btnCancelar);
        painelSuperior.add(btnNovoAluno);
        painelSuperior.add(btnNovaDisciplina);

        // Painel Central: Lista de ativas à esquerda e Tabela de ocupação à direita
        JPanel painelCentral = new JPanel(new GridLayout(1, 2, 12, 10));

        // Esquerda: Disciplinas Ativas
        JList<Disciplina> listaDisciplinasAtivas = new JList<>(modeloDisciplinasAtivas);
        listaDisciplinasAtivas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel painelEsquerda = new JPanel(new BorderLayout());
        painelEsquerda.setBorder(BorderFactory.createTitledBorder("Disciplinas ativas do aluno"));
        painelEsquerda.add(new JScrollPane(listaDisciplinasAtivas), BorderLayout.CENTER);

        // Direita: Ocupação das Disciplinas
        JTable tabelaOcupacao = new JTable(modeloOcupacao);
        tabelaOcupacao.setFillsViewportHeight(true);
        tabelaOcupacao.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel painelDireita = new JPanel(new BorderLayout());
        painelDireita.setBorder(BorderFactory.createTitledBorder("Ocupação das disciplinas"));
        painelDireita.add(new JScrollPane(tabelaOcupacao), BorderLayout.CENTER);

        painelCentral.add(painelEsquerda);
        painelCentral.add(painelDireita);

        // Painel Inferior: Barra de status
        JPanel painelInferior = new JPanel(new BorderLayout());
        painelInferior.setBorder(BorderFactory.createEtchedBorder());
        painelInferior.add(barraStatus, BorderLayout.CENTER);

        // Configuração do Frame
        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(painelSuperior, BorderLayout.NORTH);
        getContentPane().add(painelCentral, BorderLayout.CENTER);
        getContentPane().add(painelInferior, BorderLayout.SOUTH);

        // Configuração de Ouvintes de Eventos
        comboAlunos.addActionListener(e -> atualizarDisciplinasDoAluno());

        listaDisciplinasAtivas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Disciplina selecionada = listaDisciplinasAtivas.getSelectedValue();
                if (selecionada != null) {
                    comboDisciplinas.setSelectedItem(selecionada);
                }
            }
        });

        btnMatricular.addActionListener(e -> aoMatricular());
        btnCancelar.addActionListener(e -> aoCancelar());
        btnNovoAluno.addActionListener(e -> aoNovoAluno());
        btnNovaDisciplina.addActionListener(e -> aoNovaDisciplina());
    }

    /**
     * Trata o evento de solicitação de matrícula para o aluno e disciplina selecionados.
     */
    private void aoMatricular() {
        Aluno aluno = (Aluno) comboAlunos.getSelectedItem();
        Disciplina disciplina = (Disciplina) comboDisciplinas.getSelectedItem();

        if (aluno == null || disciplina == null) {
            mostrarErro("Selecione um aluno e uma disciplina para realizar a matrícula.");
            return;
        }

        try {
            // [CAMADAS] Delega a validação de regras e matrícula para o serviço
            Matricula matricula = service.matricular(aluno.getRa(), disciplina.getCodigo());
            mostrarSucesso("Matrícula #" + matricula.getId() + " realizada: " + aluno.getNome() + " -> " + disciplina.getCodigo());
            atualizarTela();
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Trata o evento de cancelamento de matrícula ativa para o aluno e disciplina selecionados.
     */
    private void aoCancelar() {
        Aluno aluno = (Aluno) comboAlunos.getSelectedItem();
        Disciplina disciplina = (Disciplina) comboDisciplinas.getSelectedItem();

        if (aluno == null || disciplina == null) {
            mostrarErro("Selecione um aluno e uma disciplina para cancelar a matrícula.");
            return;
        }

        try {
            // [CAMADAS] Delega o cancelamento para o serviço
            service.cancelarMatricula(aluno.getRa(), disciplina.getCodigo());
            mostrarSucesso("Matrícula de " + aluno.getNome() + " em " + disciplina.getCodigo() + " cancelada.");
            atualizarTela();
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Coleta os dados para cadastro de um novo aluno e solicita ao serviço a inclusão.
     */
    private void aoNovoAluno() {
        String ra = JOptionPane.showInputDialog(
                this,
                "Informe o RA do aluno:",
                "Cadastrar Novo Aluno",
                JOptionPane.QUESTION_MESSAGE
        );
        if (ra == null) {
            return; // Usuário cancelou
        }

        String nome = JOptionPane.showInputDialog(
                this,
                "Informe o Nome do aluno:",
                "Cadastrar Novo Aluno",
                JOptionPane.QUESTION_MESSAGE
        );
        if (nome == null) {
            return; // Usuário cancelou
        }

        try {
            // [CAMADAS] Delega o cadastro e a persistência do aluno para o serviço
            Aluno novoAluno = service.cadastrarAluno(ra.trim(), nome.trim());
            mostrarSucesso("Aluno cadastrado com sucesso: " + novoAluno.getNome());
            atualizarTela();
            comboAlunos.setSelectedItem(novoAluno);
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Coleta os dados para cadastro de uma nova disciplina e solicita ao serviço a inclusão.
     */
    private void aoNovaDisciplina() {
        String codigo = JOptionPane.showInputDialog(
                this,
                "Informe o código da disciplina (ex: WEB501):",
                "Cadastrar Nova Disciplina",
                JOptionPane.QUESTION_MESSAGE
        );
        if (codigo == null) {
            return; // Usuário cancelou
        }

        String nome = JOptionPane.showInputDialog(
                this,
                "Informe o nome da disciplina:",
                "Cadastrar Nova Disciplina",
                JOptionPane.QUESTION_MESSAGE
        );
        if (nome == null) {
            return; // Usuário cancelou
        }

        String limiteStr = JOptionPane.showInputDialog(
                this,
                "Informe o limite de vagas (número inteiro positivo):",
                "Cadastrar Nova Disciplina",
                JOptionPane.QUESTION_MESSAGE
        );
        if (limiteStr == null) {
            return; // Usuário cancelou
        }

        int limiteVagas;
        try {
            limiteVagas = Integer.parseInt(limiteStr.trim());
        } catch (NumberFormatException e) {
            mostrarErro("O limite de vagas deve ser um número inteiro válido.");
            return;
        }

        try {
            // [CAMADAS] Delega o cadastro e a persistência da disciplina para o serviço
            Disciplina novaDisciplina = service.cadastrarDisciplina(codigo.trim(), nome.trim(), limiteVagas);
            mostrarSucesso("Disciplina cadastrada com sucesso: " + novaDisciplina.getCodigo());
            atualizarTela();
            comboDisciplinas.setSelectedItem(novaDisciplina);
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Recarrega todos os componentes visuais a partir do estado atual da camada de serviço,
     * preservando sempre que possível as entidades selecionadas nos componentes.
     */
    private void atualizarTela() {
        Aluno alunoSelecionado = (Aluno) comboAlunos.getSelectedItem();
        Disciplina disciplinaSelecionada = (Disciplina) comboDisciplinas.getSelectedItem();

        // [CAMADAS] Consulta listagens imutáveis do serviço para popular combos
        comboAlunos.removeAllItems();
        for (Aluno a : service.listarAlunos()) {
            comboAlunos.addItem(a);
        }
        if (alunoSelecionado != null) {
            comboAlunos.setSelectedItem(alunoSelecionado);
        }

        comboDisciplinas.removeAllItems();
        for (Disciplina d : service.listarDisciplinas()) {
            comboDisciplinas.addItem(d);
        }
        if (disciplinaSelecionada != null) {
            comboDisciplinas.setSelectedItem(disciplinaSelecionada);
        }

        atualizarDisciplinasDoAluno();
        atualizarTabelaOcupacao();
    }

    /**
     * Atualiza a lista de disciplinas ativas correspondentes ao aluno atualmente selecionado.
     */
    private void atualizarDisciplinasDoAluno() {
        modeloDisciplinasAtivas.clear();
        Aluno aluno = (Aluno) comboAlunos.getSelectedItem();
        if (aluno != null) {
            try {
                // [CAMADAS] Consulta disciplinas ativas do aluno via service
                List<Disciplina> ativas = service.listarDisciplinasAtivas(aluno.getRa());
                for (Disciplina d : ativas) {
                    modeloDisciplinasAtivas.addElement(d);
                }
            } catch (MatriculaException ex) {
                mostrarErro(ex.getMessage());
            }
        }
    }

    /**
     * Atualiza a tabela de disciplinas com o número de vagas ocupadas e situação atual.
     */
    private void atualizarTabelaOcupacao() {
        modeloOcupacao.setRowCount(0);
        // [CAMADAS] Obtém a lista atualizada de disciplinas do serviço
        for (Disciplina d : service.listarDisciplinas()) {
            String situacao = d.temVagas() ? "Disponível" : "LOTADA";
            modeloOcupacao.addRow(new Object[]{
                    d.getCodigo(),
                    d.getNome(),
                    d.getVagasOcupadas(),
                    d.getLimiteVagas(),
                    situacao
            });
        }
    }

    /**
     * Atualiza o texto da barra de status no rodapé com uma mensagem de sucesso.
     *
     * @param mensagem texto informativo a ser exibido
     */
    private void mostrarSucesso(String mensagem) {
        barraStatus.setText(" " + mensagem);
    }

    /**
     * Exibe uma caixa de diálogo de erro com a mensagem explicativa e atualiza a barra de status.
     *
     * @param mensagem motivo da falha informado pela camada de negócio
     */
    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
        barraStatus.setText(" Erro: " + mensagem);
    }
}
