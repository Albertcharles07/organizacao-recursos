# API e fluxos públicos

Base: `http://localhost:8080/api`. JSON UTF-8; instantes ISO-8601 com offset, como `2026-12-01T14:00:00-03:00`. Os horários de exemplo devem ser substituídos por datas futuras.

## Autenticação de sessão e CSRF

1. `GET /csrf` retorna `token` e `headerName` e estabelece sessão. Preserve os cookies.
2. `POST /login`, Content-Type `application/x-www-form-urlencoded`, campos `username` e `password`, enviando o token no header retornado.
3. Sucesso: 204. Busque **novo** token com `GET /csrf` após login, pois a autenticação renova a proteção.
4. Em toda mutação, envie cookie de sessão e header CSRF. `POST /logout` encerra a sessão.
5. `GET /me` retorna username e authorities. Conta desativada perde acesso no próximo request.

## Endpoints

| Método | Caminho | Entrada / comportamento |
|---|---|---|
| GET | `/resources` | Filtros `type`, `capacity`, `location`, `competence`, `start`, `end`; início e fim devem vir juntos. `includeInactive=true` é exclusivo de ADMIN |
| POST | `/resources` | Cadastro (ADMIN); retorna `{id}` |
| PUT | `/resources/{id}` | Edição/desativação (ADMIN), sem alocações futuras ativas |
| GET/POST | `/users` | Lista sem hashes / cria usuário (ADMIN) |
| PUT | `/users/{id}/enabled` | `{ "enabled": false }`; não desativar a própria conta |
| GET | `/bookings` | Lista conforme escopo do perfil |
| POST | `/bookings` | Cria reserva; retorna `{id}` |
| PUT | `/bookings/{id}` | Substitui dados da reserva própria futura; retorna `{id}` |
| POST | `/bookings/{id}/state` | `{ "state": "APROVADA" }`; regras em REGRAS.md |
| POST | `/bookings/{id}/movements` | `{ "resource": 3, "kind": "RETIRADA" }` ou DEVOLUCAO |
| GET | `/movements` | Registro de retiradas e devoluções (RESPONSAVEL) |
| GET/POST | `/maintenance` | Lista bloqueios ativos / cria bloqueio (ADMIN) |
| DELETE | `/maintenance/{id}` | Desativa bloqueio sem apagar histórico (ADMIN) |
| GET | `/audit` | Últimas 500 entradas (ADMIN/RESPONSAVEL) |
| GET | `/notifications` | Últimas 100 notificações próprias |
| GET | `/reports` | Utilização por recurso e total de conflitos (ADMIN/RESPONSAVEL) |

### Cadastro de recurso

```json
{"name":"Projetor 01","type":"MATERIAL","capacity":1,"location":"Bloco A","competence":"Projeção HDMI","restricted":true,"active":true}
```

Tipos: SALA, PROFESSOR, MATERIAL. Para docente, competence contém disciplina/habilidade; localização pode ser a unidade/campus.

### Criação/alteração de reserva

```json
{"title":"Aula de Java","start":"2026-12-01T14:00:00-03:00","end":"2026-12-01T16:00:00-03:00","resources":[1,2,3]}
```

Até 30 IDs, positivos e existentes. Duplicatas são removidas. Para alterar, envie a lista completa novamente. ID vem do banco; nunca é gerado pelo navegador.

### Bloqueio e usuário

```json
{"resource":1,"start":"2026-12-02T08:00:00-03:00","end":"2026-12-02T12:00:00-03:00","reason":"Manutenção elétrica"}
```

```json
{"username":"coordenacao","password":"substituir-por-senha-propria","role":"RESPONSAVEL"}
```

### Respostas

Operações com retorno: 200. Mutações sem conteúdo: 200 vazio; login/logout: 204. Erros de domínio têm `{ "status": 409, "message": "Horário indisponível: existe reserva ou manutenção para um dos recursos." }`.

- 400: formato/regra inválida, recurso ausente, transição impossível.
- 401: sessão ausente, login incorreto ou conta desativada.
- 403: perfil insuficiente, dono diferente ou CSRF inválido.
- 409: sobreposição, duplicata ou integridade incompatível.
- 500: falha inesperada, sem detalhes de SQL/stacktrace expostos.

Erros anteriores ao controller (Spring Security) usam o formato de erro padrão do Spring; clientes devem lidar com o status sem depender da presença de `message`. Não existe endpoint de exclusão de reservas.

## Webhook opcional

Defina `NOTIFICATION_URL` no ambiente (e repasse em compose.yaml se usar Compose). Sem URL, a notificação fica SIMULADA. Com URL, após o commit é enviado POST com `{ "bookingId": 123 }`, timeout de conexão 3s e total 5s. Não envia dados pessoais. 2xx marca ENVIADA; falha marca FALHOU. Não há reenvio automático nesta versão; a notificação interna permanece acessível.

Use URL fixa administrada pela equipe, nunca URL recebida do solicitante. WireMock testa payload, falha e não seguimento de redirects.
