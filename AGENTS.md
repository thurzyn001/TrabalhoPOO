# 🤖 AGENTS.md — Regras para o Agente Codificador

> Este arquivo define **como** o código deve ser escrito. Ele vale para qualquer agente de IA
> **e** para os integrantes humanos do grupo. Leia-o **inteiro** antes de criar qualquer arquivo.

---

## 0. Ordem de Leitura e Precedência

1. `AGENTS.md` (este arquivo) — regras inegociáveis.
2. `ARCHITECTURE.md` — **o que** construir (classes, assinaturas, invariantes, mensagens).
3. `TASKS.md` — **em que ordem** construir e como verificar cada etapa.
4. `README.md` — contexto acadêmico e instruções de execução.

**Em caso de conflito:** `AGENTS.md` > `ARCHITECTURE.md` > `TASKS.md` > `README.md`.
Se a dúvida persistir, **pare e pergunte** ao usuário em vez de supor.

---

## 1. Regras de Ouro (inegociáveis)

| # | Regra |
|---|---|
| **R1** | **Java puro, JDK 17+.** Somente classes da biblioteca padrão (`java.*` e `javax.swing.*`). |
| **R2** | **Proibido** qualquer sistema de build ou dependência externa: nada de `pom.xml`, `build.gradle`, `settings.gradle`, `mvnw`, `gradlew`, pasta `lib/`, arquivos `.jar`. |
| **R3** | **Proibidas** as bibliotecas/frameworks: JUnit, TestNG, Mockito, Lombok, Spring, JavaFX, Jackson, Gson, OpenCSV, Apache Commons, Guava, SQLite, H2, JDBC, Log4j/SLF4J — e qualquer outra não listada em R1. |
| **R4** | **No máximo 10 arquivos `.java`.** O projeto previsto tem **9** (seção 2). Não crie `module-info.java`. |
| **R5** | **Separação de camadas estrita** conforme a matriz de dependências (seção 3). A `view` nunca acessa o `repository`; o `model` nunca conhece Swing nem arquivos. |
| **R6** | **Spec-Driven:** implemente exatamente as classes, atributos, assinaturas e mensagens de `ARCHITECTURE.md`. Para mudar algo, atualize **primeiro** o documento (diagrama incluído) e explique o motivo. Métodos **privados** auxiliares extras são permitidos, desde que não alterem a API pública/de pacote documentada. |
| **R7** | **Uma etapa por vez** de `TASKS.md`, na ordem. Só avance depois de cumprir os critérios de verificação da etapa atual e marcar os checkboxes. |
| **R8** | **Código didático:** claro, comentado em português e pronto para ser **explicado em uma apresentação** (seção 5). |
| **R9** | **Não altere o seed** (5 alunos, 4 disciplinas, 8 matrículas) — os casos de teste dependem dele. |
| **R10** | **Compilação limpa:** `javac -Xlint:all` deve terminar **sem erros e sem avisos**. |

---

## 2. Lista Branca de Arquivos

Somente estes arquivos `.java` podem existir (caminhos relativos a `src/`):

| Arquivo | Pacote |
|---|---|
| `model/Aluno.java` | `model` |
| `model/Disciplina.java` | `model` |
| `model/Matricula.java` | `model` |
| `service/MatriculaException.java` | `service` |
| `service/SistemaAcademicoService.java` | `service` |
| `repository/ArquivoRepository.java` | `repository` |
| `view/TelaPrincipal.java` | `view` |
| `app/Main.java` | `app` |
| `app/CasosDeTesteRunner.java` | `app` |

- Tipos auxiliares (enum `Status`, interface funcional `Teste`, modelos de tabela) devem ser **aninhados** em uma das classes acima.
- Um 10º arquivo só pode ser criado com **justificativa escrita** em `ARCHITECTURE.md` e aprovação do usuário.
- Arquivos não-Java permitidos: os 4 `.md`, `.gitignore` e os `.csv` gerados em `dados/`.
- **Nunca** versionar `bin/`, `dados/` ou `fontes.txt`.

---

## 3. Matriz de Dependências (imports permitidos)

| Pacote | ✅ Pode importar | ❌ Não pode importar |
|---|---|---|
| `model` | `java.util.*`, `java.time.*` | `service`, `repository`, `view`, `app`, `javax.swing.*`, `java.awt.*`, `java.io.*`, `java.nio.*` |
| `repository` | `model`, `java.nio.*`, `java.io.*`, `java.util.*`, `java.time.*` | `service`, `view`, `app`, `javax.swing.*`, `java.awt.*` |
| `service` | `model`, `repository`, `java.util.*`, `java.time.*`, `java.io.IOException` | `view`, `app`, `javax.swing.*`, `java.awt.*` |
| `view` | `service`, `model`, `javax.swing.*`, `java.awt.*`, `java.util.*` | `repository`, `java.io.*`, `java.nio.*` |
| `app` | todos os acima | — |

**Regras complementares de camada:**

- A `view` usa objetos do `model` **somente para leitura** (getters e `toString()`); toda alteração passa pelo `SistemaAcademicoService`.
- `model` e `service` **não imprimem** no console (`System.out`/`System.err`) e **não** chamam `System.exit`.
- Somente `app.Main` e `app.CasosDeTesteRunner` podem ter método `main`.
- Somente `app.CasosDeTesteRunner` pode chamar `System.exit`.

---

## 4. Regras de Orientação a Objetos

1. **Encapsulamento total:** todos os atributos são `private`. Use `final` sempre que o valor não mudar após a construção.
2. **Coleções protegidas:** getters de listas devolvem `Collections.unmodifiableList(...)` (no model) ou `List.copyOf(...)` (no service). Nunca exponha a lista interna.
3. **Vínculo em ponto único:** apenas `Matricula.criar(...)` liga `Aluno` ↔ `Matricula` ↔ `Disciplina`. Métodos de vínculo (`adicionarMatricula`, `liberarVaga`) são **package-private** — sem modificador de acesso — e isso deve ser comentado.
4. **Construtor de `Matricula` é `private`.** Não crie construtores públicos alternativos.
5. **Sem setters genéricos.** Não existe `setStatus`, `setVagasOcupadas`, `setAluno` etc. O estado muda apenas por métodos com significado de negócio (`cancelar()`).
6. **Identidade:** `Aluno` e `Disciplina` sobrescrevem `equals`/`hashCode` usando apenas `ra`/`codigo`. `Matricula` **não** sobrescreve (identidade por referência).
7. **Validação de argumentos** nos construtores do model com `IllegalArgumentException`; estados impossíveis com `IllegalStateException`; regras de negócio com `MatriculaException` (apenas no service).
8. **Respeite as invariantes I1–I6** de `ARCHITECTURE.md`, seção 5. Nenhum método público pode deixar o sistema em estado que as viole.
9. **Mensagens de erro** devem ser **idênticas** às da tabela da seção 6 de `ARCHITECTURE.md`.

---

## 5. Estilo de Código e Comentários Didáticos

### 5.1 Convenções

- Identificadores, comentários e mensagens em **português do Brasil** (domínio acadêmico), sem abreviações obscuras.
- Convenções Java: `PascalCase` para classes, `camelCase` para métodos/atributos, `UPPER_SNAKE_CASE` para constantes.
- Indentação de 4 espaços, chaves na mesma linha, um comando por linha.
- Métodos curtos (idealmente até ~30 linhas). Se crescer, extraia um método privado com nome descritivo.
- Prefira **tipos explícitos** a `var` e **laços `for` explícitos** a *streams* no `model`, para facilitar a explicação. *Streams* simples são aceitáveis no `service` e na `view`.
- Não use `record` para as classes do model (queremos mostrar encapsulamento clássico).
- Todos os arquivos em **UTF-8**; todo I/O com `StandardCharsets.UTF_8` explícito.
- Sem código morto, sem `printStackTrace()`, sem `catch` vazio, sem *imports* não usados, sem `import pacote.*` curinga.
- Classes Swing que estendem `JFrame` (e qualquer subclasse serializável, inclusive classes anônimas de `DefaultTableModel`) devem declarar `private static final long serialVersionUID = 1L;` ou usar `@SuppressWarnings("serial")` (evita aviso do `-Xlint`).
- Declare as classes como `final` quando não houver herança prevista (todas as 9, exceto se a arquitetura disser o contrário). Isso comunica a intenção de projeto **e** evita o aviso `this-escape` do `-Xlint:all` em JDK 21+ (ex.: `TelaPrincipal` chamando `setTitle(...)` no construtor).

### 5.2 Comentários obrigatórios

1. **Javadoc de classe** em todas as 9 classes, explicando a responsabilidade e **qual conceito de POO ela demonstra**.
2. **Javadoc em todo método público**, com `@param`, `@return` e `@throws` quando aplicável.
3. **Marcadores didáticos** em comentários de linha, para que o grupo localize os conceitos durante a apresentação:

| Marcador | Usar quando o trecho demonstra… |
|---|---|
| `// [ASSOCIAÇÃO]` | criação ou navegação de vínculo entre objetos |
| `// [CONSISTÊNCIA]` | manutenção de uma invariante (cite o ID, ex.: `I2`) |
| `// [ENCAPSULAMENTO]` | proteção de estado (visibilidade, lista não modificável, `final`) |
| `// [REGRA DE NEGÓCIO]` | validação de duplicidade, vagas ou existência |
| `// [CAMADAS]` | ponto em que uma camada delega para outra |
| `// [PERSISTÊNCIA]` | leitura/escrita de CSV ou geração do seed |

4. Comente o **porquê**, não o óbvio. ❌ `// incrementa i` · ✅ `// [CONSISTÊNCIA] I2: a vaga é ocupada no mesmo passo em que o vínculo é criado`.

### 5.3 Modelo de Javadoc de classe

```text
/**
 * <Nome da classe> — <responsabilidade em uma frase>.
 *
 * <p>Conceito de POO demonstrado: <associação | classe associativa | encapsulamento | ...>.</p>
 *
 * <p>Colabora com: <classes>. Invariantes que ajuda a manter: <I1, I2, ...>.</p>
 */
```

---

## 6. Interface Swing — Regras Específicas

- Toda criação/alteração de componentes ocorre na **Event Dispatch Thread** (`SwingUtilities.invokeLater` em `Main`).
- Apenas componentes nativos (`JFrame`, `JPanel`, `JComboBox`, `JList`, `JTable`, `JButton`, `JLabel`, `JScrollPane`, `JOptionPane`) e *layouts* padrão (`BorderLayout`, `FlowLayout`, `GridLayout`). Sem editores visuais (`.form`) e sem `null` layout.
- Todo `MatriculaException` capturado vira `JOptionPane.showMessageDialog(..., JOptionPane.ERROR_MESSAGE)` com a mensagem da exceção; sucesso vira mensagem na barra de status (e opcionalmente um diálogo informativo).
- A tela nunca calcula regras (ex.: não decide sozinha se há vaga); ela **pergunta ao service** e **exibe** o resultado.

---

## 7. Persistência — Regras Específicas

- Apenas `java.nio.file.Files`/`Path` (preferencial) ou `java.io`. Nada de serialização Java (`ObjectOutputStream`).
- Formato, cabeçalhos e separador **exatamente** como em `ARCHITECTURE.md`, seção 7.
- O repositório **não** contém regra de negócio: ele apenas lê, converte e grava.
- Erros de formato devem gerar `IOException` com o **nome do arquivo e o número da linha**.

---

## 8. Testes — Regras Específicas

- Sem JUnit: os testes vivem em `app/CasosDeTesteRunner.java`.
- O runner deve usar **diretório temporário** e **nunca** tocar na pasta `dados/`.
- Cada caso deve imprimir: **ID, título, entrada, esperado, obtido e status (`PASSOU`/`FALHOU`)**, exatamente no formato de `TASKS.md`, Etapa 4.
- Um caso que lança exceção inesperada conta como `FALHOU` (com a mensagem impressa) e **não** interrompe os demais.
- Ao final, imprimir resumo e tabela Markdown; sair com código `0` (tudo passou) ou `1`.

---

## 9. Fluxo de Trabalho do Agente

1. Leia os 4 documentos.
2. Execute **apenas** a próxima etapa pendente de `TASKS.md`.
3. Compile com:
   ```powershell
   javac -Xlint:all -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
   ```
4. Rode a verificação descrita na etapa e mostre a saída ao usuário.
5. Marque os checkboxes concluídos em `TASKS.md`.
6. Sugira a mensagem de commit indicada na etapa e **aguarde** confirmação do usuário antes de seguir.

### O que o agente NUNCA deve fazer

- ❌ Gerar várias etapas de uma vez "para adiantar".
- ❌ Introduzir classes, pacotes ou dependências não previstos.
- ❌ Alterar assinaturas, mensagens ou o seed sem atualizar a documentação.
- ❌ Remover comentários didáticos existentes.
- ❌ Declarar uma etapa concluída sem ter compilado e executado a verificação.

---

## 10. Definição de Pronto (Projeto Completo)

- [x] Exatamente 9 arquivos `.java` nos pacotes corretos.
- [x] `javac -Xlint:all` sem erros e sem avisos.
- [x] `java -cp bin app.CasosDeTesteRunner` → **17/17 casos PASSOU**, código de saída `0`.
- [x] `java -cp bin app.Main` abre a janela e o roteiro de demonstração do `README.md` funciona do início ao fim.
- [x] Apagar `dados/` e reexecutar recria o seed corretamente.
- [x] Nenhum import proibido pela matriz da seção 3 (verificável com busca textual).
- [x] Todas as classes e métodos públicos com Javadoc e marcadores didáticos.
- [x] `ARCHITECTURE.md` corresponde 100% ao código final.
