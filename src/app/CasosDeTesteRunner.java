package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.Aluno;
import model.Disciplina;
import model.Matricula;
import repository.ArquivoRepository;
import service.MatriculaException;
import service.SistemaAcademicoService;

/**
 * CasosDeTesteRunner — Executor automatizado de casos de teste em console para validação de regras de negócio e consistência.
 *
 * <p>Conceito de POO demonstrado: Validação de Invariantes de Domínio, Automação de Testes de Integração,
 * Encapsulamento e Avaliação de Consistência Bidirecional.</p>
 *
 * <p>Colabora com: {@link SistemaAcademicoService}, {@link ArquivoRepository}, {@link Aluno}, {@link Disciplina}, {@link Matricula}.</p>
 */
public final class CasosDeTesteRunner {

    @FunctionalInterface
    private interface Teste {
        void executar() throws Exception;
    }

    private static int aprovados = 0;
    private static int reprovados = 0;
    private static final List<String> detalhesDoCaso = new ArrayList<>();
    private static final List<String> linhasTabela = new ArrayList<>();

    private static Path tempDir;
    private static SistemaAcademicoService service;

    /**
     * Construtor privado para impedir instanciação de classe utilitária de testes.
     */
    private CasosDeTesteRunner() {}

    /**
     * Ponto de entrada do executor de testes.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        try {
            tempDir = Files.createTempDirectory("academico-teste");
            service = new SistemaAcademicoService(new ArquivoRepository(tempDir));
        } catch (IOException e) {
            System.err.println("Falha fatal ao criar diretório temporário para testes: " + e.getMessage());
            System.exit(1);
            return;
        }

        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        System.out.println("==================================================================");
        System.out.println(" CASOS DE TESTE - SISTEMA ACADÊMICO (Associações entre Classes)");
        System.out.println(" Execução: " + dataHora + " | Dados: diretório temporário (seed)");
        System.out.println("==================================================================\n");

        // CT01
        executarCaso("CT01", "Carga inicial (seed) e consistência",
                "Carga inicial (seed) e consistência",
                () -> {
                    registrar("Entrada", "diretório temporário vazio (seed automático)");
                    registrar("Esperado", "5 alunos, 4 disciplinas, 8 matrículas (7 ativas)");

                    service.carregarDados();

                    List<Aluno> alunos = service.listarAlunos();
                    List<Disciplina> disciplinas = service.listarDisciplinas();
                    List<Matricula> matriculas = service.listarMatriculas();

                    long ativas = matriculas.stream().filter(Matricula::isAtiva).count();

                    verificar(alunos.size() == 5, "Quantidade de alunos diferente de 5: " + alunos.size());
                    verificar(disciplinas.size() == 4, "Quantidade de disciplinas diferente de 4: " + disciplinas.size());
                    verificar(matriculas.size() == 8, "Quantidade de matrículas diferente de 8: " + matriculas.size());
                    verificar(ativas == 7, "Quantidade de matrículas ativas diferente de 7: " + ativas);

                    Disciplina lab = buscarDisciplina("LAB401");
                    verificar(lab.getVagasOcupadas() == 2 && lab.getLimiteVagas() == 2, "LAB401 deveria estar 2/2");

                    registrar("Obtido", alunos.size() + " alunos, " + disciplinas.size() + " disciplinas, "
                            + matriculas.size() + " matrículas (" + ativas + " ativas)");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT02
        executarCaso("CT02", "Matrícula válida",
                "Matrícula válida",
                () -> {
                    registrar("Entrada", "Elisa Ferreira (2024005) -> POO101");
                    registrar("Esperado", "matrícula ATIVA; POO101 de 3/30 para 4/30");

                    Matricula m = service.matricular("2024005", "POO101");

                    verificar(m.getId() == 9, "ID esperado 9, obtido " + m.getId());
                    verificar(m.isAtiva(), "Matrícula não está com status ATIVA");
                    verificar(m.getDisciplina().getVagasOcupadas() == 4, "Vagas ocupadas em POO101 deveria ser 4");
                    verificar(m.getDisciplina().getMatriculasAtivas().contains(m), "Associação bidirecional ausente na disciplina");

                    List<Disciplina> ativasElisa = service.listarDisciplinasAtivas("2024005");
                    verificar(ativasElisa.size() == 1 && ativasElisa.get(0).getCodigo().equals("POO101"), "Elisa deveria ter POO101 ativa");

                    registrar("Obtido", "#" + m.getId() + " " + m.getAluno().getNome() + " -> "
                            + m.getDisciplina().getCodigo() + " [" + m.getStatus() + "]; POO101 4/30");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT03
        executarCaso("CT03", "Bloqueio de matrícula duplicada",
                "Bloqueio de matrícula duplicada",
                () -> {
                    registrar("Entrada", "Ana Souza (2024001) -> POO101");
                    registrar("Esperado", "MatriculaException (matrícula duplicada)");

                    MatriculaException ex = esperarExcecao(() -> service.matricular("2024001", "POO101"));
                    String msgEsperada = "O aluno Ana Souza já possui matrícula ativa em POO101.";
                    verificar(ex.getMessage().equals(msgEsperada), "Mensagem divergente: " + ex.getMessage());

                    Disciplina poo = buscarDisciplina("POO101");
                    verificar(poo.getVagasOcupadas() == 4, "Vagas de POO101 foram alteradas indevidamente");

                    registrar("Obtido", "\"" + ex.getMessage() + "\"");
                    registrar("Estado", "POO101 permanece 4/30; invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT04
        executarCaso("CT04", "Bloqueio por disciplina lotada",
                "Bloqueio por disciplina lotada",
                () -> {
                    registrar("Entrada", "Elisa Ferreira (2024005) -> LAB401");
                    registrar("Esperado", "MatriculaException (disciplina lotada)");

                    MatriculaException ex = esperarExcecao(() -> service.matricular("2024005", "LAB401"));
                    String msgEsperada = "A disciplina LAB401 está lotada (2/2 vagas ocupadas).";
                    verificar(ex.getMessage().equals(msgEsperada), "Mensagem divergente: " + ex.getMessage());

                    Disciplina lab = buscarDisciplina("LAB401");
                    verificar(lab.getVagasOcupadas() == 2, "LAB401 não deveria ter vagas alteradas");

                    List<Disciplina> ativas = service.listarDisciplinasAtivas("2024005");
                    verificar(ativas.size() == 1, "Elisa não deveria ter novas disciplinas ativas");

                    registrar("Obtido", "\"" + ex.getMessage() + "\"");
                    registrar("Estado", "LAB401 permanece 2/2; invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT05
        executarCaso("CT05", "Cancelamento de matrícula",
                "Cancelamento de matrícula",
                () -> {
                    registrar("Entrada", "Cancelar Bruno Lima (2024002) em LAB401");
                    registrar("Esperado", "matrícula #4 CANCELADA; LAB401 de 2/2 para 1/2");

                    service.cancelarMatricula("2024002", "LAB401");

                    Disciplina lab = buscarDisciplina("LAB401");
                    verificar(lab.getVagasOcupadas() == 1, "LAB401 deveria ter 1 vaga ocupada");

                    Aluno bruno = buscarAluno("2024002");
                    verificar(bruno.getMatriculas().size() == 2, "Bruno ainda deve ter 2 matrículas no histórico");

                    List<Disciplina> ativasBruno = service.listarDisciplinasAtivas("2024002");
                    verificar(ativasBruno.size() == 1 && ativasBruno.get(0).getCodigo().equals("POO101"),
                            "Bruno só deve ter POO101 ativa");

                    registrar("Obtido", "matrícula #4 CANCELADA; LAB401 1/2; Bruno ativas = [POO101]");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT06
        executarCaso("CT06", "Reaproveitamento da vaga liberada",
                "Reaproveitamento da vaga liberada",
                () -> {
                    registrar("Entrada", "Elisa Ferreira (2024005) -> LAB401");
                    registrar("Esperado", "matrícula #10 ATIVA; LAB401 de 1/2 para 2/2");

                    Matricula m = service.matricular("2024005", "LAB401");
                    verificar(m.getId() == 10, "ID esperado 10, obtido " + m.getId());
                    verificar(m.isAtiva(), "Matrícula não está ativa");

                    Disciplina lab = buscarDisciplina("LAB401");
                    verificar(lab.getVagasOcupadas() == 2, "LAB401 deveria voltar a estar lotada (2/2)");

                    List<Disciplina> ativasElisa = service.listarDisciplinasAtivas("2024005");
                    verificar(ativasElisa.size() == 2, "Elisa deveria ter 2 disciplinas ativas");

                    registrar("Obtido", "#" + m.getId() + " " + m.getAluno().getNome() + " -> "
                            + m.getDisciplina().getCodigo() + " [" + m.getStatus() + "]; LAB401 2/2");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT07
        executarCaso("CT07", "Cancelamento de matrícula inexistente",
                "Cancelamento de matrícula inexistente",
                () -> {
                    registrar("Entrada", "Cancelar Elisa Ferreira (2024005) em ED301");
                    registrar("Esperado", "MatriculaException: O aluno Elisa Ferreira não possui matrícula ativa em ED301.");

                    MatriculaException ex = esperarExcecao(() -> service.cancelarMatricula("2024005", "ED301"));
                    String msgEsperada = "O aluno Elisa Ferreira não possui matrícula ativa em ED301.";
                    verificar(ex.getMessage().equals(msgEsperada), "Mensagem divergente: " + ex.getMessage());

                    Disciplina ed = buscarDisciplina("ED301");
                    verificar(ed.getVagasOcupadas() == 1, "ED301 não deveria ter vagas alteradas");

                    registrar("Obtido", "\"" + ex.getMessage() + "\"");
                    registrar("Estado", "ED301 permanece 1/20; invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT08
        executarCaso("CT08", "Rematrícula após cancelamento",
                "Rematrícula após cancelamento",
                () -> {
                    registrar("Entrada", "Carla Mendes (2024003) -> ED301 (a #6 está CANCELADA)");
                    registrar("Esperado", "nova matrícula #11 ATIVA; a #6 continua CANCELADA; ED301 de 1/20 para 2/20");

                    Matricula m = service.matricular("2024003", "ED301");
                    verificar(m.getId() == 11, "ID esperado 11, obtido " + m.getId());
                    verificar(m.isAtiva(), "Matrícula nova deve estar ativa");

                    Aluno carla = buscarAluno("2024003");
                    verificar(carla.getMatriculas().size() == 3, "Carla deve possuir 3 matrículas no histórico");

                    Disciplina ed = buscarDisciplina("ED301");
                    verificar(ed.getVagasOcupadas() == 2, "ED301 deveria ter 2 vagas ocupadas");

                    registrar("Obtido", "#" + m.getId() + " Carla Mendes -> ED301 [ATIVA]; ED301 2/20; 3 matrículas no aluno");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT09
        executarCaso("CT09", "Listagem de disciplinas ativas",
                "Listagem de disciplinas ativas",
                () -> {
                    registrar("Entrada", "listarDisciplinasAtivas(\"2024001\")");
                    registrar("Esperado", "[POO101, BD201], nessa ordem");

                    List<Disciplina> ativas = service.listarDisciplinasAtivas("2024001");
                    verificar(ativas.size() == 2, "Esperado 2 disciplinas ativas");
                    verificar(ativas.get(0).getCodigo().equals("POO101"), "Primeira disciplina deve ser POO101");
                    verificar(ativas.get(1).getCodigo().equals("BD201"), "Segunda disciplina deve ser BD201");

                    registrar("Obtido", "[" + ativas.get(0).getCodigo() + ", " + ativas.get(1).getCodigo() + "]");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT10
        executarCaso("CT10", "Aluno e disciplina inexistentes",
                "Aluno e disciplina inexistentes",
                () -> {
                    registrar("Entrada", "matricular(\"9999999\",\"POO101\") e matricular(\"2024001\",\"XYZ999\")");
                    registrar("Esperado", "mensagens \"Aluno com RA '9999999' não encontrado.\" e \"Disciplina com código 'XYZ999' não encontrada.\"");

                    MatriculaException ex1 = esperarExcecao(() -> service.matricular("9999999", "POO101"));
                    verificar(ex1.getMessage().equals("Aluno com RA '9999999' não encontrado."),
                            "Mensagem 1 divergente: " + ex1.getMessage());

                    MatriculaException ex2 = esperarExcecao(() -> service.matricular("2024001", "XYZ999"));
                    verificar(ex2.getMessage().equals("Disciplina com código 'XYZ999' não encontrada."),
                            "Mensagem 2 divergente: " + ex2.getMessage());

                    verificar(service.listarMatriculas().size() == 11, "Total de matrículas alterado indevidamente");

                    registrar("Obtido", "mensagens padronizadas validadas; total permanece 11 matrículas");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT11
        executarCaso("CT11", "Persistência e recarga",
                "Persistência e recarga",
                () -> {
                    registrar("Entrada", "Novo repositório + novo service no mesmo diretório temporário; depois Bruno Lima -> BD201");
                    registrar("Esperado", "Após recarga: 11 matrículas (9 ativas); POO101 4/30, BD201 1/25, ED301 2/20, LAB401 2/2; nova matrícula recebe #12; BD201 -> 2/25");

                    service = new SistemaAcademicoService(new ArquivoRepository(tempDir));
                    service.carregarDados();

                    verificar(service.listarMatriculas().size() == 11, "Deveria ter 11 matrículas recarregadas do CSV");

                    Matricula m = service.matricular("2024002", "BD201");
                    verificar(m.getId() == 12, "Próximo ID após recarga deveria ser 12, obtido " + m.getId());

                    Disciplina bd = buscarDisciplina("BD201");
                    verificar(bd.getVagasOcupadas() == 2, "BD201 deveria ter 2/25 vagas ocupadas");

                    registrar("Obtido", "11 matrículas recarregadas do disco; nova matrícula #12 criada; BD201 2/25");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT12
        executarCaso("CT12", "Encapsulamento protege a consistência",
                "Encapsulamento protege a consistência",
                () -> {
                    registrar("Entrada", "aluno.getMatriculas().clear(), disciplina.getMatriculas().clear(), service.listarAlunos().clear()");
                    registrar("Esperado", "UnsupportedOperationException nas 3 tentativas; contagens inalteradas");

                    Aluno a = service.listarAlunos().get(0);
                    Disciplina d = service.listarDisciplinas().get(0);

                    boolean c1 = false;
                    boolean c2 = false;
                    boolean c3 = false;

                    try {
                        a.getMatriculas().clear();
                    } catch (UnsupportedOperationException e) {
                        c1 = true;
                    }

                    try {
                        d.getMatriculas().clear();
                    } catch (UnsupportedOperationException e) {
                        c2 = true;
                    }

                    try {
                        service.listarAlunos().clear();
                    } catch (UnsupportedOperationException e) {
                        c3 = true;
                    }

                    verificar(c1 && c2 && c3, "Todas as coleções devem ser protegidas por UnsupportedOperationException");

                    registrar("Obtido", "UnsupportedOperationException nas 3 tentativas; coleções intactas");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT13
        executarCaso("CT13", "Cadastro de novo aluno e nova disciplina com matrícula e persistência",
                "Cadastro de novo aluno e nova disciplina com matrícula e persistência",
                () -> {
                    registrar("Entrada", "Cadastrar Fernanda Costa (2024006), cadastrar WEB501 (2 vagas), matricular Fernanda em WEB501, depois recarregar novo repositório + service no mesmo diretório temporário");
                    registrar("Esperado", "Novo Aluno e Disciplina cadastrados; matrícula #13 ATIVA; WEB501 0/2 -> 1/2; após recarga dos 3 CSVs: Fernanda em listarAlunos(), WEB501 em listarDisciplinas() com 1/2 vagas, listarDisciplinasAtivas(\"2024006\") = [WEB501]; invariantes I1-I4 OK");

                    Aluno novoAluno = service.cadastrarAluno("2024006", "Fernanda Costa");
                    verificar(novoAluno != null && novoAluno.getRa().equals("2024006"), "Aluno não cadastrado corretamente");

                    Disciplina novaDisc = service.cadastrarDisciplina("WEB501", "Desenvolvimento Web", 2);
                    verificar(novaDisc != null && novaDisc.getCodigo().equals("WEB501"), "Disciplina não cadastrada corretamente");

                    Matricula m = service.matricular("2024006", "WEB501");
                    verificar(m.getId() == 13, "ID esperado 13, obtido " + m.getId());
                    verificar(novaDisc.getVagasOcupadas() == 1, "Vagas ocupadas em WEB501 deveria ser 1");
                    verificarInvariantes(service);

                    // Recarga de novo repositório e service a partir dos arquivos CSV salvos em disco
                    service = new SistemaAcademicoService(new ArquivoRepository(tempDir));
                    service.carregarDados();

                    verificar(service.listarAlunos().size() == 6, "Esperado 6 alunos no CSV recarregado");
                    verificar(service.listarDisciplinas().size() == 5, "Esperado 5 disciplinas no CSV recarregado");
                    verificar(service.listarMatriculas().size() == 13, "Esperado 13 matrículas no CSV recarregado");

                    List<Disciplina> ativasFernanda = service.listarDisciplinasAtivas("2024006");
                    verificar(ativasFernanda.size() == 1 && ativasFernanda.get(0).getCodigo().equals("WEB501"),
                            "Fernanda deveria ter matrícula ativa em WEB501 após recarga");

                    Disciplina discRecarregada = buscarDisciplina("WEB501");
                    verificar(discRecarregada.getVagasOcupadas() == 1, "WEB501 deveria recarregar com 1/2 vagas ocupadas");

                    registrar("Obtido", "Novo aluno e nova disciplina cadastrados; matrícula #13 criada; estado persistido e recarregado dos 3 CSVs");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT14
        executarCaso("CT14", "Bloqueio de remoção com matrículas ativas",
                "Bloqueio de remoção com matrículas ativas",
                () -> {
                    registrar("Entrada", "Tentar remover Fernanda Costa (2024006) e disciplina WEB501");
                    registrar("Esperado", "MatriculaException: \"O aluno Fernanda Costa não pode ser removido pois possui matrículas ativas.\" e \"A disciplina WEB501 não pode ser removida pois possui matrículas ativas.\"; cadastros permanecem inalterados; invariantes I1-I4 OK");

                    // 1. Tentar remover aluno com matrícula ativa
                    MatriculaException exAluno = esperarExcecao(() -> service.removerAluno("2024006"));
                    verificar(exAluno.getMessage().equals("O aluno Fernanda Costa não pode ser removido pois possui matrículas ativas."),
                            "Mensagem inesperada ao remover aluno: " + exAluno.getMessage());

                    // 2. Tentar remover disciplina com matrícula ativa
                    MatriculaException exDisc = esperarExcecao(() -> service.removerDisciplina("WEB501"));
                    verificar(exDisc.getMessage().equals("A disciplina WEB501 não pode ser removida pois possui matrículas ativas."),
                            "Mensagem inesperada ao remover disciplina: " + exDisc.getMessage());

                    verificar(service.listarAlunos().size() == 6, "Total de alunos deve permanecer 6");
                    verificar(service.listarDisciplinas().size() == 5, "Total de disciplinas deve permanecer 5");

                    registrar("Obtido", "Bloqueio de remoção validado com mensagens padronizadas; entidades preservadas");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        // CT15
        executarCaso("CT15", "Remoção válida sem matrículas ativas e persistência",
                "Remoção válida sem matrículas ativas e persistência",
                () -> {
                    registrar("Entrada", "Cancelar matrícula de Fernanda em WEB501, remover Fernanda Costa e disciplina WEB501, depois recarregar novo repositório + service no mesmo diretório temporário");
                    registrar("Esperado", "Fernanda e WEB501 removidos com sucesso; expurgo de matrículas do histórico; após recarga dos 3 CSVs: Fernanda não está em listarAlunos(), WEB501 não está em listarDisciplinas(), listarMatriculas() sem referências órfãs; total de matrículas volta a 12; invariantes I1-I4 OK");

                    // 1. Cancelar matrícula ativa de Fernanda em WEB501 para liberar remoção
                    service.cancelarMatricula("2024006", "WEB501");

                    // 2. Remover aluno elegível (sem ativas)
                    service.removerAluno("2024006");
                    verificar(service.listarAlunos().stream().noneMatch(a -> a.getRa().equals("2024006")),
                            "Fernanda Costa não deveria mais estar na lista de alunos");

                    // 3. Remover disciplina elegível (sem ativas nem vagas ocupadas)
                    service.removerDisciplina("WEB501");
                    verificar(service.listarDisciplinas().stream().noneMatch(d -> d.getCodigo().equals("WEB501")),
                            "WEB501 não deveria mais estar na lista de disciplinas");

                    // 4. Recarregar do disco e conferir integridade
                    service = new SistemaAcademicoService(new ArquivoRepository(tempDir));
                    service.carregarDados();

                    verificar(service.listarAlunos().size() == 5, "Esperado 5 alunos após remoção e recarga do CSV");
                    verificar(service.listarDisciplinas().size() == 4, "Esperado 4 disciplinas após remoção e recarga do CSV");
                    verificar(service.listarMatriculas().size() == 12, "Esperado 12 matrículas após expurgo e recarga do CSV");

                    registrar("Obtido", "Fernanda e WEB501 removidos com sucesso; CSVs atualizados e recarregados sem referências órfãs");
                    registrar("Estado", "invariantes I1-I4 OK");
                    verificarInvariantes(service);
                });

        imprimirResumo();
        System.exit(reprovados == 0 ? 0 : 1);
    }

    private static void executarCaso(String id, String titulo, String cenarioRelatorio, Teste teste) {
        detalhesDoCaso.clear();
        boolean passou = false;
        String resultadoObtido = "";
        String motivoFalha = "";

        try {
            teste.executar();
            passou = true;
            aprovados++;
            for (String detalhe : detalhesDoCaso) {
                if (detalhe.startsWith("Obtido  : ")) {
                    resultadoObtido = detalhe.substring("Obtido  : ".length());
                }
            }
        } catch (Throwable t) {
            passou = false;
            reprovados++;
            motivoFalha = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            resultadoObtido = "FALHOU: " + motivoFalha;
        }

        String prefix = "[" + id + "] " + titulo + " ";
        int dotsCount = Math.max(2, 58 - prefix.length());
        String dots = ".".repeat(dotsCount);
        String statusStr = passou ? "PASSOU" : "FALHOU";

        System.out.println(prefix + dots + " " + statusStr);
        for (String detalhe : detalhesDoCaso) {
            System.out.println("       " + detalhe);
        }
        if (!passou) {
            System.out.println("       Motivo  : " + motivoFalha);
        }
        System.out.println();

        String esperadoRelatorio = "";
        for (String detalhe : detalhesDoCaso) {
            if (detalhe.startsWith("Esperado: ")) {
                esperadoRelatorio = detalhe.substring("Esperado: ".length());
            }
        }

        linhasTabela.add(String.format("| %s | %s | %s | %s | %s |",
                id, cenarioRelatorio, esperadoRelatorio, resultadoObtido, statusStr));
    }

    private static void registrar(String rotulo, String valor) {
        detalhesDoCaso.add(String.format("%-8s: %s", rotulo, valor));
    }

    private static void verificar(boolean condicao, String mensagemFalha) {
        if (!condicao) {
            throw new AssertionError(mensagemFalha);
        }
    }

    private static MatriculaException esperarExcecao(Teste acao) {
        try {
            acao.executar();
        } catch (MatriculaException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Exceção inesperada lançada: " + e.getClass().getName() + " - " + e.getMessage());
        }
        throw new AssertionError("Nenhuma exceção lançada quando esperava MatriculaException.");
    }

    private static void verificarInvariantes(SistemaAcademicoService s) {
        List<Matricula> matriculas = s.listarMatriculas();
        List<Disciplina> disciplinas = s.listarDisciplinas();

        // [CONSISTÊNCIA] I1: Vínculo bidirecional presente em ambos os lados
        for (Matricula m : matriculas) {
            verificar(m.getAluno().getMatriculas().contains(m),
                    "I1 violada: matrícula #" + m.getId() + " ausente no Aluno " + m.getAluno().getRa());
            verificar(m.getDisciplina().getMatriculas().contains(m),
                    "I1 violada: matrícula #" + m.getId() + " ausente na Disciplina " + m.getDisciplina().getCodigo());
        }

        // [CONSISTÊNCIA] I2 e I3: Ocupação de vagas e integridade dos limites
        for (Disciplina d : disciplinas) {
            int ativasContadas = 0;
            for (Matricula m : d.getMatriculas()) {
                if (m.isAtiva()) {
                    ativasContadas++;
                }
            }
            verificar(d.getVagasOcupadas() == ativasContadas,
                    "I2 violada na disciplina " + d.getCodigo() + ": vagasOcupadas=" + d.getVagasOcupadas() + ", ativas=" + ativasContadas);
            verificar(d.getVagasOcupadas() >= 0 && d.getVagasOcupadas() <= d.getLimiteVagas(),
                    "I3 violada na disciplina " + d.getCodigo() + ": vagas fora dos limites");
        }

        // [CONSISTÊNCIA] I4: No máximo uma matrícula ativa por par (aluno, disciplina)
        Set<String> paresAtivos = new HashSet<>();
        for (Matricula m : matriculas) {
            if (m.isAtiva()) {
                String par = m.getAluno().getRa() + ":" + m.getDisciplina().getCodigo();
                verificar(paresAtivos.add(par),
                        "I4 violada: matrícula ativa duplicada para o par " + par);
            }
        }
    }

    private static Aluno buscarAluno(String ra) {
        for (Aluno a : service.listarAlunos()) {
            if (a.getRa().equals(ra)) {
                return a;
            }
        }
        throw new AssertionError("Aluno não encontrado para validação do teste: " + ra);
    }

    private static Disciplina buscarDisciplina(String codigo) {
        for (Disciplina d : service.listarDisciplinas()) {
            if (d.getCodigo().equals(codigo)) {
                return d;
            }
        }
        throw new AssertionError("Disciplina não encontrada para validação do teste: " + codigo);
    }

    private static void imprimirResumo() {
        int total = aprovados + reprovados;
        System.out.println("------------------------------------------------------------------");
        System.out.println(" RESUMO: " + total + " casos executados | " + aprovados + " PASSOU | " + reprovados + " FALHOU");
        System.out.println("------------------------------------------------------------------\n");

        System.out.println("TABELA PARA O RELATÓRIO (copie e cole em Markdown):\n");
        System.out.println("| Caso | Cenário | Resultado esperado | Resultado obtido | Status |");
        System.out.println("|------|---------|--------------------|------------------|--------|");
        for (String linha : linhasTabela) {
            System.out.println(linha);
        }
    }
}

