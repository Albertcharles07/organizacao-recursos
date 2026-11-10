# Comece aqui

1. Abra esta pasta no VS Code.
2. Instale/abra Docker Desktop (containers Linux).
3. Copie `.env.example` para `.env` e escolha senhas próprias. A senha ADMIN_PASSWORD precisa de pelo menos 12 caracteres.
4. Abra o terminal nesta pasta e execute `docker compose up --build -d`.
5. Acesse http://localhost:8080 e entre com `admin` e a senha que definiu.
6. Cadastre contas RESPONSAVEL e SOLICITANTE em Usuários e cadastre seus recursos.

O passo a passo completo está no README.md. Os slides estão em docs/Apresentacao-Recursos.pptx.

**Antes de entregar à faculdade:** leia docs/relatorio-qualidade.md e docs/ENTREGA.md. A suíte completa (unidade, integração, E2E e carga) roda no GitHub Actions; falta a análise do SonarCloud.
