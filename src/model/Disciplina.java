package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Disciplina — Representa a disciplina ofertada, controla o limite e ocupação de vagas e conhece suas matrículas.
 *
 * <p>Conceito de POO demonstrado: Associação 1 para 0..* com {@link Matricula}, Encapsulamento e Defesa de Invariantes.</p>
 *
 * <p>Colabora com: {@link Matricula}, {@link Aluno}. Invariantes que ajuda a manter: I1, I2, I3.</p>
 */
public final class Disciplina {

    // [ENCAPSULAMENTO] Atributos protegidos; código e matriculas imutáveis; nome e limiteVagas alteráveis via métodos de negócio
    private final String codigo;
    private String nome;
    private int limiteVagas;
    private int vagasOcupadas;
    private final List<Matricula> matriculas;

    /**
     * Construtor da classe Disciplina.
     *
     * @param codigo o código identificador da disciplina (ex.: "POO101")
     * @param nome o nome descritivo da disciplina
     * @param limiteVagas a capacidade máxima de vagas (deve ser maior que zero)
     * @throws IllegalArgumentException se código ou nome forem nulos/vazios, ou se limiteVagas for <= 0
     */
    public Disciplina(String codigo, String nome, int limiteVagas) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código da disciplina não pode ser nulo ou vazio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da disciplina não pode ser nulo ou vazio.");
        }
        if (limiteVagas <= 0) {
            throw new IllegalArgumentException("Limite de vagas deve ser maior que zero.");
        }
        this.codigo = codigo.trim();
        this.nome = nome.trim();
        this.limiteVagas = limiteVagas;
        this.vagasOcupadas = 0;
        this.matriculas = new ArrayList<>();
    }

    /**
     * Obtém o código da disciplina.
     *
     * @return o código da disciplina
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Obtém o nome da disciplina.
     *
     * @return o nome da disciplina
     */
    public String getNome() {
        return nome;
    }

    /**
     * Altera o nome da disciplina.
     * Método com semântica de negócio para atualização cadastral da disciplina.
     *
     * @param novoNome o novo nome descritivo da disciplina (não nulo e não em branco)
     * @throws IllegalArgumentException se o novo nome for nulo ou vazio
     */
    public void alterarNome(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("Nome da disciplina não pode ser nulo ou vazio.");
        }
        // [ENCAPSULAMENTO] Atualização segura do atributo descritivo
        this.nome = novoNome.trim();
    }

    /**
     * Obtém o limite total de vagas oferecidas pela disciplina.
     *
     * @return o limite de vagas
     */
    public int getLimiteVagas() {
        return limiteVagas;
    }

    /**
     * Altera o limite total de vagas oferecidas pela disciplina.
     * Método com semântica de negócio para redefinição de capacidade da turma.
     *
     * @param novoLimite o novo limite de vagas (deve ser maior que zero e >= vagasOcupadas)
     * @throws IllegalArgumentException se o novo limite for menor ou igual a zero
     * @throws IllegalStateException se o novo limite for menor que as vagas já ocupadas (defesa da invariante I3)
     */
    public void alterarLimiteVagas(int novoLimite) {
        if (novoLimite <= 0) {
            throw new IllegalArgumentException("Limite de vagas deve ser maior que zero.");
        }
        // [CONSISTÊNCIA] I3: Defesa interna de último nível que impede redução abaixo do número de matriculados
        if (novoLimite < vagasOcupadas) {
            throw new IllegalStateException("O novo limite de vagas (" + novoLimite 
                    + ") não pode ser menor que o total de vagas já ocupadas (" + vagasOcupadas + ").");
        }
        this.limiteVagas = novoLimite;
    }

    /**
     * Obtém a quantidade de vagas atualmente ocupadas por matrículas ativas.
     *
     * @return a quantidade de vagas ocupadas
     */
    public int getVagasOcupadas() {
        return vagasOcupadas;
    }

    /**
     * Calcula o número de vagas ainda disponíveis na disciplina.
     *
     * @return o saldo de vagas disponíveis
     */
    public int getVagasDisponiveis() {
        return limiteVagas - vagasOcupadas;
    }

    /**
     * Verifica se a disciplina ainda possui vagas disponíveis para novas matrículas.
     *
     * @return {@code true} se houver vagas disponíveis, {@code false} se estiver lotada
     */
    public boolean temVagas() {
        // [CONSISTÊNCIA] I3: Avaliação de capacidade com base no limite
        return vagasOcupadas < limiteVagas;
    }

    /**
     * Retorna a lista de todas as matrículas vinculadas a esta disciplina (ativas e canceladas).
     *
     * @return lista não modificável de matrículas
     */
    public List<Matricula> getMatriculas() {
        // [ENCAPSULAMENTO] Lista somente-leitura para preservar a integridade interna
        return Collections.unmodifiableList(matriculas);
    }

    /**
     * Retorna apenas as matrículas com status ATIVA nesta disciplina.
     *
     * @return lista não modificável de matrículas ativas
     */
    public List<Matricula> getMatriculasAtivas() {
        List<Matricula> ativas = new ArrayList<>();
        for (Matricula matricula : matriculas) {
            if (matricula.isAtiva()) {
                ativas.add(matricula);
            }
        }
        return Collections.unmodifiableList(ativas);
    }

    /**
     * Adiciona uma matrícula à disciplina e ocupa a respectiva vaga.
     * Método com visibilidade de pacote (package-private), invocado exclusivamente
     * pela factory {@link Matricula#criar(int, Aluno, Disciplina, java.time.LocalDate)}.
     *
     * @param matricula a matrícula a ser associada à disciplina
     * @throws IllegalArgumentException se a matrícula for nula
     * @throws IllegalStateException se a disciplina estiver lotada
     */
    void adicionarMatricula(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("Matrícula não pode ser nula.");
        }
        // [CONSISTÊNCIA] I2/I3: Primeiro ocupa a vaga; se lotada, lança exceção antes de registrar o vínculo
        ocuparVaga();
        // [ASSOCIAÇÃO] Registra a matrícula na lista da disciplina
        matriculas.add(matricula);
    }

    /**
     * Incrementa o contador de vagas ocupadas como defesa interna de integridade.
     *
     * @throws IllegalStateException se não houver mais vagas
     */
    private void ocuparVaga() {
        if (vagasOcupadas >= limiteVagas) {
            throw new IllegalStateException("Disciplina " + codigo + " está lotada (" + vagasOcupadas + "/" + limiteVagas + ").");
        }
        vagasOcupadas++;
    }

    /**
     * Libera uma vaga ocupada quando uma matrícula ativa é cancelada.
     * Método com visibilidade de pacote (package-private), invocado por {@link Matricula#cancelar()}.
     *
     * @throws IllegalStateException se não houver vagas ocupadas para liberar
     */
    void liberarVaga() {
        // [CONSISTÊNCIA] I2/I3: Decrementa vaga ocupada mantendo invariante I2
        if (vagasOcupadas <= 0) {
            throw new IllegalStateException("Não há vagas ocupadas para liberar na disciplina " + codigo + ".");
        }
        vagasOcupadas--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Disciplina that = (Disciplina) o;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + nome + " (" + vagasOcupadas + "/" + limiteVagas + ")";
    }
}

