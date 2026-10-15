package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aluno — Representa o aluno no sistema acadêmico e mantém o histórico das suas matrículas.
 *
 * <p>Conceito de POO demonstrado: Associação bidirecional 1 para 0..* com {@link Matricula} e Encapsulamento.</p>
 *
 * <p>Colabora com: {@link Matricula}, {@link Disciplina}. Invariantes que ajuda a manter: I1, I4.</p>
 */
public final class Aluno {

    // [ENCAPSULAMENTO] Atributos privados e imutáveis após a construção
    private final String ra;
    private final String nome;
    private final List<Matricula> matriculas;

    /**
     * Construtor da classe Aluno.
     *
     * @param ra o Registro Acadêmico único do aluno (não nulo e não vazio)
     * @param nome o nome completo do aluno (não nulo e não vazio)
     * @throws IllegalArgumentException se o RA ou o nome forem nulos ou vazios
     */
    public Aluno(String ra, String nome) {
        if (ra == null || ra.isBlank()) {
            throw new IllegalArgumentException("RA não pode ser nulo ou vazio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome não pode ser nulo ou vazio.");
        }
        this.ra = ra.trim();
        this.nome = nome.trim();
        this.matriculas = new ArrayList<>();
    }

    /**
     * Obtém o Registro Acadêmico do aluno.
     *
     * @return o RA do aluno
     */
    public String getRa() {
        return ra;
    }

    /**
     * Obtém o nome do aluno.
     *
     * @return o nome do aluno
     */
    public String getNome() {
        return nome;
    }

    /**
     * Obtém a lista de todas as matrículas (ativas e canceladas) do aluno.
     *
     * @return uma lista não modificável das matrículas
     */
    public List<Matricula> getMatriculas() {
        // [ENCAPSULAMENTO] Protege a lista interna contra modificações externas indevidas
        return Collections.unmodifiableList(matriculas);
    }

    /**
     * Retorna a lista de disciplinas nas quais o aluno possui matrícula ativa.
     *
     * @return lista de disciplinas ativas na ordem em que foram matriculadas
     */
    public List<Disciplina> getDisciplinasAtivas() {
        // [ASSOCIAÇÃO] Navega pelas matrículas para obter as disciplinas associadas ativas
        List<Disciplina> ativas = new ArrayList<>();
        for (Matricula matricula : matriculas) {
            if (matricula.isAtiva()) {
                ativas.add(matricula.getDisciplina());
            }
        }
        return Collections.unmodifiableList(ativas);
    }

    /**
     * Busca uma matrícula ativa do aluno em determinada disciplina.
     *
     * @param disciplina a disciplina a ser consultada
     * @return um {@link Optional} contendo a matrícula ativa, se existir
     * @throws IllegalArgumentException se a disciplina informada for nula
     */
    public Optional<Matricula> buscarMatriculaAtivaEm(Disciplina disciplina) {
        if (disciplina == null) {
            throw new IllegalArgumentException("Disciplina não pode ser nula.");
        }
        for (Matricula matricula : matriculas) {
            if (matricula.isAtiva() && matricula.getDisciplina().equals(disciplina)) {
                return Optional.of(matricula);
            }
        }
        return Optional.empty();
    }

    /**
     * Verifica se o aluno já possui matrícula ativa na disciplina informada.
     *
     * @param disciplina a disciplina a ser verificada
     * @return {@code true} se houver matrícula ativa, caso contrário {@code false}
     */
    public boolean possuiMatriculaAtivaEm(Disciplina disciplina) {
        // [CONSISTÊNCIA] I4: Facilita a checagem de unicidade da matrícula ativa
        return buscarMatriculaAtivaEm(disciplina).isPresent();
    }

    /**
     * Vincula uma matrícula à lista interna do aluno.
     * Método com visibilidade de pacote (package-private), restrito à orquestração
     * atômica pela classe associativa {@link Matricula}.
     *
     * @param matricula a matrícula a ser adicionada
     * @throws IllegalArgumentException se a matrícula for nula
     */
    void adicionarMatricula(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("Matrícula não pode ser nula.");
        }
        // [ASSOCIAÇÃO] Registro interno do vínculo mantido pelo Aluno
        matriculas.add(matricula);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Aluno aluno = (Aluno) o;
        return Objects.equals(ra, aluno.ra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ra);
    }

    @Override
    public String toString() {
        return ra + " - " + nome;
    }
}

