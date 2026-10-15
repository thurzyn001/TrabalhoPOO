package service;

/**
 * MatriculaException — Exceção verificada para sinalizar violações de regras de negócio do sistema acadêmico.
 *
 * <p>Conceito de POO demonstrado: Tratamento de Exceções Customizadas (Checked Exceptions),
 * obrigando a camada de apresentação a lidar explicitamente com falhas de negócio.</p>
 *
 * <p>Colabora com: {@link SistemaAcademicoService}. Invariantes que ajuda a manter: I1 a I5.</p>
 */
public final class MatriculaException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constrói a exceção com uma mensagem descritiva.
     *
     * @param mensagem a mensagem em português detalhando a regra violada
     */
    public MatriculaException(String mensagem) {
        super(mensagem);
    }

    /**
     * Constrói a exceção com uma mensagem descritiva e a causa raiz.
     *
     * @param mensagem a mensagem detalhando a falha
     * @param causa a causa original da exceção
     */
    public MatriculaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

