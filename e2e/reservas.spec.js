const { test, expect } = require("@playwright/test");
test("administrador cadastra sala e cria reserva pela interface", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByLabel("Usuário", { exact: true }).fill("admin");
  await page
    .getByLabel("Senha", { exact: true })
    .fill(process.env.ADMIN_PASSWORD || "e2e-admin-password-123");
  await page.getByRole("button", { name: "Entrar na plataforma" }).click();
  await expect(
    page.getByRole("heading", { name: "Suas reservas" }),
  ).toBeVisible();
  await page.locator("nav").getByRole("button", { name: "Recursos" }).click();
  await page.getByRole("button", { name: "Cadastrar recurso" }).click();
  const name = "Sala E2E " + Date.now();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Nome", { exact: true }).fill(name);
  await dialog.getByLabel("Capacidade", { exact: true }).fill("30");
  await dialog.getByLabel("Localização", { exact: true }).fill("Bloco E2E");
  await dialog.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  await page.locator("nav").getByRole("button", { name: "Reservas" }).click();
  await page.getByRole("button", { name: "Nova reserva" }).click();
  await dialog.getByLabel("Título", { exact: true }).fill("Aula de teste E2E");
  const start = new Date(Date.now() + 86400000),
    end = new Date(start.getTime() + 3600000);
  const fmt = (d) =>
    new Date(d.getTime() - d.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
  await dialog.getByLabel("Início", { exact: true }).fill(fmt(start));
  await dialog.getByLabel("Término", { exact: true }).fill(fmt(end));
  await dialog.getByRole("checkbox", { name: new RegExp(name) }).check();
  await dialog.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Aula de teste E2E" }).last(),
  ).toBeVisible();
  await expect(
    page
      .locator("article")
      .filter({ hasText: name })
      .getByText("APROVADA", { exact: true }),
  ).toBeVisible();
});
test("layout móvel mantém login acessível", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Entrar na plataforma" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
});
