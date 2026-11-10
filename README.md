# Reserva — Organização de Recursos

Aplicação acadêmica para salas, professores e materiais, com Java 21, Spring Boot 3.5.16, PostgreSQL 16, interface responsiva em português e autenticação por sessão.

> **Estado da entrega:** a suíte automatizada roda no GitHub Actions (unidade, integração com PostgreSQL, API, E2E e carga). Veja `docs/relatorio-qualidade.md` para os resultados e as pendências, e `docs/evidencias/README.md` para o índice das evidências. O quality gate do SonarCloud está aprovado, com nota de segurança A. Isso não é uma declaração de homologação nem de ausência de vulnerabilidades.

## Começar pelo Docker (Windows, macOS ou Linux)

1. Instale e abra Docker Desktop com suporte a containers Linux (ou Docker Engine + Compose).
2. Extraia o ZIP. Abra a pasta `organizacao-recursos` no VS Code e abra o terminal nessa pasta.
3. Copie `.env.example` para `.env`:
   - PowerShell: `Copy-Item .env.example .env`
   - Linux/macOS: `cp .env.example .env`
4. Edite `.env`, escolhendo duas senhas próprias. `ADMIN_PASSWORD` precisa de pelo menos 12 caracteres. Não envie `.env` para o GitHub.
5. Execute `docker compose up --build -d`. O primeiro build baixa dependências e pode demorar alguns minutos.
6. Abra **http://localhost:8080**. Entre com usuário **admin** e a senha `ADMIN_PASSWORD`.
7. Em **Usuários**, crie uma conta `RESPONSAVEL` e uma conta `SOLICITANTE`. Em **Recursos**, cadastre salas, docentes e materiais.
8. Faça logout e entre com cada perfil para testar as permissões. Administrador não ganha automaticamente a permissão de responsável.

Para acompanhar erros: `docker compose logs -f app`. Para parar: `docker compose down`. Os dados ficam no volume. **`docker compose down -v` apaga o banco**: use somente em ambiente descartável. Alterar `ADMIN_PASSWORD` não troca a senha de uma conta já criada no banco.

## Alternativa com Java instalado

Instale **JDK 21** e Docker. Não é preciso instalar o Maven: o projeto traz o **Maven Wrapper** (`mvnw` no Linux e macOS, `mvnw.cmd` no Windows), que baixa o Maven 3.9.11 na primeira execução. Copie `.env` e configure as variáveis `DB_PASSWORD` e `ADMIN_PASSWORD` também no terminal (o Spring não lê `.env` automaticamente fora do Compose). Suba `docker compose up -d db` e execute `./mvnw spring-boot:run`.

## Testes

```sh
./mvnw test                 # JUnit e WireMock; não precisa de Docker
./mvnw -B verify            # inclui Testcontainers e HTTP caixa-preta; precisa de Docker
./mvnw -B verify -Pquality  # adiciona metas de 80% linhas / 70% branches
npm install
npx playwright install chromium
npm run test:e2e            # aplicação já deve estar em execução
```

No Windows (PowerShell ou CMD), troque `./mvnw` por `.\mvnw.cmd`, por exemplo `.\mvnw.cmd -B verify`.

Para E2E, defina `ADMIN_PASSWORD` com a mesma senha usada na aplicação. Use banco descartável: o teste cria registros reais. Resultados: `target/surefire-reports`, `target/failsafe-reports`, `target/site/jacoco/index.html` e `playwright-report/index.html`.

## O que está implementado

- Perfis SOLICITANTE, RESPONSAVEL e ADMIN; senhas BCrypt, sessão HttpOnly, CSRF, validação e CSP.
- Cadastro/edição/desativação de recursos; criação e ativação/desativação de usuários.
- Filtros por tipo, capacidade, localização, competência e disponibilidade.
- Reserva de **vários recursos na mesma transação**, alteração pelo dono e cancelamento antes do início.
- Agenda de professor usando a mesma regra de exclusão de sala/material.
- Restrições PostgreSQL que impedem sobreposição inclusive sob concorrência; manutenção ocupa a mesma agenda.
- Aprovação de recursos restritos somente por RESPONSAVEL; alterações exigem reavaliação.
- Retirada/devolução, proibição de conclusão com materiais pendentes, estados e auditoria append-only.
- Notificações simuladas persistidas. Integração opcional por webhook com timeout e sem redirecionamento.
- Relatórios de horas previstas, reservas aceitas por recurso e conflitos evitados pela restrição de agenda.

## Decisões de escopo

Cada material é uma **unidade física individual** (ex.: Projetor 01). Não existe estoque quantitativo de materiais intercambiáveis. Capacidade é metadado de filtro; a reserva ocupa o recurso inteiro. Horários usam intervalo **[início, fim)**: 10h–11h e 11h–12h são compatíveis. Dados armazenados como instantes com timezone, exibidos no fuso do navegador. Solicitações pendentes também bloqueiam a agenda. Não há recorrência ou recuperação de senha nesta versão.

Recursos sem aprovação seguem automaticamente para APROVADA ao serem criados; recursos restritos ficam SOLICITADA. Uso e ausência são registrados pelo responsável, não por um agendador automático. Reservas não têm exclusão física. Veja regras completas em `docs/REGRAS.md`.

## GitHub e SonarCloud

1. Crie um repositório e envie **o conteúdo desta pasta**, incluindo `.github/`. Não envie `.env` ou `target/`.
2. Crie o projeto no SonarCloud conectado ao repositório e desative análise automática se usar o scanner Maven.
3. No GitHub, configure os secrets `SONAR_TOKEN`, `DB_PASSWORD` e `ADMIN_PASSWORD` e as variables `SONAR_PROJECT_KEY` e `SONAR_ORGANIZATION`. O secret `ADMIN_PASSWORD` segue a mesma regra do `.env`: pelo menos 12 caracteres, senão a aplicação não sobe e o job `e2e` falha (defeito D10).
4. Ative proteção de `main`, revisão por colega e checks obrigatórios. Abra um PR para executar `.github/workflows/ci.yml`.
5. O pipeline exige as metas de cobertura e o quality gate. Sem credenciais/configuração ou sem atingir as metas, ficará vermelho: isso é uma pendência real, não deve ser ocultada.
6. Baixe os artifacts do Actions e registre links/commit na RTM e no relatório final.

## Estrutura e documentação

- `src/main/java`: API, domínio, segurança, notificações.
- `src/main/resources/db/migration`: migração PostgreSQL/Flyway.
- `src/main/resources/static`: interface HTML/CSS/JavaScript sem CDN.
- `src/test`: testes JUnit, parametrizados, integração, concorrência, HTTP e WireMock.
- `e2e`: testes Playwright; `load`: plano JMeter e verificação objetiva.
- `docs/README.md`: índice de toda a documentação.
- `docs/rtm.md`: requisito → risco → teste → evidência. `docs/status-requisitos.md`: situação de cada requisito.
- `docs/API.md`, `docs/ARQUITETURA.md`, `docs/PLANO-TESTES.md`, `docs/defeitos/README.md`, `docs/relatorio-qualidade.md`, `docs/testes/mutacao-manual.md`, `docs/performance/resultado-01.md`, `docs/seguranca/matriz-autorizacao.md`.
- `docs/Apresentacao-Recursos.pptx`: slides editáveis; `docs/APRESENTACAO.md`: roteiro da demonstração.
- `docs/ENTREGA.md`: lista do que a equipe precisa executar, revisar e completar.

A autoria e as revisões por pares precisam ser registradas pela equipe real. Não foi inventado histórico de commits nem de revisão. O grupo deve compreender, revisar e adaptar o código antes da defesa oral.
