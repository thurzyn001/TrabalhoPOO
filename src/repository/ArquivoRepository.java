package repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import model.Aluno;
import model.Disciplina;
import model.Matricula;

/**
 * ArquivoRepository — Responsável pela persistência e leitura de dados em arquivos CSV locais.
 *
 * <p>Conceito de POO demonstrado: Separação de Camadas (Repository Pattern) e Isolamento de I/O.</p>
 *
 * <p>Colabora com: {@link Aluno}, {@link Disciplina}, {@link Matricula}. Invariantes que ajuda a manter: I1, I2, I5.</p>
 */
public final class ArquivoRepository {

    private static final String SEPARADOR = ";";
    private static final String ARQ_ALUNOS = "alunos.csv";
    private static final String ARQ_DISCIPLINAS = "disciplinas.csv";
    private static final String ARQ_MATRICULAS = "matriculas.csv";

    // [PERSISTÊNCIA] Dados de semente (seed inicial) padronizados
    private static final List<String> SEED_ALUNOS = List.of(
        "ra;nome",
        "2024001;Ana Souza",
        "2024002;Bruno Lima",
        "2024003;Carla Mendes",
        "2024004;Diego Rocha",
        "2024005;Elisa Ferreira"
    );

    private static final List<String> SEED_DISCIPLINAS = List.of(
        "codigo;nome;limiteVagas",
        "POO101;Programação Orientada a Objetos;30",
        "BD201;Banco de Dados;25",
        "ED301;Estruturas de Dados;20",
        "LAB401;Laboratório de Robótica;2"
    );

    private static final List<String> SEED_MATRICULAS = List.of(
        "id;ra;codigoDisciplina;data;status",
        "1;2024001;POO101;2026-02-10;ATIVA",
        "2;2024001;BD201;2026-02-10;ATIVA",
        "3;2024002;POO101;2026-02-11;ATIVA",
        "4;2024002;LAB401;2026-02-11;ATIVA",
        "5;2024003;LAB401;2026-02-12;ATIVA",
        "6;2024003;ED301;2026-02-12;CANCELADA",
        "7;2024004;ED301;2026-02-13;ATIVA",
        "8;2024004;POO101;2026-02-13;ATIVA"
    );

    private final Path diretorio;

    /**
     * Construtor do repositório baseado em diretório de arquivos.
     *
     * @param diretorio caminho base para leitura e escrita dos arquivos CSV
     */
    public ArquivoRepository(Path diretorio) {
        if (diretorio == null) {
            throw new IllegalArgumentException("Diretório não pode ser nulo.");
        }
        this.diretorio = diretorio;
    }

    /**
     * Verifica se os arquivos de dados necessários já existem no diretório.
     *
     * @return {@code true} se todos os três arquivos existem fisicamente, {@code false} caso contrário
     */
    public boolean existemDados() {
        // [PERSISTÊNCIA] Verifica se os três arquivos CSV existem no diretório configurado
        return Files.exists(diretorio.resolve(ARQ_ALUNOS))
                && Files.exists(diretorio.resolve(ARQ_DISCIPLINAS))
                && Files.exists(diretorio.resolve(ARQ_MATRICULAS));
    }

    /**
     * Gera os arquivos CSV iniciais no diretório com os dados de demonstração (seed).
     *
     * @throws IOException em caso de erro de gravação
     */
    public void gerarSeed() throws IOException {
        // [PERSISTÊNCIA] Grava os arquivos CSV iniciais a partir das sementes padronizadas
        escreverLinhas(ARQ_ALUNOS, SEED_ALUNOS);
        escreverLinhas(ARQ_DISCIPLINAS, SEED_DISCIPLINAS);
        escreverLinhas(ARQ_MATRICULAS, SEED_MATRICULAS);
    }

    /**
     * Lê as linhas de um arquivo CSV específico no diretório utilizando UTF-8 explícito.
     *
     * @param nomeArquivo nome do arquivo CSV a ser lido
     * @return lista de linhas contidas no arquivo
     * @throws IOException se o arquivo não existir ou se houver erro de leitura
     */
    private List<String> lerLinhas(String nomeArquivo) throws IOException {
        // [PERSISTÊNCIA] Leitura completa de arquivo em disco utilizando UTF-8 explícito
        Path caminho = diretorio.resolve(nomeArquivo);
        if (!Files.exists(caminho)) {
            throw new IOException("Arquivo não encontrado: " + caminho.toAbsolutePath());
        }
        return Files.readAllLines(caminho, StandardCharsets.UTF_8);
    }

    /**
     * Escreve linhas em um arquivo CSV, garantindo a criação prévia de diretórios necessários.
     *
     * @param nomeArquivo nome do arquivo destino
     * @param linhas linhas a serem persistidas
     * @throws IOException se houver falha de gravação
     */
    private void escreverLinhas(String nomeArquivo, List<String> linhas) throws IOException {
        // [PERSISTÊNCIA] Garante a existência do diretório e grava o arquivo com UTF-8 explícito
        Files.createDirectories(diretorio);
        Path caminho = diretorio.resolve(nomeArquivo);
        Files.write(caminho, linhas, StandardCharsets.UTF_8);
    }

    /**
     * Carrega a lista de alunos a partir do arquivo CSV.
     *
     * @return lista de alunos instanciados
     * @throws IOException se houver erro de formato ou leitura
     */
    public List<Aluno> carregarAlunos() throws IOException {
        List<String> linhas = lerLinhas(ARQ_ALUNOS);
        List<Aluno> alunos = new ArrayList<>();
        boolean primeiraLinha = true;

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i).trim();
            if (linha.isBlank()) {
                continue;
            }
            if (primeiraLinha) {
                primeiraLinha = false;
                continue;
            }
            String[] partes = linha.split(SEPARADOR);
            if (partes.length < 2) {
                throw new IOException("Erro de formato em " + ARQ_ALUNOS + " na linha " + (i + 1) + ": " + linha);
            }
            try {
                alunos.add(new Aluno(partes[0].trim(), partes[1].trim()));
            } catch (IllegalArgumentException e) {
                throw new IOException("Dados inválidos em " + ARQ_ALUNOS + " na linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return alunos;
    }

    /**
     * Carrega a lista de disciplinas a partir do arquivo CSV.
     *
     * @return lista de disciplinas instanciadas
     * @throws IOException se houver erro de formato ou leitura
     */
    public List<Disciplina> carregarDisciplinas() throws IOException {
        List<String> linhas = lerLinhas(ARQ_DISCIPLINAS);
        List<Disciplina> disciplinas = new ArrayList<>();
        boolean primeiraLinha = true;

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i).trim();
            if (linha.isBlank()) {
                continue;
            }
            if (primeiraLinha) {
                primeiraLinha = false;
                continue;
            }
            String[] partes = linha.split(SEPARADOR);
            if (partes.length < 3) {
                throw new IOException("Erro de formato em " + ARQ_DISCIPLINAS + " na linha " + (i + 1) + ": " + linha);
            }
            try {
                int limiteVagas = Integer.parseInt(partes[2].trim());
                disciplinas.add(new Disciplina(partes[0].trim(), partes[1].trim(), limiteVagas));
            } catch (Exception e) {
                throw new IOException("Dados inválidos em " + ARQ_DISCIPLINAS + " na linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return disciplinas;
    }

    /**
     * Carrega e reconstrói as associações das matrículas a partir do arquivo CSV.
     *
     * @param alunosPorRa mapa de alunos já carregados indexados por RA
     * @param disciplinasPorCodigo mapa de disciplinas já carregadas indexadas por código
     * @return lista de matrículas com associações atômicas estabelecidas
     * @throws IOException se houver referência inválida ou erro de leitura
     */
    public List<Matricula> carregarMatriculas(Map<String, Aluno> alunosPorRa,
                                             Map<String, Disciplina> disciplinasPorCodigo) throws IOException {
        List<String> linhas = lerLinhas(ARQ_MATRICULAS);
        List<Matricula> matriculas = new ArrayList<>();
        boolean primeiraLinha = true;

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i).trim();
            if (linha.isBlank()) {
                continue;
            }
            if (primeiraLinha) {
                primeiraLinha = false;
                continue;
            }
            String[] partes = linha.split(SEPARADOR);
            if (partes.length < 5) {
                throw new IOException("Erro de formato em " + ARQ_MATRICULAS + " na linha " + (i + 1) + ": " + linha);
            }
            try {
                int id = Integer.parseInt(partes[0].trim());
                String ra = partes[1].trim();
                String codigo = partes[2].trim();
                LocalDate data = LocalDate.parse(partes[3].trim());
                String status = partes[4].trim();

                Aluno aluno = alunosPorRa.get(ra);
                if (aluno == null) {
                    throw new IOException("Aluno com RA '" + ra + "' não encontrado em " + ARQ_MATRICULAS + " na linha " + (i + 1));
                }
                Disciplina disciplina = disciplinasPorCodigo.get(codigo);
                if (disciplina == null) {
                    throw new IOException("Disciplina com código '" + codigo + "' não encontrada em " + ARQ_MATRICULAS + " na linha " + (i + 1));
                }

                // [ASSOCIAÇÃO] Criação do vínculo bidirecional
                Matricula matricula = Matricula.criar(id, aluno, disciplina, data);

                // [CONSISTÊNCIA] I2: Se a matrícula estiver cancelada no CSV, cancela liberando a vaga ocupada
                if ("CANCELADA".equalsIgnoreCase(status)) {
                    matricula.cancelar();
                }
                matriculas.add(matricula);
            } catch (Exception e) {
                throw new IOException("Dados inválidos em " + ARQ_MATRICULAS + " na linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return matriculas;
    }

    /**
     * Persiste a lista de alunos em arquivo CSV no disco.
     *
     * @param alunos coleção de alunos a serem persistidos
     * @throws IOException em caso de falha de escrita
     */
    public void salvarAlunos(Collection<Aluno> alunos) throws IOException {
        // [PERSISTÊNCIA] Serializa coleção de alunos no formato CSV com cabeçalho
        List<String> linhas = new ArrayList<>();
        linhas.add("ra" + SEPARADOR + "nome");
        for (Aluno aluno : alunos) {
            linhas.add(aluno.getRa() + SEPARADOR + aluno.getNome());
        }
        escreverLinhas(ARQ_ALUNOS, linhas);
    }

    /**
     * Persiste a lista de disciplinas em arquivo CSV no disco.
     *
     * @param disciplinas coleção de disciplinas a serem persistidas
     * @throws IOException em caso de falha de escrita
     */
    public void salvarDisciplinas(Collection<Disciplina> disciplinas) throws IOException {
        // [PERSISTÊNCIA] Serializa coleção de disciplinas no formato CSV com cabeçalho
        List<String> linhas = new ArrayList<>();
        linhas.add("codigo" + SEPARADOR + "nome" + SEPARADOR + "limiteVagas");
        for (Disciplina disciplina : disciplinas) {
            linhas.add(disciplina.getCodigo() + SEPARADOR + disciplina.getNome() + SEPARADOR + disciplina.getLimiteVagas());
        }
        escreverLinhas(ARQ_DISCIPLINAS, linhas);
    }

    /**
     * Persiste a lista de matrículas em arquivo CSV no disco.
     *
     * @param matriculas lista de matrículas a serem persistidas
     * @throws IOException em caso de falha de escrita
     */
    public void salvarMatriculas(List<Matricula> matriculas) throws IOException {
        // [PERSISTÊNCIA] Serializa coleção de matrículas no formato CSV com cabeçalho
        List<String> linhas = new ArrayList<>();
        linhas.add("id" + SEPARADOR + "ra" + SEPARADOR + "codigoDisciplina" + SEPARADOR + "data" + SEPARADOR + "status");
        for (Matricula matricula : matriculas) {
            linhas.add(matricula.getId() + SEPARADOR
                    + matricula.getAluno().getRa() + SEPARADOR
                    + matricula.getDisciplina().getCodigo() + SEPARADOR
                    + matricula.getDataMatricula().toString() + SEPARADOR
                    + matricula.getStatus().name());
        }
        escreverLinhas(ARQ_MATRICULAS, linhas);
    }
}
