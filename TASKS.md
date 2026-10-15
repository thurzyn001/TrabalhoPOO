# ✅ TASKS.md — Plano de Implementação

> Plano em **5 etapas curtas e verificáveis**. Execute **uma etapa por vez**, na ordem.
> Só avance quando **todos** os critérios de verificação da etapa estiverem ✅.
> Regras de código: [AGENTS.md](AGENTS.md) · Especificação: [ARCHITECTURE.md](ARCHITECTURE.md).

**Legenda:** `[ ]` pendente · `[x]` concluído

| Etapa | Entrega | Arquivos `.java` acumulados | Status |
|---|---|---|---|
| 1 | Models e Associações | 3 | ✅ |
| 2 | Service e Regras de Negócio | 6 | ✅ |
| 3 | Persistência CSV e Seed | 6 | ✅ |
| 4 | Script de Casos de Teste | 7 | ✅ |
| 5 | Interface Swing | 9 | ✅ |

### Comando padrão de compilação (usado em todas as etapas)

```powershell
# PowerShell (Windows)
javac -Xlint:all -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
```

```bash
# Linux / macOS / Git Bash
javac -Xlint:all -encoding UTF-8 -d bin $(find src -name "*.java")
```

> As verificações das Etapas 1–3 usam o **`jshell`** (já incluso no JDK), sem criar arquivos extras.
> Em todas as sessões, abra com `jshell --class-path bin` e saia com `/exit`.

---

## Etapa 1 — Models e Associações

**Objetivo:** criar o domínio (`Aluno`, `Disciplina`, `Matricula`) com vínculo bidirecional e controle de vagas garantidos **pelo próprio modelo**.

**Arquivos:** `src/model/Aluno.java`, `src/model/Disciplina.java`, `src/model/Matricula.java`

### Tarefas

- [x] **1.1** `Aluno`: atributos `ra`, `nome` (`final`) e `matriculas` (`ArrayList`); construtor com validação; getters; `equals`/`hashCode` por `ra`; `toString()` → `"2024001 - Ana Souza"`.
- [x] **1.2** `Aluno`: `getMatriculas()` não modificável; `adicionarMatricula(...)` **package-private**; `buscarMatriculaAtivaEm(...)` → `Optional<Matricula>`; `possuiMatriculaAtivaEm(...)`; `getDisciplinasAtivas()` (laço `for` explícito, ordem de matrícula).
- [x] **1.3** `Disciplina`: atributos `codigo`, `nome`, `limiteVagas` (`final`), `vagasOcupadas`, `matriculas`; construtor rejeita `limiteVagas <= 0`; `temVagas()`, `getVagasDisponiveis()`, `getMatriculasAtivas()`; `equals`/`hashCode` por `codigo`; `toString()` → `"POO101 - Programação Orientada a Objetos (3/30)"`.
- [x] **1.4** `Disciplina`: `adicionarMatricula(...)` (package-private) chama **primeiro** `ocuparVaga()` (privado, lança `IllegalStateException` se lotada) e **só depois** adiciona à lista — se recusar, nada muda; `liberarVaga()` (package-private, lança `IllegalStateException` se `vagasOcupadas == 0`).
- [x] **1.5** `Matricula`: enum aninhado `Status { ATIVA, CANCELADA }`; construtor **privado**; *factory* `criar(...)` que cria com `ATIVA` e vincula aos dois lados; `cancelar()` (lança `IllegalStateException` se já cancelada; libera vaga); `isAtiva()`; getters; `toString()` → `"#9 Elisa Ferreira -> POO101 [ATIVA]"`.
- [x] **1.6** Javadoc de classe e de métodos públicos + marcadores `[ASSOCIAÇÃO]`, `[CONSISTÊNCIA]`, `[ENCAPSULAMENTO]`.

### Verificação

1. Compilar com o comando padrão → **0 erros, 0 avisos**.
2. No `jshell --class-path bin`:

```java
import model.*;
import java.time.LocalDate;
Aluno ana = new Aluno("2024001", "Ana Souza");
Disciplina lab = new Disciplina("LAB401", "Laboratório de Robótica", 1);
Matricula m = Matricula.criar(1, ana, lab, LocalDate.now());
ana.getMatriculas().contains(m) && lab.getMatriculas().contains(m)   // esperado: true   (I1)
lab.getVagasOcupadas()                                               // esperado: 1      (I2)
lab.temVagas()                                                       // esperado: false
ana.getDisciplinasAtivas()                                           // esperado: [LAB401 - Laboratório de Robótica (1/1)]
Matricula.criar(2, new Aluno("2024002", "Bruno Lima"), lab, LocalDate.now())   // esperado: IllegalStateException (defesa I3)
m.cancelar();
lab.getVagasOcupadas() + " " + m.getStatus() + " " + ana.getMatriculas().size()  // esperado: "0 CANCELADA 1"
ana.getDisciplinasAtivas()                                           // esperado: []
m.cancelar()                                                         // esperado: IllegalStateException (I5)
ana.getMatriculas().clear()                                          // esperado: UnsupportedOperationException
ana.adicionarMatricula(m)                                            // esperado: ERRO DE COMPILAÇÃO (package-private)
```

> ⚠️ **Ordem de vínculo em `criar(...)`:** chame `disciplina.adicionarMatricula(m)` **antes** de
> `aluno.adicionarMatricula(m)`. Assim, quando a disciplina recusa por estar lotada (linha com
> `Matricula.criar(2, ...)` acima), o aluno Bruno **não** fica com um vínculo pela metade.
> No uso real, o service verifica `temVagas()` antes (Etapa 2); esta é a **defesa de último nível**.

### Critérios de aceite

- [x] Todos os resultados do `jshell` conferem com os "esperados".
- [x] Nenhum `import` de `javax.swing`, `java.io` ou `java.nio` no pacote `model`.
- [x] Não existe nenhum *setter* público nem construtor público em `Matricula`.

**Commit sugerido:** `feat(model): Aluno, Disciplina e Matricula com associações bidirecionais`

---

## Etapa 2 — Service e Regras de Negócio

**Objetivo:** implementar os casos de uso de matrícula e cancelamento, cadastro de alunos e disciplinas em tempo de execução, e as validações obrigatórias com mensagens padronizadas.

**Arquivos:** `src/service/MatriculaException.java`, `src/service/SistemaAcademicoService.java`, `src/repository/ArquivoRepository.java` *(versão provisória em memória)*

> **Por que o repositório aparece já aqui?** O service recebe um `ArquivoRepository` no construtor.
> Nesta etapa, criamos o repositório com a **API final**, mas lendo das constantes de seed **em memória**.
> Na Etapa 3, apenas os métodos de I/O são trocados por leitura/escrita real em disco — o service não muda.

### Tarefas

- [x] **2.1** `MatriculaException extends Exception` com os dois construtores e `serialVersionUID`.
- [x] **2.2** `ArquivoRepository` (provisório): constantes `SEPARADOR`, `ARQ_*` e `SEED_ALUNOS`, `SEED_DISCIPLINAS`, `SEED_MATRICULAS` (linhas CSV com cabeçalho, exatamente os dados do `README.md`); `carregarAlunos()`, `carregarDisciplinas()`, `carregarMatriculas(...)` **definitivos** (parser real); `lerLinhas(nome)` provisoriamente devolve a constante `SEED_*` correspondente; `existemDados()` → `true`; `gerarSeed()`, `escreverLinhas(...)`, `salvarAlunos(...)`, `salvarDisciplinas(...)` e `salvarMatriculas(...)` com as assinaturas finais e corpos provisórios vazios, com comentário `// Etapa 3: implementar gravação em disco`.
- [x] **2.3** `SistemaAcademicoService`: atributos conforme o diagrama; `carregarDados()` (carga na ordem alunos → disciplinas → matrículas; cálculo de `proximoIdMatricula`).
- [x] **2.4** `buscarAluno` / `buscarDisciplina` privados, tratando nulo/vazio/inexistente com as mensagens da seção 6 de `ARCHITECTURE.md`.
- [x] **2.5** `cadastrarAluno(ra, nome)` e `cadastrarDisciplina(codigo, nome, limiteVagas)` validando campos vazios/inválidos e duplicidade via `MatriculaException`, inserindo nos mapas internos e chamando `persistirAlunos()` / `persistirDisciplinas()`.
- [x] **2.6** `matricular(ra, codigo)` com a ordem: existe aluno → existe disciplina → **duplicidade** → **vagas** → `Matricula.criar(...)` → adiciona à lista global → `persistirMatriculas()`.
- [x] **2.7** `cancelarMatricula(ra, codigo)` via `aluno.buscarMatriculaAtivaEm(...)` → `cancelar()` → `persistirMatriculas()`.
- [x] **2.8** `listarDisciplinasAtivas(ra)`, `listarAlunos()`, `listarDisciplinas()`, `listarMatriculas()` devolvendo `List.copyOf(...)`.
- [x] **2.9** `persistirMatriculas()`, `persistirAlunos()`, `persistirDisciplinas()` convertendo qualquer `IOException` em `MatriculaException("Falha ao salvar os dados: ...", causa)`.
- [x] **2.10** Javadoc + marcadores `[REGRA DE NEGÓCIO]` e `[CAMADAS]`.

### Verificação

No `jshell --class-path bin`:

```java
import service.*;
import repository.*;
import java.nio.file.Path;
SistemaAcademicoService s = new SistemaAcademicoService(new ArquivoRepository(Path.of("dados")));
s.carregarDados();
s.listarDisciplinas()                     // esperado: POO101 (3/30), BD201 (1/25), ED301 (1/20), LAB401 (2/2)
s.matricular("2024005", "POO101")         // esperado: #9 Elisa Ferreira -> POO101 [ATIVA]
s.matricular("2024001", "POO101")         // esperado: MatriculaException: O aluno Ana Souza já possui matrícula ativa em POO101.
s.matricular("2024005", "LAB401")         // esperado: MatriculaException: A disciplina LAB401 está lotada (2/2 vagas ocupadas).
s.cancelarMatricula("2024002", "LAB401");
s.matricular("2024005", "LAB401")         // esperado: #10 Elisa Ferreira -> LAB401 [ATIVA]
s.listarDisciplinasAtivas("2024005")      // esperado: [POO101 ... (4/30), LAB401 ... (2/2)]
s.cancelarMatricula("2024005", "ED301")   // esperado: MatriculaException: O aluno Elisa Ferreira não possui matrícula ativa em ED301.
s.matricular("9999999", "POO101")         // esperado: MatriculaException: Aluno com RA '9999999' não encontrado.
s.cadastrarAluno("2024006", "Fernanda Costa") // esperado: 2024006 - Fernanda Costa
s.cadastrarAluno("2024001", "Outro")      // esperado: MatriculaException: Aluno com RA '2024001' já está cadastrado.
s.cadastrarDisciplina("WEB501", "Desenvolvimento Web", 2) // esperado: WEB501 - Desenvolvimento Web (0/2)
s.cadastrarDisciplina("POO101", "Outro", 10) // esperado: MatriculaException: Disciplina com código 'POO101' já está cadastrada.
s.listarAlunos().clear()                  // esperado: UnsupportedOperationException
```

### Critérios de aceite

- [x] Todas as saídas conferem, **inclusive o texto exato** das mensagens.
- [x] Nenhum arquivo foi criado em disco (a pasta `dados/` **não** deve existir ainda).
- [x] `service` não importa `javax.swing`/`java.awt` e não usa `System.out`.

**Commit sugerido:** `feat(service): casos de uso de matrícula e cadastro com validações de regras de negócio`

---

## Etapa 3 — Persistência CSV e Seed

**Objetivo:** tornar o `ArquivoRepository` real (disco, UTF-8, `;`) e gerar o seed automaticamente.

**Arquivos:** `src/repository/ArquivoRepository.java` *(finalização)*, `.gitignore` *(novo, não-Java)*

### Tarefas

- [x] **3.1** `lerLinhas(nome)` → `Files.readAllLines(diretorio.resolve(nome), StandardCharsets.UTF_8)`.
- [x] **3.2** `escreverLinhas(nome, linhas)` → `Files.createDirectories(diretorio)` + `Files.write(..., StandardCharsets.UTF_8)`.
- [x] **3.3** `existemDados()` → verdadeiro somente se os 3 arquivos existirem.
- [x] **3.4** `gerarSeed()` → grava `SEED_ALUNOS`, `SEED_DISCIPLINAS`, `SEED_MATRICULAS`.
- [x] **3.5** `salvarMatriculas(...)` → cabeçalho + uma linha por matrícula (`id;ra;codigo;data;status`).
- [x] **3.6** `salvarAlunos(...)` e `salvarDisciplinas(...)` → cabeçalhos + linhas formatadas em `alunos.csv` e `disciplinas.csv`.
- [x] **3.7** Parser robusto: ignora cabeçalho e linhas em branco; erro de formato → `IOException` com **arquivo e número da linha**.
- [x] **3.8** Remover todos os comentários provisórios da Etapa 2.
- [x] **3.9** Criar `.gitignore` com:
  ```text
  bin/
  dados/
  fontes.txt
  *.class
  ```
- [x] **3.10** Marcadores `[PERSISTÊNCIA]` nos pontos de leitura, escrita e seed.

### Verificação

1. Apagar dados antigos e compilar: `Remove-Item -Recurse -Force dados -ErrorAction SilentlyContinue`.
2. Repetir a sessão `jshell` da Etapa 2 até a linha `s.matricular("2024005", "POO101")`, depois `/exit`.
3. Conferir os arquivos:

```powershell
Get-ChildItem dados                        # esperado: alunos.csv, disciplinas.csv, matriculas.csv
Get-Content dados\matriculas.csv -Encoding UTF8
# esperado: cabeçalho + 9 linhas; a última termina em ";2024005;POO101;<data de hoje>;ATIVA"
```

4. Nova sessão `jshell --class-path bin` (simula reabrir o programa):

```java
import service.*;
import repository.*;
import java.nio.file.Path;
SistemaAcademicoService s2 = new SistemaAcademicoService(new ArquivoRepository(Path.of("dados")));
s2.carregarDados();
s2.listarDisciplinas()                    // esperado: POO101 (4/30) — o estado foi persistido
s2.listarDisciplinasAtivas("2024005")     // esperado: [POO101 - Programação Orientada a Objetos (4/30)]
s2.matricular("2024004", "BD201")         // esperado: #10 ... (ids continuam a sequência)
```

5. Abrir `dados/disciplinas.csv` no Excel/Bloco de Notas → acentos corretos (`Laboratório de Robótica`).

### Critérios de aceite

- [x] Seed é criado automaticamente quando `dados/` não existe.
- [x] Estado sobrevive ao reinício; `vagasOcupadas` é recalculado corretamente (não está no CSV).
- [x] Matrícula `CANCELADA` no CSV **não** ocupa vaga após a carga.
- [x] `git status` não lista `bin/` nem `dados/`.

**Commit sugerido:** `feat(repository): persistência CSV com seed automático`

---

## Etapa 4 — Script de Casos de Teste (`CasosDeTesteRunner`)

**Objetivo:** automatizar os cenários de sucesso e erro e gerar texto pronto para a seção *Análise de Resultados*.

**Arquivo:** `src/app/CasosDeTesteRunner.java`

### Tarefas

- [x] **4.1** Estrutura: interface funcional aninhada `Teste`; auxiliares `executarCaso`, `registrar`, `verificar`, `esperarExcecao`, `verificarInvariantes`, `imprimirResumo` (ver `ARCHITECTURE.md`, 4.9).
- [x] **4.2** Criar diretório temporário com `Files.createTempDirectory("academico-teste")`, montar repositório + service e chamar `carregarDados()`.
- [x] **4.3** `verificarInvariantes(service)`: checar **I1–I4** para todas as matrículas e disciplinas.
- [x] **4.4** Implementar os 13 casos da tabela abaixo, **na ordem** (o estado é compartilhado).
- [x] **4.5** Imprimir cabeçalho, blocos por caso, resumo e tabela Markdown no formato especificado.
- [x] **4.6** Encerrar com `System.exit(0)` se tudo passou, senão `System.exit(1)`.

### Casos de teste obrigatórios

| ID | Título | Entrada | Resultado esperado |
|---|---|---|---|
| **CT01** | Carga inicial (seed) e consistência | Diretório temporário vazio | 5 alunos, 4 disciplinas, 8 matrículas (7 ativas); LAB401 2/2; invariantes OK |
| **CT02** | Matrícula válida | Elisa Ferreira (2024005) → POO101 | Matrícula **#9** ATIVA; POO101 3/30 → **4/30**; Elisa ativas = [POO101]; matrícula presente em `POO101.getMatriculasAtivas()` (bidirecional) |
| **CT03** | Bloqueio de matrícula duplicada | Ana Souza (2024001) → POO101 | `MatriculaException`: "O aluno Ana Souza já possui matrícula ativa em POO101."; POO101 **permanece 4/30**; Ana continua com 2 matrículas |
| **CT04** | Bloqueio por disciplina lotada | Elisa Ferreira (2024005) → LAB401 | `MatriculaException`: "A disciplina LAB401 está lotada (2/2 vagas ocupadas)."; LAB401 permanece 2/2; Elisa continua só com [POO101] |
| **CT05** | Cancelamento de matrícula | Cancelar Bruno Lima (2024002) em LAB401 | Matrícula **#4** CANCELADA; LAB401 2/2 → **1/2**; Bruno ativas = [POO101]; Bruno ainda tem **2** matrículas (histórico preservado) |
| **CT06** | Reaproveitamento da vaga liberada | Elisa Ferreira (2024005) → LAB401 | Matrícula **#10** ATIVA; LAB401 → **2/2**; Elisa ativas = [POO101, LAB401] |
| **CT07** | Cancelamento de matrícula inexistente | Cancelar Elisa Ferreira (2024005) em ED301 | `MatriculaException`: "O aluno Elisa Ferreira não possui matrícula ativa em ED301."; ED301 permanece 1/20 |
| **CT08** | Rematrícula após cancelamento | Carla Mendes (2024003) → ED301 (a #6 está CANCELADA) | Nova matrícula **#11** ATIVA; a #6 **continua** CANCELADA; ED301 1/20 → **2/20**; Carla tem 3 matrículas (ativas: LAB401, ED301) |
| **CT09** | Listagem de disciplinas ativas | `listarDisciplinasAtivas("2024001")` | [POO101, BD201], nessa ordem |
| **CT10** | Aluno e disciplina inexistentes | `matricular("9999999","POO101")` e `matricular("2024001","XYZ999")` | Mensagens "Aluno com RA '9999999' não encontrado." e "Disciplina com código 'XYZ999' não encontrada."; total continua 11 matrículas |
| **CT11** | Persistência e recarga | Novo repositório + novo service no **mesmo** diretório temporário; depois Bruno Lima → BD201 | Após recarga: 11 matrículas (9 ativas); POO101 4/30, BD201 1/25, ED301 2/20, LAB401 2/2; nova matrícula recebe **#12**; BD201 → 2/25 |
| **CT12** | Encapsulamento protege a consistência | `aluno.getMatriculas().clear()`, `disciplina.getMatriculas().clear()`, `service.listarAlunos().clear()` | `UnsupportedOperationException` nas 3 tentativas; contagens inalteradas |
| **CT13** | Cadastro de novo aluno e nova disciplina com matrícula e persistência | Cadastrar Fernanda Costa (2024006), cadastrar WEB501 (2 vagas), matricular Fernanda em WEB501, depois recarregar novo repositório + service no mesmo diretório temporário | Novo Aluno e Disciplina cadastrados; matrícula **#13** ATIVA; WEB501 0/2 → **1/2**; após recarga dos 3 CSVs: Fernanda em `listarAlunos()`, WEB501 em `listarDisciplinas()` com 1/2 vagas, `listarDisciplinasAtivas("2024006")` = [WEB501]; invariantes I1-I4 OK |

> Todos os casos terminam chamando `verificarInvariantes(...)`. No CT11, as verificações usam o **novo** service recarregado (e no CT12 e CT13 também).

### Formato de saída obrigatório

```text
==================================================================
 CASOS DE TESTE - SISTEMA ACADÊMICO (Associações entre Classes)
 Execução: 2026-10-15 00:45 | Dados: diretório temporário (seed)
==================================================================

[CT01] Carga inicial (seed) e consistência ............... PASSOU
       Entrada : diretório temporário vazio (seed automático)
       Esperado: 5 alunos, 4 disciplinas, 8 matrículas (7 ativas)
       Obtido  : 5 alunos, 4 disciplinas, 8 matrículas (7 ativas)
       Estado  : invariantes I1-I4 OK

[CT02] Matrícula válida .................................. PASSOU
       Entrada : Elisa Ferreira (2024005) -> POO101
       Esperado: matrícula ATIVA; POO101 de 3/30 para 4/30
       Obtido  : #9 Elisa Ferreira -> POO101 [ATIVA]; POO101 4/30
       Estado  : invariantes I1-I4 OK

[CT03] Bloqueio de matrícula duplicada ................... PASSOU
       Entrada : Ana Souza (2024001) -> POO101
       Esperado: MatriculaException (matrícula duplicada)
       Obtido  : "O aluno Ana Souza já possui matrícula ativa em POO101."
       Estado  : POO101 permanece 4/30; invariantes I1-I4 OK

   ... (CT04 a CT13 no mesmo formato) ...

------------------------------------------------------------------
 RESUMO: 13 casos executados | 13 PASSOU | 0 FALHOU
------------------------------------------------------------------

TABELA PARA O RELATÓRIO (copie e cole em Markdown):

| Caso | Cenário | Resultado esperado | Resultado obtido | Status |
|------|---------|--------------------|------------------|--------|
| CT01 | Carga inicial (seed) e consistência | 5 alunos, 4 disciplinas, 8 matrículas (7 ativas) | 5 alunos, 4 disciplinas, 8 matrículas (7 ativas) | PASSOU |
| CT02 | Matrícula válida | matrícula ATIVA; POO101 de 3/30 para 4/30 | #9 Elisa Ferreira -> POO101 [ATIVA]; POO101 4/30 | PASSOU |
| ...  | ...     | ...                | ...              | ...    |
```

Em caso de falha, o bloco mostra `FALHOU` e uma linha extra `Motivo  : <mensagem do AssertionError ou da exceção>`.

### Verificação

```bash
java -cp bin app.CasosDeTesteRunner
echo $LASTEXITCODE        # PowerShell — esperado: 0   (Bash: echo $?)
```

### Critérios de aceite

- [x] **13/13 PASSOU** e código de saída `0`.
- [x] A pasta `dados/` do projeto **não** foi modificada (compare a data de modificação antes/depois).
- [x] Sabotagem controlada: comentar temporariamente a verificação de duplicidade no service faz o **CT03 FALHAR** (e os demais continuarem rodando). Desfazer em seguida.
- [x] A tabela Markdown renderiza corretamente quando colada em um editor Markdown.

**Commit sugerido:** `test: CasosDeTesteRunner com 13 cenários e saída para o relatório`

---

## Etapa 5 — Interface Swing (`TelaPrincipal`) e Ponto de Entrada (`Main`)

**Objetivo:** janela simples e intuitiva que apenas **exibe** dados e **delega** ações ao service.

**Arquivos:** `src/view/TelaPrincipal.java`, `src/app/Main.java`

### Tarefas

- [x] **5.1** `Main`: criar `ArquivoRepository(Path.of("dados"))` → `SistemaAcademicoService` → `carregarDados()`; em `IOException`, `JOptionPane` de erro e encerrar; abrir a tela com `SwingUtilities.invokeLater`.
- [x] **5.2** `TelaPrincipal` (`final`, `extends JFrame`, `serialVersionUID`): título `"Sistema Acadêmico — Matrículas"`, ~920×540, centralizada, `EXIT_ON_CLOSE`.
- [x] **5.3** Painel superior (`FlowLayout`): combos de **Aluno** e **Disciplina** + botões **Matricular**, **Cancelar matrícula**, **+ Novo Aluno** e **+ Nova Disciplina**.
- [x] **5.4** Painel central (`GridLayout(1, 2)`): à esquerda, `JList` "Disciplinas ativas do aluno"; à direita, `JTable` não editável "Ocupação das disciplinas" (Código, Nome, Ocupadas, Limite, Situação = `Disponível`/`LOTADA`).
- [x] **5.5** Barra de status (`JLabel`) no rodapé com a última operação.
- [x] **5.6** Eventos: trocar aluno → atualiza a lista; clicar numa disciplina da lista → seleciona-a no combo; botões de matrícula/cancelamento → chamam service; botões Novo Aluno / Nova Disciplina → solicitam campos via `JOptionPane.showInputDialog`, invocam o service → `atualizarTela()` (preservando as seleções) ou `mostrarErro(e.getMessage())`.
- [x] **5.7** Javadoc + marcadores `[CAMADAS]` em cada chamada ao service.

### Verificação (roteiro manual — o mesmo da apresentação)

Apague `dados/`, compile e execute `java -cp bin app.Main`:

| # | Ação | Resultado esperado |
|---|---|---|
| 1 | Selecionar **Elisa Ferreira** | Lista de ativas vazia; tabela mostra LAB401 como **LOTADA** |
| 2 | Matricular Elisa em **POO101** | Sucesso; lista = [POO101]; tabela POO101 = 4/30; status atualizado |
| 3 | Selecionar **Ana Souza** → matricular em **POO101** | Diálogo de erro: "O aluno Ana Souza já possui matrícula ativa em POO101." |
| 4 | Selecionar **Elisa** → matricular em **LAB401** | Diálogo de erro: "A disciplina LAB401 está lotada (2/2 vagas ocupadas)." |
| 5 | Selecionar **Bruno Lima** → **LAB401** → Cancelar matrícula | Sucesso; LAB401 = 1/2, situação `Disponível`; lista do Bruno = [POO101] |
| 6 | Selecionar **Elisa** → matricular em **LAB401** | Sucesso; LAB401 = 2/2 **LOTADA**; lista = [POO101, LAB401] |
| 7 | Clicar em **+ Novo Aluno** → RA: `2024006`, Nome: `Fernanda Costa` | Diálogo informativo/status de sucesso; combo de alunos passa a incluir Fernanda |
| 8 | Clicar em **+ Nova Disciplina** → Código: `WEB501`, Nome: `Desenvolvimento Web`, Limite: `2` | Sucesso; combo e tabela passam a exibir WEB501 (0/2, Disponível) |
| 9 | Selecionar **Fernanda Costa** e matricular em **WEB501** | Sucesso; ocupação de WEB501 vai para 1/2; lista ativa de Fernanda = [WEB501] |
| 10 | Fechar e reabrir o programa | Todo o estado anterior (inclusive novos cadastros) é mantido em disco |

### Verificações estruturais (PowerShell)

```powershell
(Get-ChildItem -Recurse -Filter *.java src).Count                                   # esperado: 9
Select-String -Path src\view\*.java  -Pattern "import repository|import java\.io|import java\.nio"   # esperado: nenhuma saída
Select-String -Path src\model\*.java -Pattern "javax\.swing|java\.awt|java\.io|java\.nio|import (service|repository|view)"   # esperado: nenhuma saída
Select-String -Path src\service\*.java -Pattern "javax\.swing|java\.awt|System\.out"                    # esperado: nenhuma saída
```

### Critérios de aceite

- [x] Roteiro manual 1–10 executado sem divergências.
- [x] Verificações estruturais sem nenhuma saída indevida; total de **9** arquivos `.java`.
- [x] `java -cp bin app.CasosDeTesteRunner` continua com **13/13 PASSOU**.
- [x] Compilação com `-Xlint:all` sem avisos.
- [x] Checklist **"Definição de Pronto"** de `AGENTS.md` (seção 10) 100% marcado.

**Commit sugerido:** `feat(view): interface Swing com matrículas e cadastros dinâmicos`

---

## 📝 Após a Etapa 5 — Insumos para o Relatório

- [ ] Copiar a tabela Markdown do `CasosDeTesteRunner` para **Análise de Resultados**.
- [ ] Exportar os diagramas de `ARCHITECTURE.md` (seções 3.1 e 5) como imagem (ex.: [mermaid.live](https://mermaid.live)).
- [ ] Capturar telas dos passos 2, 3, 4, 6 e 9 do roteiro manual.
- [ ] Usar a seção 8 de `ARCHITECTURE.md` (Associações × Classe Única) na **Discussão/Conclusão**.
