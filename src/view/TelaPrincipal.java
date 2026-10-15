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
        setSize(1020, 580);
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
        // Painel Superior: Dividido em duas linhas para visualização clara de todos os controles
        JPanel painelSuperior = new JPanel(new GridLayout(2, 1, 0, 4));

        JButton btnMatricular = new JButton("Matricular");
        JButton btnCancelar = new JButton("Cancelar matrícula");
        JButton btnNovoAluno = new JButton("+ Novo Aluno");
        JButton btnEditarAluno = new JButton("* Editar Aluno");
        JButton btnRemoverAluno = new JButton("- Remover Aluno");
        JButton btnNovaDisciplina = new JButton("+ Nova Disciplina");
        JButton btnEditarDisciplina = new JButton("* Editar Disciplina");
        JButton btnRemoverDisciplina = new JButton("- Remover Disciplina");

        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        linha1.add(new JLabel("Aluno:"));
        linha1.add(comboAlunos);
        linha1.add(new JLabel("Disciplina:"));
        linha1.add(comboDisciplinas);
        linha1.add(btnMatricular);
        linha1.add(btnCancelar);

        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        linha2.add(btnNovoAluno);
        linha2.add(btnEditarAluno);
        linha2.add(btnRemoverAluno);
        linha2.add(new JLabel("  |  "));
        linha2.add(btnNovaDisciplina);
        linha2.add(btnEditarDisciplina);
        linha2.add(btnRemoverDisciplina);

        painelSuperior.add(linha1);
        painelSuperior.add(linha2);

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
        btnEditarAluno.addActionListener(e -> aoEditarAluno());
        btnRemoverAluno.addActionListener(e -> aoRemoverAluno());
        btnNovaDisciplina.addActionListener(e -> aoNovaDisciplina());
        btnEditarDisciplina.addActionListener(e -> aoEditarDisciplina());
        btnRemoverDisciplina.addActionListener(e -> aoRemoverDisciplina());
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
        String ra = pedirTexto(
                "Cadastrar Novo Aluno",
                "Informe o RA do aluno:",
                null
        );
        if (ra == null) {
            return; // Usuário cancelou
        }

        String nome = pedirTexto(
                "Cadastrar Novo Aluno",
                "Informe o Nome do aluno:",
                null
        );
        if (nome == null) {
            return; // Usuário cancelou
        }

        try {
            // [CAMADAS] Delega o cadastro e a persistência do aluno para o serviço
            Aluno novoAluno = service.cadastrarAluno(ra, nome);
            mostrarSucesso("Aluno cadastrado com sucesso: " + novoAluno.getNome());
            atualizarTela();
            comboAlunos.setSelectedItem(novoAluno);
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Coleta o novo nome para o aluno atualmente selecionado e solicita a alteração ao serviço.
     */
    private void aoEditarAluno() {
        Aluno aluno = (Aluno) comboAlunos.getSelectedItem();
        if (aluno == null) {
            mostrarErro("Selecione um aluno para editar.");
            return;
        }

        String novoNome = pedirTexto(
                "Editar Aluno",
                "Informe o novo nome para o aluno (RA: " + aluno.getRa() + "):",
                aluno.getNome()
        );
        if (novoNome == null) {
            return; // Usuário cancelou
        }

        try {
            // [CAMADAS] Delega a edição e persistência do aluno para o serviço
            service.editarAluno(aluno.getRa(), novoNome);
            mostrarSucesso("Dados do aluno " + aluno.getRa() + " atualizados com sucesso.");
            atualizarTela();
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Coleta os dados para cadastro de uma nova disciplina e solicita ao serviço a inclusão.
     */
    private void aoNovaDisciplina() {
        String codigo = pedirTexto(
                "Cadastrar Nova Disciplina",
                "Informe o código da disciplina (ex: WEB501):",
                null
        );
        if (codigo == null) {
            return; // Usuário cancelou
        }

        String nome = pedirTexto(
                "Cadastrar Nova Disciplina",
                "Informe o nome da disciplina:",
                null
        );
        if (nome == null) {
            return; // Usuário cancelou
        }

        Integer limiteVagas = pedirInteiro(
                "Cadastrar Nova Disciplina",
                "Informe o limite de vagas (número inteiro positivo):",
                null
        );
        if (limiteVagas == null) {
            return; // Usuário cancelou
        }

        try {
            // [CAMADAS] Delega o cadastro e a persistência da disciplina para o serviço
            Disciplina novaDisciplina = service.cadastrarDisciplina(codigo, nome, limiteVagas);
            mostrarSucesso("Disciplina cadastrada com sucesso: " + novaDisciplina.getCodigo());
            atualizarTela();
            comboDisciplinas.setSelectedItem(novaDisciplina);
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Coleta o novo nome e novo limite de vagas para a disciplina selecionada e solicita a alteração ao serviço.
     */
    private void aoEditarDisciplina() {
        Disciplina disciplina = (Disciplina) comboDisciplinas.getSelectedItem();
        if (disciplina == null) {
            mostrarErro("Selecione uma disciplina para editar.");
            return;
        }

        String novoNome = pedirTexto(
                "Editar Disciplina",
                "Informe o novo nome para a disciplina (" + disciplina.getCodigo() + "):",
                disciplina.getNome()
        );
        if (novoNome == null) {
            return; // Usuário cancelou
        }

        Integer novoLimite = pedirInteiro(
                "Editar Disciplina",
                "Informe o novo limite de vagas para a disciplina (mínimo atual ocupado: " + disciplina.getVagasOcupadas() + "):",
                String.valueOf(disciplina.getLimiteVagas())
        );
        if (novoLimite == null) {
            return; // Usuário cancelou
        }

        try {
            // [CAMADAS] Delega a edição e persistência da disciplina para o serviço
            service.editarDisciplina(disciplina.getCodigo(), novoNome, novoLimite);
            mostrarSucesso("Dados da disciplina " + disciplina.getCodigo() + " atualizados com sucesso.");
            atualizarTela();
        } catch (MatriculaException ex) {
            mostrarErro(ex.getMessage());
        }
    }

    /**
     * Solicita um texto ao usuário em laço de repetição,
     * impedindo o cancelamento abrupto da operação por entrada em branco ou acidental.
     *
     * @param titulo o título da janela de diálogo
     * @param mensagem a orientação descritiva do campo
     * @param valorInicial valor sugerido/atual pré-preenchido (ou null)
     * @return o texto válido informado (com espaços podados), ou {@code null} se o usuário cancelar
     */
    private String pedirTexto(String titulo, String mensagem, String valorInicial) {
        String valorAtual = valorInicial != null ? valorInicial : "";
        while (true) {
            String entrada = (String) JOptionPane.showInputDialog(
                    this,
                    mensagem,
                    titulo,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    valorAtual
            );
            if (entrada == null) {
                return null; // Usuário cancelou ou fechou a janela
            }
            if (!entrada.isBlank()) {
                return entrada.trim();
            }
            JOptionPane.showMessageDialog(
                    this,
                    "O campo não pode ser deixado em branco. Por favor, informe um valor válido.",
                    "Aviso — Entrada Obrigatória",
                    JOptionPane.WARNING_MESSAGE
            );
            valorAtual = entrada;
        }
    }

    /**
     * Solicita um número inteiro positivo ao usuário em laço de repetição,
     * impedindo o cancelamento abrupto da operação caso o usuário digite texto ou valor menor/igual a zero.
     *
     * @param titulo o título da janela de diálogo
     * @param mensagem a orientação descritiva do campo
     * @param valorInicial valor sugerido/atual pré-preenchido (ou null)
     * @return o número inteiro positivo informado, ou {@code null} se o usuário cancelar
     */
    private Integer pedirInteiro(String titulo, String mensagem, String valorInicial) {
        String valorAtual = valorInicial != null ? valorInicial : "";
        while (true) {
            String entrada = (String) JOptionPane.showInputDialog(
                    this,
                    mensagem,
                    titulo,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    valorAtual
            );
            if (entrada == null) {
                return null; // Usuário cancelou ou fechou a janela
            }
            try {
                int valor = Integer.parseInt(entrada.trim());
                if (valor > 0) {
                    return valor;
                }
                JOptionPane.showMessageDialog(
                        this,
                        "O valor deve ser um número inteiro maior que zero.",
                        "Aviso — Valor Inválido",
                        JOptionPane.WARNING_MESSAGE
                );
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Valor inválido: \"" + entrada + "\". Por favor, digite apenas números inteiros positivos.",
                        "Aviso — Número Inválido",
                        JOptionPane.WARNING_MESSAGE
                );
            }
            valorAtual = entrada;
        }
    }

    /**
     * Confirma e executa a remoção do aluno selecionado no combo.
     */
    private void aoRemoverAluno() {
        Aluno aluno = (Aluno) comboAlunos.getSelectedItem();
        if (aluno == null) {
            mostrarErro("Selecione um aluno para remover.");
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente remover o aluno " + aluno.getNome() + " (RA: " + aluno.getRa() + ")?",
                "Confirmar Remoção de Aluno",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmacao == JOptionPane.YES_OPTION) {
            try {
                // [CAMADAS] Delega a remoção do aluno para o serviço
                service.removerAluno(aluno.getRa());
                mostrarSucesso("Aluno " + aluno.getNome() + " removido com sucesso.");
                atualizarTela();
            } catch (MatriculaException ex) {
                mostrarErro(ex.getMessage());
            }
        }
    }

    /**
     * Confirma e executa a remoção da disciplina selecionada no combo.
     */
    private void aoRemoverDisciplina() {
        Disciplina disciplina = (Disciplina) comboDisciplinas.getSelectedItem();
        if (disciplina == null) {
            mostrarErro("Selecione uma disciplina para remover.");
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente remover a disciplina " + disciplina.getNome() + " (" + disciplina.getCodigo() + ")?",
                "Confirmar Remoção de Disciplina",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmacao == JOptionPane.YES_OPTION) {
            try {
                // [CAMADAS] Delega a remoção da disciplina para o serviço
                service.removerDisciplina(disciplina.getCodigo());
                mostrarSucesso("Disciplina " + disciplina.getCodigo() + " removida com sucesso.");
                atualizarTela();
            } catch (MatriculaException ex) {
                mostrarErro(ex.getMessage());
            }
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
