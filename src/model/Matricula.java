package model;

import java.time.LocalDate;

/**
 * Matricula — Classe associativa que materializa o relacionamento N:N entre {@link Aluno} e {@link Disciplina}.
 *
 * <p>Conceito de POO demonstrado: Classe Associativa com controle de ciclo de vida,
 * Encapsulamento, Factory Method estático e manutenção de consistência bidirecional.</p>
 *
 * <p>Colabora com: {@link Aluno}, {@link Disciplina}. Invariantes que ajuda a manter: I1, I2, I5.</p>
 */
public final class Matricula {

    /**
     * Enumeração que representa o status atual da matrícula no sistema.
     */
    public enum Status {
        /**
         * Indica que a matrícula está regular e ativa, ocupando uma vaga na disciplina.
         */
        ATIVA,

        /**
         * Indica que a matrícula foi cancelada (soft delete), mantendo o histórico sem ocupar vaga.
         */
        CANCELADA
    }

    // [ENCAPSULAMENTO] Atributos imutáveis do vínculo; apenas o status transiciona de ATIVA para CANCELADA
    private final int id;
    private final Aluno aluno;
    private final Disciplina disciplina;
    private final LocalDate dataMatricula;
    private Status status;

    /**
     * Construtor privado para impedir instanciação direta sem o estabelecimento das associações bidirecionais.
     * Use {@link #criar(int, Aluno, Disciplina, LocalDate)}.
     *
     * @param id identificador único da matrícula
     * @param aluno aluno associado
     * @param disciplina disciplina associada
     * @param dataMatricula data em que a matrícula foi efetuada
     */
    private Matricula(int id, Aluno aluno, Disciplina disciplina, LocalDate dataMatricula) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID da matrícula deve ser maior que zero.");
        }
        if (aluno == null) {
            throw new IllegalArgumentException("Aluno não pode ser nulo.");
        }
        if (disciplina == null) {
            throw new IllegalArgumentException("Disciplina não pode ser nula.");
        }
        if (dataMatricula == null) {
            throw new IllegalArgumentException("Data da matrícula não pode ser nula.");
        }
        this.id = id;
        this.aluno = aluno;
        this.disciplina = disciplina;
        this.dataMatricula = dataMatricula;
        this.status = Status.ATIVA;
    }

    /**
     * Cria e estabelece de forma atômica o vínculo bidirecional entre {@link Aluno} e {@link Disciplina}.
     *
     * @param id identificador único da nova matrícula
     * @param aluno o aluno a ser matriculado
     * @param disciplina a disciplina onde o aluno será matriculado
     * @param data data em que a matrícula foi realizada
     * @return a instância de {@link Matricula} associada
     * @throws IllegalArgumentException se algum argumento for inválido ou nulo
     * @throws IllegalStateException se a disciplina estiver com limite de vagas excedido
     */
    public static Matricula criar(int id, Aluno aluno, Disciplina disciplina, LocalDate data) {
        Matricula matricula = new Matricula(id, aluno, disciplina, data);

        // [CONSISTÊNCIA] I1/I2: Vincula primeiro a disciplina (que valida e ocupa a vaga).
        // Se a disciplina estiver lotada, lança exceção antes de vincular ao aluno, preservando a atomicidade.
        disciplina.adicionarMatricula(matricula);

        // [ASSOCIAÇÃO] Registra a matrícula no aluno após confirmação da vaga
        aluno.adicionarMatricula(matricula);

        return matricula;
    }

    /**
     * Cancela a matrícula ativa, liberando a vaga na disciplina e mantendo o registro para histórico.
     *
     * @throws IllegalStateException se a matrícula já estiver cancelada
     */
    public void cancelar() {
        if (status == Status.CANCELADA) {
            throw new IllegalStateException("A matrícula #" + id + " já está cancelada.");
        }
        // [CONSISTÊNCIA] I2/I5: Altera o status e libera a vaga na disciplina associada de forma coordenada
        this.status = Status.CANCELADA;
        disciplina.liberarVaga();
    }

    /**
     * Informa se a matrícula se encontra atualmente ativa.
     *
     * @return {@code true} se o status for ATIVA, caso contrário {@code false}
     */
    public boolean isAtiva() {
        return status == Status.ATIVA;
    }

    /**
     * Obtém o identificador numérico da matrícula.
     *
     * @return o id da matrícula
     */
    public int getId() {
        return id;
    }

    /**
     * Obtém o aluno vinculado a esta matrícula.
     *
     * @return o aluno
     */
    public Aluno getAluno() {
        return aluno;
    }

    /**
     * Obtém a disciplina vinculada a esta matrícula.
     *
     * @return a disciplina
     */
    public Disciplina getDisciplina() {
        return disciplina;
    }

    /**
     * Obtém a data de realização da matrícula.
     *
     * @return a data da matrícula
     */
    public LocalDate getDataMatricula() {
        return dataMatricula;
    }

    /**
     * Obtém o status atual da matrícula (ATIVA ou CANCELADA).
     *
     * @return o status da matrícula
     */
    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "#" + id + " " + aluno.getNome() + " -> " + disciplina.getCodigo() + " [" + status + "]";
    }
}

