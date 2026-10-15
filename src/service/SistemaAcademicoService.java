package service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.Aluno;
import model.Disciplina;
import model.Matricula;
import repository.ArquivoRepository;

/**
 * SistemaAcademicoService — Fachada de serviços que coordena as regras de negócio acadêmicas.
 *
 * <p>Conceito de POO demonstrado: Padrão Fachada (Facade), Separação em Camadas,
 * Imutabilidade de Coleções Expostas e Validação Estrita de Invariantes de Negócio.</p>
 *
 * <p>Colabora com: {@link Aluno}, {@link Disciplina}, {@link Matricula}, {@link ArquivoRepository}.
 * Invariantes que ajuda a manter: I1 a I6.</p>
 */
public final class SistemaAcademicoService {

    // [CAMADAS] Dependência injetada da camada de persistência
    private final ArquivoRepository repositorio;

    // [ENCAPSULAMENTO] Mapas internos preservam ordem de inserção (LinkedHashMap)
    private final Map<String, Aluno> alunosPorRa;
    private final Map<String, Disciplina> disciplinasPorCodigo;
    private final List<Matricula> matriculas;
    private int proximoIdMatricula;

    /**
     * Construtor da camada de serviço.
     *
     * @param repositorio repositório para acesso a arquivos CSV
     * @throws IllegalArgumentException se o repositório for nulo
     */
    public SistemaAcademicoService(ArquivoRepository repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("Repositório não pode ser nulo.");
        }
        this.repositorio = repositorio;
        this.alunosPorRa = new LinkedHashMap<>();
        this.disciplinasPorCodigo = new LinkedHashMap<>();
        this.matriculas = new ArrayList<>();
        this.proximoIdMatricula = 1;
    }

    /**
     * Carrega todos os registros do repositório em memória e inicializa o gerador de IDs.
     *
     * @throws IOException se houver erro de leitura nos arquivos
     */
    public void carregarDados() throws IOException {
        // [CAMADAS] Solicita carga dos dados ao repositório
        if (!repositorio.existemDados()) {
            repositorio.gerarSeed();
        }

        alunosPorRa.clear();
        for (Aluno aluno : repositorio.carregarAlunos()) {
            alunosPorRa.put(aluno.getRa(), aluno);
        }

        disciplinasPorCodigo.clear();
        for (Disciplina disciplina : repositorio.carregarDisciplinas()) {
            disciplinasPorCodigo.put(disciplina.getCodigo(), disciplina);
        }

        matriculas.clear();
        List<Matricula> carregadas = repositorio.carregarMatriculas(alunosPorRa, disciplinasPorCodigo);
        matriculas.addAll(carregadas);

        // [CONSISTÊNCIA] I6: Determina o próximo ID de matrícula sequencial
        int maiorId = 0;
        for (Matricula matricula : matriculas) {
            if (matricula.getId() > maiorId) {
                maiorId = matricula.getId();
            }
        }
        this.proximoIdMatricula = maiorId + 1;
    }

    /**
     * Cadastra um novo aluno no sistema.
     *
     * @param ra o Registro Acadêmico único do aluno
     * @param nome o nome completo do aluno
     * @return o aluno cadastrado
     * @throws MatriculaException se os dados forem inválidos ou se o RA já estiver cadastrado
     */
    public Aluno cadastrarAluno(String ra, String nome) throws MatriculaException {
        // [REGRA DE NEGÓCIO] Validação de campos obrigatórios
        if (ra == null || ra.isBlank()) {
            throw new MatriculaException("O RA do aluno não pode ser nulo ou vazio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new MatriculaException("O nome do aluno não pode ser nulo ou vazio.");
        }

        String raTrim = ra.trim();
        String nomeTrim = nome.trim();

        // [REGRA DE NEGÓCIO] Bloqueio de duplicidade de RA
        if (alunosPorRa.containsKey(raTrim)) {
            throw new MatriculaException("Aluno com RA '" + raTrim + "' já está cadastrado.");
        }

        Aluno novoAluno = new Aluno(raTrim, nomeTrim);
        alunosPorRa.put(raTrim, novoAluno);
        persistirAlunos();
        return novoAluno;
    }

    /**
     * Cadastra uma nova disciplina ofertada no sistema.
     *
     * @param codigo código identificador da disciplina (ex: "WEB501")
     * @param nome nome descritivo da disciplina
     * @param limiteVagas capacidade máxima de alunos na disciplina
     * @return a disciplina cadastrada
     * @throws MatriculaException se os dados forem inválidos, vagas <= 0 ou código duplicado
     */
    public Disciplina cadastrarDisciplina(String codigo, String nome, int limiteVagas) throws MatriculaException {
        // [REGRA DE NEGÓCIO] Validação dos parâmetros da disciplina
        if (codigo == null || codigo.isBlank()) {
            throw new MatriculaException("O código da disciplina não pode ser nulo ou vazio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new MatriculaException("O nome da disciplina não pode ser nulo ou vazio.");
        }
        if (limiteVagas <= 0) {
            throw new MatriculaException("O limite de vagas deve ser maior que zero.");
        }

        String codigoTrim = codigo.trim();
        String nomeTrim = nome.trim();

        // [REGRA DE NEGÓCIO] Bloqueio de código de disciplina duplicado
        if (disciplinasPorCodigo.containsKey(codigoTrim)) {
            throw new MatriculaException("Disciplina com código '" + codigoTrim + "' já está cadastrada.");
        }

        Disciplina novaDisciplina = new Disciplina(codigoTrim, nomeTrim, limiteVagas);
        disciplinasPorCodigo.put(codigoTrim, novaDisciplina);
        persistirDisciplinas();
        return novaDisciplina;
    }

    /**
     * Realiza a matrícula de um aluno em uma disciplina com validação de regras de negócio.
     *
     * @param ra o RA do aluno a ser matriculado
     * @param codigoDisciplina o código da disciplina desejada
     * @return a matrícula criada e vinculada
     * @throws MatriculaException se o aluno/disciplina não existirem, se for duplicada ou se não houver vagas
     */
    public Matricula matricular(String ra, String codigoDisciplina) throws MatriculaException {
        // 1. Validar existência do aluno
        Aluno aluno = buscarAluno(ra);

        // 2. Validar existência da disciplina
        Disciplina disciplina = buscarDisciplina(codigoDisciplina);

        // 3. [REGRA DE NEGÓCIO] Bloqueio de matrícula duplicada (invariante I4)
        if (aluno.possuiMatriculaAtivaEm(disciplina)) {
            throw new MatriculaException("O aluno " + aluno.getNome() + " já possui matrícula ativa em " + disciplina.getCodigo() + ".");
        }

        // 4. [REGRA DE NEGÓCIO] Bloqueio de disciplina lotada (invariante I3)
        if (!disciplina.temVagas()) {
            throw new MatriculaException("A disciplina " + disciplina.getCodigo() + " está lotada ("
                    + disciplina.getVagasOcupadas() + "/" + disciplina.getLimiteVagas() + " vagas ocupadas).");
        }

        // [ASSOCIAÇÃO] Criação da classe associativa e ligação bidirecional coordenada
        Matricula matricula = Matricula.criar(proximoIdMatricula++, aluno, disciplina, LocalDate.now());
        matriculas.add(matricula);

        persistirMatriculas();
        return matricula;
    }

    /**
     * Cancela uma matrícula ativa existente, liberando a vaga na disciplina.
     *
     * @param ra o RA do aluno
     * @param codigoDisciplina o código da disciplina
     * @throws MatriculaException se o aluno ou disciplina não existirem, ou se não houver matrícula ativa
     */
    public void cancelarMatricula(String ra, String codigoDisciplina) throws MatriculaException {
        Aluno aluno = buscarAluno(ra);
        Disciplina disciplina = buscarDisciplina(codigoDisciplina);

        Optional<Matricula> matriculaAtiva = aluno.buscarMatriculaAtivaEm(disciplina);
        if (matriculaAtiva.isEmpty()) {
            throw new MatriculaException("O aluno " + aluno.getNome() + " não possui matrícula ativa em " + disciplina.getCodigo() + ".");
        }

        // [CONSISTÊNCIA] I2/I5: Altera status para CANCELADA e decrementa vaga na disciplina associada
        matriculaAtiva.get().cancelar();
        persistirMatriculas();
    }

    /**
     * Lista as disciplinas com matrículas ativas de um aluno.
     *
     * @param ra o RA do aluno
     * @return lista de disciplinas ativas na ordem de cadastro da matrícula
     * @throws MatriculaException se o aluno não for encontrado
     */
    public List<Disciplina> listarDisciplinasAtivas(String ra) throws MatriculaException {
        Aluno aluno = buscarAluno(ra);
        return aluno.getDisciplinasAtivas();
    }

    /**
     * Retorna uma cópia imutável de todos os alunos cadastrados.
     *
     * @return lista não modificável de alunos
     */
    public List<Aluno> listarAlunos() {
        // [ENCAPSULAMENTO] Protege a coleção interna contra modificações externas
        return List.copyOf(alunosPorRa.values());
    }

    /**
     * Retorna uma cópia imutável de todas as disciplinas cadastradas.
     *
     * @return lista não modificável de disciplinas
     */
    public List<Disciplina> listarDisciplinas() {
        // [ENCAPSULAMENTO] Protege a coleção interna contra modificações externas
        return List.copyOf(disciplinasPorCodigo.values());
    }

    /**
     * Retorna uma cópia imutável do registro geral de matrículas.
     *
     * @return lista não modificável de matrículas
     */
    public List<Matricula> listarMatriculas() {
        // [ENCAPSULAMENTO] Protege a coleção interna contra modificações externas
        return List.copyOf(matriculas);
    }

    /**
     * Busca aluno por RA ou lança exceção com mensagem padronizada.
     */
    private Aluno buscarAluno(String ra) throws MatriculaException {
        if (ra == null || ra.isBlank()) {
            throw new MatriculaException("Aluno com RA '" + ra + "' não encontrado.");
        }
        Aluno aluno = alunosPorRa.get(ra.trim());
        if (aluno == null) {
            throw new MatriculaException("Aluno com RA '" + ra + "' não encontrado.");
        }
        return aluno;
    }

    /**
     * Busca disciplina por código ou lança exceção com mensagem padronizada.
     */
    private Disciplina buscarDisciplina(String codigo) throws MatriculaException {
        if (codigo == null || codigo.isBlank()) {
            throw new MatriculaException("Disciplina com código '" + codigo + "' não encontrada.");
        }
        Disciplina disciplina = disciplinasPorCodigo.get(codigo.trim());
        if (disciplina == null) {
            throw new MatriculaException("Disciplina com código '" + codigo + "' não encontrada.");
        }
        return disciplina;
    }

    /**
     * Persiste as matrículas via repositório tratando falhas de I/O.
     */
    private void persistirMatriculas() throws MatriculaException {
        // [CAMADAS] Delega a gravação ao repositório
        try {
            repositorio.salvarMatriculas(matriculas);
        } catch (IOException e) {
            throw new MatriculaException("Falha ao salvar os dados: " + e.getMessage(), e);
        }
    }

    /**
     * Persiste os alunos via repositório tratando falhas de I/O.
     */
    private void persistirAlunos() throws MatriculaException {
        // [CAMADAS] Delega a gravação ao repositório
        try {
            repositorio.salvarAlunos(alunosPorRa.values());
        } catch (IOException e) {
            throw new MatriculaException("Falha ao salvar os dados: " + e.getMessage(), e);
        }
    }

    /**
     * Persiste as disciplinas via repositório tratando falhas de I/O.
     */
    private void persistirDisciplinas() throws MatriculaException {
        // [CAMADAS] Delega a gravação ao repositório
        try {
            repositorio.salvarDisciplinas(disciplinasPorCodigo.values());
        } catch (IOException e) {
            throw new MatriculaException("Falha ao salvar os dados: " + e.getMessage(), e);
        }
    }
}

