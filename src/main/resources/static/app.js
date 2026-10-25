"use strict";
const $ = (s) => document.querySelector(s);
let csrf,
  me,
  page = "bookings";
const esc = (s) =>
  String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const date = (s) => new Date(s).toLocaleString("pt-BR");
const role = (r) => me?.roles.some((a) => a.authority === "ROLE_" + r);
const local = (s) => {
  const d = new Date(s);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};
function message(s, error = false) {
  $("#message").textContent = s;
  $("#message").className = error ? "error" : "";
  $("#message").hidden = false;
  setTimeout(() => ($("#message").hidden = true), 6500);
}
async function token() {
  csrf = await (await fetch("/api/csrf")).json();
}
async function api(path, method = "GET", body) {
  if (!csrf) await token();
  const headers = {};
  if (method !== "GET") headers[csrf.headerName] = csrf.token;
  if (body) headers["Content-Type"] = "application/json";
  const response = await fetch("/api" + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    let data;
    try {
      data = await response.json();
    } catch {
      data = {};
    }
    if (response.status === 401) {
      $("#shell").hidden = true;
      $("#login").hidden = false;
    }
    throw new Error(
      data.message ||
        {
          401: "Entre com usuário e senha válidos.",
          403: "Você não tem permissão para essa operação.",
        }[response.status] ||
        "Não foi possível concluir a operação. Revise os dados.",
    );
  }
  return response.status === 204 ||
    response.headers.get("content-length") === "0"
    ? null
    : response.text().then((t) => (t ? JSON.parse(t) : null));
}
async function safe(fn) {
  try {
    await fn();
  } catch (e) {
    message(e.message, true);
  }
}
$("#login-form").onsubmit = (e) => {
  e.preventDefault();
  safe(async () => {
    await token();
    const r = await fetch("/api/login", {
      method: "POST",
      headers: { [csrf.headerName]: csrf.token },
      body: new URLSearchParams(new FormData(e.target)),
    });
    if (!r.ok) throw Error("Usuário ou senha inválidos.");
    e.target.reset();
    await token();
    await boot();
  });
};
$("#logout").onclick = () =>
  safe(async () => {
    await api("/logout", "POST");
    me = null;
    $("#shell").hidden = true;
    $("#login").hidden = false;
    await token();
  });
$("#close").onclick = () => $("#dialog").close();
async function boot() {
  me = await api("/me");
  $("#login").hidden = true;
  $("#shell").hidden = false;
  $("#who").textContent = me.username;
  $("#role").textContent = me.roles
    .map((r) => r.authority.replace("ROLE_", ""))
    .join(", ");
  const tabs = [
    ["bookings", "▦ Reservas"],
    ["resources", "◇ Recursos"],
    ["maintenance", "◷ Manutenção"],
    ["notifications", "◉ Notificações"],
  ];
  if (role("RESPONSAVEL")) tabs.push(["movements", "⇄ Materiais"]);
  if (role("ADMIN")) tabs.push(["users", "♙ Usuários"]);
  if (role("ADMIN") || role("RESPONSAVEL"))
    tabs.push(["reports", "▥ Relatórios"], ["audit", "≡ Auditoria"]);
  $("#nav").innerHTML = tabs
    .map(([id, label]) => `<button data-page="${id}">${label}</button>`)
    .join("");
  $("#nav").onclick = (e) => {
    const b = e.target.closest("[data-page]");
    if (b) safe(() => render(b.dataset.page));
  };
  await render("bookings");
}
function toolbar(title, button = "") {
  return `<div class="toolbar"><div><h2>${title}</h2><p class="muted">Consulte, organize e acompanhe suas alocações.</p></div>${button}</div>`;
}
function empty(s) {
  return `<div class="empty">${s}</div>`;
}
function table(rows, columns) {
  return rows.length
    ? `<div class="table-wrap"><table><thead><tr>${columns.map(([k, t]) => `<th>${t}</th>`).join("")}</tr></thead><tbody>${rows.map((r) => `<tr>${columns.map(([k]) => `<td>${esc(r[k])}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`
    : empty("Nenhum registro por aqui ainda.");
}
async function render(next = page) {
  page = next;
  $("#nav")
    .querySelectorAll("button")
    .forEach((b) => b.classList.toggle("selected", b.dataset.page === page));
  $("#page-title").textContent = {
    bookings: "Suas reservas",
    resources: "Explore os recursos",
    maintenance: "Bloqueios e manutenção",
    notifications: "Notificações",
    movements: "Retiradas e devoluções",
    users: "Gestão de usuários",
    reports: "Utilização dos recursos",
    audit: "Histórico auditável",
  }[page];
  const box = $("#content");
  if (page === "bookings") {
    const rows = await api("/bookings");
    box.innerHTML =
      toolbar(
        "Agenda de reservas",
        '<button id="new">+ Nova reserva</button>',
      ) +
      (rows.length
        ? `<div class="cards">${rows.map((b) => `<article class="card"><span class="badge">${esc(b.state)}</span><span class="muted">#${b.id}</span><h3>${esc(b.title)}</h3><p>${esc(b.resources)}<br>${date(b.start_at)}<br>até ${date(b.end_at)}</p><span class="muted">Solicitante: ${esc(b.owner)}</span><div class="actions">${b.owner === me.username && ["SOLICITADA", "APROVADA"].includes(b.state) ? `<button class="ghost" data-edit="${b.id}">Editar</button><button class="ghost" data-state="CANCELADA" data-id="${b.id}">Cancelar</button>` : ""}${role("RESPONSAVEL") ? (b.state === "SOLICITADA" ? `<button data-state="APROVADA" data-id="${b.id}">Aprovar</button><button class="ghost" data-state="REJEITADA" data-id="${b.id}">Rejeitar</button>` : b.state === "APROVADA" ? `<button data-state="EM_USO" data-id="${b.id}">Iniciar uso</button><button class="ghost" data-state="NAO_COMPARECEU" data-id="${b.id}">Não compareceu</button>` : b.state === "EM_USO" ? `<button data-state="CONCLUIDA" data-id="${b.id}">Concluir</button><button class="ghost" data-move="${b.id}">Movimentar material</button>` : "") : ""}</div></article>`).join("")}</div>`
        : empty("Sua agenda está pronta. Crie a primeira reserva."));
    $("#new").onclick = () => safe(() => bookingForm());
    box
      .querySelectorAll("[data-edit]")
      .forEach(
        (b) =>
          (b.onclick = () =>
            safe(() =>
              bookingForm(rows.find((r) => r.id === Number(b.dataset.edit))),
            )),
      );
    box.querySelectorAll("[data-state]").forEach(
      (b) =>
        (b.onclick = () =>
          safe(async () => {
            if (!confirm("Confirmar mudança para " + b.dataset.state + "?"))
              return;
            await api(`/bookings/${b.dataset.id}/state`, "POST", {
              state: b.dataset.state,
            });
            message("Estado atualizado.");
            await render();
          })),
    );
    box
      .querySelectorAll("[data-move]")
      .forEach(
        (b) => (b.onclick = () => safe(() => movementForm(b.dataset.move))),
      );
  }
  if (page === "resources") {
    box.innerHTML =
      toolbar(
        "Encontre o que precisa",
        role("ADMIN") ? '<button id="new">+ Cadastrar recurso</button>' : "",
      ) +
      `<form id="filter" class="filters"><label>Tipo<select name="type"><option value="">Todos</option><option>SALA</option><option>PROFESSOR</option><option>MATERIAL</option></select></label><label>Localização<input name="location"></label><label>Competência<input name="competence"></label><label>Capacidade mínima<input name="capacity" type="number" min="1" value="1"></label><label>Disponível de<input name="start" type="datetime-local"></label><label>Até<input name="end" type="datetime-local"></label><button>Pesquisar</button></form><div id="resource-list"></div>`;
    async function list(query = "") {
      const rows = await api(
        "/resources" +
          query +
          (role("ADMIN") ? (query ? "&" : "?") + "includeInactive=true" : ""),
      );
      $("#resource-list").innerHTML = rows.length
        ? `<div class="cards">${rows.map((r) => `<article class="card"><span class="badge">${esc(r.type)}</span>${r.restricted ? '<span class="badge">Restrito</span>' : ""}${!r.active ? '<span class="badge">Inativo</span>' : ""}<h3>${esc(r.name)}</h3><p>${esc(r.location)} · Capacidade: ${r.capacity}<br>${esc(r.competence)}</p><span class="muted">ID #${r.id}</span>${role("ADMIN") ? `<div class="actions"><button class="ghost" data-resource="${r.id}">Editar</button></div>` : ""}</article>`).join("")}</div>`
        : empty("Nenhum recurso encontrado com esses filtros.");
      box
        .querySelectorAll("[data-resource]")
        .forEach(
          (b) =>
            (b.onclick = () =>
              resourceForm(
                rows.find((r) => r.id === Number(b.dataset.resource)),
              )),
        );
    }
    $("#filter").onsubmit = (e) => {
      e.preventDefault();
      safe(async () => {
        const data = Object.fromEntries(new FormData(e.target));
        for (const k of ["start", "end"])
          if (data[k]) data[k] = new Date(data[k]).toISOString();
          else delete data[k];
        await list("?" + new URLSearchParams(data));
      });
    };
    if ($("#new")) $("#new").onclick = () => resourceForm();
    await list();
  }
  if (page === "users") {
    const rows = await api("/users");
    box.innerHTML =
      toolbar("Contas da equipe", '<button id="new">+ Novo usuário</button>') +
      table(rows, [
        ["id", "ID"],
        ["username", "Usuário"],
        ["role", "Perfil"],
        ["enabled", "Ativo"],
      ]) +
      '<div class="actions">' +
      rows
        .filter((r) => r.username !== me.username)
        .map(
          (r) =>
            `<button class="ghost" data-enable="${r.id}" data-value="${!r.enabled}">${r.enabled ? "Desativar" : "Ativar"} ${esc(r.username)}</button>`,
        )
        .join("") +
      "</div>";
    $("#new").onclick = () =>
      openForm(
        "Novo usuário",
        field("username", "Usuário") +
          field("password", "Senha (mínimo 12 caracteres)", "password") +
          select("role", "Perfil", ["SOLICITANTE", "RESPONSAVEL", "ADMIN"]),
        async (d) => api("/users", "POST", d),
      );
    box.querySelectorAll("[data-enable]").forEach(
      (b) =>
        (b.onclick = () =>
          safe(async () => {
            await api("/users/" + b.dataset.enable + "/enabled", "PUT", {
              enabled: b.dataset.value === "true",
            });
            await render();
          })),
    );
  }
  if (page === "maintenance") {
    const rows = await api("/maintenance");
    box.innerHTML =
      toolbar(
        "Indisponibilidades programadas",
        role("ADMIN") ? '<button id="new">+ Novo bloqueio</button>' : "",
      ) +
      table(
        rows.map((r) => ({
          ...r,
          start_at: date(r.start_at),
          end_at: date(r.end_at),
        })),
        [
          ["id", "ID"],
          ["name", "Recurso"],
          ["reason", "Motivo"],
          ["start_at", "Início"],
          ["end_at", "Fim"],
        ],
      );
    if (role("ADMIN")) {
      box.innerHTML +=
        '<div class="actions">' +
        rows
          .map(
            (r) =>
              `<button class="ghost" data-unblock="${r.id}">Remover bloqueio #${r.id}</button>`,
          )
          .join("") +
        "</div>";
      $("#new").onclick = () =>
        safe(async () => {
          const resources = await api("/resources");
          openForm(
            "Bloquear recurso",
            resourceSelect(resources) + field("reason", "Motivo") + dates(),
            async (d) =>
              api("/maintenance", "POST", {
                ...d,
                resource: Number(d.resource),
                start: new Date(d.start).toISOString(),
                end: new Date(d.end).toISOString(),
              }),
          );
        });
      box.querySelectorAll("[data-unblock]").forEach(
        (b) =>
          (b.onclick = () =>
            safe(async () => {
              if (confirm("Remover este bloqueio?")) {
                await api("/maintenance/" + b.dataset.unblock, "DELETE");
                await render();
              }
            })),
      );
    }
  }
  if (page === "notifications") {
    const rows = await api("/notifications");
    box.innerHTML =
      toolbar("Avisos da sua conta") +
      table(
        rows.map((r) => ({ ...r, created_at: date(r.created_at) })),
        [
          ["message", "Mensagem"],
          ["delivery", "Entrega"],
          ["created_at", "Recebida em"],
        ],
      );
  }
  if (page === "audit") {
    const rows = await api("/audit");
    box.innerHTML =
      toolbar("Últimas 500 alterações") +
      table(
        rows.map((r) => ({ ...r, created_at: date(r.created_at) })),
        [
          ["id", "ID"],
          ["actor", "Autor"],
          ["action", "Ação"],
          ["entity_id", "Entidade"],
          ["detail", "Detalhes"],
          ["created_at", "Data"],
        ],
      );
  }
  if (page === "movements") {
    const rows = await api("/movements");
    box.innerHTML =
      toolbar("Registro de materiais") +
      "<p>Para registrar retirada ou devolução, abra uma reserva em uso e selecione “Movimentar material”.</p>" +
      table(
        rows.map((r) => ({ ...r, created_at: date(r.created_at) })),
        [
          ["booking_id", "Reserva"],
          ["resource_id", "Material"],
          ["kind", "Movimento"],
          ["actor", "Responsável"],
          ["created_at", "Data"],
        ],
      );
  }
  if (page === "reports") {
    const r = await api("/reports");
    box.innerHTML =
      `<div class="stats"><div class="card"><span>Conflitos evitados</span><strong>${r.avoidedConflicts}</strong></div><div class="card"><span>Recursos cadastrados</span><strong>${r.resources.length}</strong></div></div>` +
      table(r.resources, [
        ["name", "Recurso"],
        ["type", "Tipo"],
        ["reservations", "Reservas aceitas"],
        ["hours", "Horas alocadas"],
      ]) +
      '<p class="muted">Horas previstas de reservas aprovadas, em uso e concluídas. Não representam presença efetiva. Manutenções e solicitações pendentes não entram no total.</p>';
  }
}
function field(name, label, type = "text", value = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" required></label>`;
}
function select(name, label, options, value) {
  return `<label>${label}<select name="${name}">${options.map((o) => `<option ${o === value ? "selected" : ""}>${o}</option>`).join("")}</select></label>`;
}
function dates(b) {
  return (
    '<div class="row">' +
    field("start", "Início", "datetime-local", b ? local(b.start_at) : "") +
    field("end", "Término", "datetime-local", b ? local(b.end_at) : "") +
    "</div>"
  );
}
function resourceSelect(rows) {
  return `<label>Recurso<select name="resource">${rows.map((r) => `<option value="${r.id}">#${r.id} · ${esc(r.name)}</option>`).join("")}</select></label>`;
}
function openForm(title, html, save) {
  $("#dialog-title").textContent = title;
  $("#fields").innerHTML = html;
  $("#editor").onsubmit = (e) => {
    e.preventDefault();
    safe(async () => {
      const button = $("#save");
      button.disabled = true;
      try {
        const data = Object.fromEntries(new FormData(e.target));
        await save(data, e.target);
        $("#dialog").close();
        message("Operação concluída.");
        await render();
      } finally {
        button.disabled = false;
      }
    });
  };
  $("#dialog").showModal();
}
async function bookingForm(b) {
  const resources = await api("/resources");
  openForm(
    b ? "Alterar reserva #" + b.id : "Nova reserva",
    field("title", "Título", "text", b?.title || "") +
      dates(b) +
      "<label>Selecione todos os recursos necessários</label>" +
      (b
        ? '<p class="muted">Selecione novamente os recursos. A alteração será reavaliada e poderá exigir aprovação.</p>'
        : "") +
      `<div class="checks">${resources.map((r) => `<label><input type="checkbox" name="resources" value="${r.id}">${esc(r.name)} · ${esc(r.type)}${r.restricted ? " · restrito" : ""}</label>`).join("")}</div>`,
    async (d, f) => {
      const ids = new FormData(f).getAll("resources").map(Number);
      if (!ids.length) throw Error("Selecione pelo menos um recurso.");
      await api("/bookings" + (b ? "/" + b.id : ""), b ? "PUT" : "POST", {
        title: d.title,
        start: new Date(d.start).toISOString(),
        end: new Date(d.end).toISOString(),
        resources: ids,
      });
    },
  );
}
function resourceForm(r) {
  openForm(
    r ? "Editar recurso #" + r.id : "Cadastrar recurso",
    field("name", "Nome", "text", r?.name || "") +
      select("type", "Tipo", ["SALA", "PROFESSOR", "MATERIAL"], r?.type) +
      field("capacity", "Capacidade", "number", r?.capacity || 1) +
      field("location", "Localização", "text", r?.location || "") +
      `<label>Competência<input name="competence" value="${esc(r?.competence || "")}"></label><label><input type="checkbox" name="restricted" ${r?.restricted ? "checked" : ""}> Exige aprovação do responsável</label><label><input type="checkbox" name="active" ${!r || r.active ? "checked" : ""}> Ativo</label>`,
    async (d) =>
      api("/resources" + (r ? "/" + r.id : ""), r ? "PUT" : "POST", {
        ...d,
        capacity: Number(d.capacity),
        restricted: !!d.restricted,
        active: !!d.active,
      }),
  );
}
async function movementForm(id) {
  const resources = (await api("/resources")).filter(
    (r) => r.type === "MATERIAL",
  );
  openForm(
    "Movimentar material · reserva #" + id,
    resourceSelect(resources) +
      select("kind", "Operação", ["RETIRADA", "DEVOLUCAO"]),
    async (d) =>
      api("/bookings/" + id + "/movements", "POST", {
        resource: Number(d.resource),
        kind: d.kind,
      }),
  );
}
safe(async () => {
  await token();
  try {
    await boot();
  } catch (e) {
    if (!e.message.includes("Entre")) message(e.message, true);
  }
});
