# 🎓 Sistema Acadêmico — Associações entre Classes em Java

> Trabalho acadêmico de **Programação Orientada a Objetos (POO)** desenvolvido com
> **Spec-Driven Development**: a especificação (`ARCHITECTURE.md`, `AGENTS.md`, `TASKS.md`)
> vem antes do código e é a fonte da verdade para a implementação.

---

## 📌 Resumo do Tema

Este trabalho apresenta um sistema acadêmico desenvolvido em Java puro que modela as associações entre as classes `Aluno`, `Disciplina` e `Matricula`, sendo esta última uma classe associativa que materializa o relacionamento muitos-para-muitos entre alunos e disciplinas. O sistema permite realizar e cancelar matrículas, cadastrar novos alunos e disciplinas dinamicamente em tempo de execução e listar as disciplinas ativas de cada aluno, aplicando regras de negócio que impedem matrículas duplicadas e matrículas em disciplinas sem vagas. A consistência entre os objetos relacionados é mantida em memória por meio de encapsulamento e de métodos de vínculo controlados, e os dados são persistidos em arquivos CSV. Com interface gráfica Swing e casos de teste automatizados, o projeto evidencia as vantagens de distribuir responsabilidades entre classes associadas em vez de concentrá-las em uma única classe.

---

## 🎯 Objetivos

### Objetivo Geral

Desenvolver um sistema acadêmico em Java, orientado a objetos, que represente as associações entre `Aluno`, `Disciplina` e `Matricula`, mantendo a consistência entre os objetos relacionados e demonstrando os benefícios da divisão de responsabilidades entre classes associadas em comparação com a concentração de toda a lógica em uma única classe.

### Objetivos Específicos

1. **Modelar** as classes `Aluno`, `Disciplina` e `Matricula` com encapsulamento adequado, utilizando `Matricula` como **classe associativa** e respeitando as multiplicidades `1` e `0..*` definidas no diagrama UML.
2. **Implementar** as operações de realizar matrícula, cancelar matrícula, cadastrar novos alunos e disciplinas em tempo de execução e listar as disciplinas ativas de um aluno, garantindo a **consistência bidirecional** entre os objetos associados.
3. **Aplicar** regras de negócio que impeçam matrículas duplicadas, matrículas em disciplinas lotadas e duplicidade de RA ou código de disciplina, com tratamento por meio de uma **exceção personalizada** (`MatriculaException`).
4. **Organizar** o sistema em camadas (`model`, `service`, `repository`, `view`), separando a interface gráfica da lógica de negócio.
5. **Persistir** os dados em arquivos CSV utilizando apenas a API nativa do Java (`java.nio`), com carga automática de dados iniciais (*seed*).
6. **Construir** uma interface gráfica simples e intuitiva com Java Swing para demonstrar as operações.
7. **Validar** o comportamento do sistema por meio de casos de teste automatizados executados via console, cobrindo cenários de sucesso e de erro, cujos resultados subsidiam a seção *Análise de Resultados* do relatório.

---

## 🧰 Tecnologias e Restrições

| Item | Escolha |
|---|---|
| Linguagem | Java puro — **JDK 17 ou superior** |
| Build | `javac` / `java` da linha de comando (**sem Maven, sem Gradle**) |
| Bibliotecas externas | **Nenhuma** |
| Interface gráfica | Java Swing (`javax.swing`) |
| Persistência | Arquivos `.csv` com `java.nio.file` (UTF-8) |
| Testes | `CasosDeTesteRunner` (console, sem JUnit) |
| Tamanho | 9 arquivos `.java` (limite máximo: 10) |

---

## 🗂️ Estrutura do Projeto

```text
TrabalhoPOO/
├── README.md               ← este arquivo (visão geral e execução)
├── ARCHITECTURE.md         ← UML, responsabilidades e invariantes de consistência
├── AGENTS.md               ← regras obrigatórias para o agente codificador
├── TASKS.md                ← plano de implementação em 5 etapas
├── .gitignore
├── src/
│   ├── app/
│   │   ├── Main.java                    ← ponto de entrada da interface gráfica
│   │   └── CasosDeTesteRunner.java      ← testes automatizados via console
│   ├── model/
│   │   ├── Aluno.java
│   │   ├── Disciplina.java
│   │   └── Matricula.java               ← classe associativa (+ enum Status)
│   ├── service/
│   │   ├── SistemaAcademicoService.java ← regras de negócio
│   │   └── MatriculaException.java      ← exceção de regra de negócio
│   ├── repository/
│   │   └── ArquivoRepository.java       ← leitura/escrita CSV + seed
│   └── view/
│       └── TelaPrincipal.java           ← janela Swing
├── bin/                    ← (gerado) arquivos .class compilados
└── dados/                  ← (gerado) alunos.csv, disciplinas.csv, matriculas.csv
```

---

## ▶️ Como Executar

### Pré-requisito

Verifique se o JDK 17+ está instalado:

```bash
java -version
javac -version
```

### 1. Compilar

**Windows (PowerShell):**

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
```

**Windows (Prompt de Comando — CMD):**

```bat
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d bin @fontes.txt
```

**Linux / macOS / Git Bash:**

```bash
javac -encoding UTF-8 -d bin $(find src -name "*.java")
```

### 2. Executar a interface gráfica (Swing)

```bash
java -cp bin app.Main
```

Na primeira execução, a pasta `dados/` é criada automaticamente com os dados de demonstração (*seed*).

### 3. Executar os casos de teste automatizados (console)

```bash
java -cp bin app.CasosDeTesteRunner
```

> Os testes usam um **diretório temporário isolado**: eles nunca alteram os dados da pasta `dados/`.
> O código de saída é `0` se todos os casos passarem e `1` caso algum falhe.

### 4. Restaurar os dados de demonstração

Basta apagar a pasta `dados/` — o *seed* será recriado na próxima execução.

```powershell
Remove-Item -Recurse -Force dados      # PowerShell
```

```bash
rm -rf dados                           # Linux / macOS / Git Bash
```

> 💡 **Acentos no console do Windows:** se os caracteres acentuados aparecerem incorretos,
> execute `chcp 65001` antes de rodar os testes. Em JDK 18+, também é possível usar
> `java -Dstdout.encoding=UTF-8 -cp bin app.CasosDeTesteRunner`.

---

## 🌱 Dados de Demonstração (Seed)

Criados automaticamente quando a pasta `dados/` não existe. **Os casos de teste dependem destes valores** — não os altere sem atualizar `ARCHITECTURE.md` e `TASKS.md`.

**Alunos**

| RA | Nome |
|---|---|
| 2024001 | Ana Souza |
| 2024002 | Bruno Lima |
| 2024003 | Carla Mendes |
| 2024004 | Diego Rocha |
| 2024005 | Elisa Ferreira *(sem matrículas — ideal para a demonstração)* |

**Disciplinas**

| Código | Nome | Ocupação inicial |
|---|---|---|
| POO101 | Programação Orientada a Objetos | 3 / 30 |
| BD201 | Banco de Dados | 1 / 25 |
| ED301 | Estruturas de Dados | 1 / 20 |
| LAB401 | Laboratório de Robótica | **2 / 2 (LOTADA)** |

**Matrículas**

| ID | Aluno | Disciplina | Data | Status |
|---|---|---|---|---|
| 1 | Ana Souza | POO101 | 2026-02-10 | ATIVA |
| 2 | Ana Souza | BD201 | 2026-02-10 | ATIVA |
| 3 | Bruno Lima | POO101 | 2026-02-11 | ATIVA |
| 4 | Bruno Lima | LAB401 | 2026-02-11 | ATIVA |
| 5 | Carla Mendes | LAB401 | 2026-02-12 | ATIVA |
| 6 | Carla Mendes | ED301 | 2026-02-12 | CANCELADA |
| 7 | Diego Rocha | ED301 | 2026-02-13 | ATIVA |
| 8 | Diego Rocha | POO101 | 2026-02-13 | ATIVA |

---

## 🎤 Roteiro Sugerido de Apresentação (5–10 min)

| Tempo | Apresentador | Conteúdo |
|---|---|---|
| 0:00 – 1:00 | Integrante 1 | Problema: por que associar classes em vez de usar uma única classe "faz-tudo". |
| 1:00 – 3:00 | Integrante 2 | Diagrama UML (`ARCHITECTURE.md`): `Matricula` como classe associativa, multiplicidades `1` e `0..*`. |
| 3:00 – 6:00 | Integrante 3 | Demonstração na interface Swing (roteiro abaixo). |
| 6:00 – 8:00 | Integrante 4 | Execução do `CasosDeTesteRunner` e leitura da tabela de resultados. |
| 8:00 – 10:00 | Integrante 5 | Conclusão: invariantes de consistência, vantagens da divisão de responsabilidades. |

**Roteiro da demonstração na interface:**

1. Selecionar **Elisa Ferreira** → lista de disciplinas ativas vazia.
2. Matricular Elisa em **POO101** → sucesso; ocupação de POO101 vai de 3/30 para 4/30.
3. Selecionar **Ana Souza** e tentar matriculá-la em **POO101** → erro de **matrícula duplicada**.
4. Selecionar **Elisa** e tentar matriculá-la em **LAB401** → erro de **disciplina lotada** (2/2).
5. Selecionar **Bruno Lima** e **cancelar** sua matrícula em **LAB401** → ocupação vai para 1/2.
6. Voltar para **Elisa** e matriculá-la em **LAB401** → agora com sucesso (2/2).
7. Clicar em **+ Novo Aluno** (RA: `2024006`, Nome: `Fernanda Costa`) e em **+ Nova Disciplina** (Código: `WEB501`, Nome: `Desenvolvimento Web`, Limite: digitar texto ou `0` para ver o diálogo de aviso com reabertura de entrada, depois digitar `2`) → ambos aparecem de imediato nos combos e na tabela de ocupação.
8. Selecionar **Diego Rocha** e clicar em **\* Editar Aluno** → alterar nome para `Diego R. Rocha` (atualização imediata no combo e preservação das matrículas).
9. Selecionar **ED301** e clicar em **\* Editar Disciplina** → alterar limite de 20 para 25 (atualização na tabela).
10. Selecionar **LAB401** (2/2) e clicar em **\* Editar Disciplina** → tentar reduzir limite para 1 (bloqueado com aviso de vagas ocupadas).
11. Selecionar **Fernanda Costa** e matriculá-la em **WEB501** → sucesso (ocupação: 1/2).
12. Tentar remover **Fernanda Costa** ou **WEB501** → erro de integridade (bloqueado por possuir matrículas ativas).
13. Cancelar a matrícula de **Fernanda Costa** em **WEB501** e clicar em **- Remover Aluno** e **- Remover Disciplina** (com diálogo de confirmação) → ambos são removidos com sucesso.
14. Fechar e reabrir o programa → todos os dados, novos cadastros, edições e remoções persistem nos arquivos CSV.

---

## 📊 Análise de Resultados (para o relatório)

Execute `java -cp bin app.CasosDeTesteRunner`. A execução dos 17 casos de teste automatizados produz:

1. Um **bloco detalhado** por caso de teste (entrada, resultado esperado, resultado obtido, estado após a operação);
2. Uma **tabela em Markdown** pronta para ser copiada para a seção *Análise de Resultados* do relatório.

---

## 📚 Documentação do Projeto

| Documento | Para quê |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Diagrama de classes UML, responsabilidades, invariantes e decisões de projeto. |
| [AGENTS.md](AGENTS.md) | Regras obrigatórias para quem (humano ou IA) for escrever o código. |
| [TASKS.md](TASKS.md) | Plano de implementação em 5 etapas verificáveis. |

---

## 👥 Integrantes

| Nome | RA | Responsabilidade principal |
|---|---|---|
| _(preencher)_ | _(preencher)_ | Model e associações |
| _(preencher)_ | _(preencher)_ | Service e regras de negócio |
| _(preencher)_ | _(preencher)_ | Persistência CSV e seed |
| _(preencher)_ | _(preencher)_ | Casos de teste e relatório |
| _(preencher)_ | _(preencher)_ | Interface Swing |

