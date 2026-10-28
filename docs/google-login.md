# Login e cadastro com Google

O fluxo usa OpenID Connect do Spring Security. A identidade é validada no backend; o frontend nunca recebe o segredo do cliente nem tokens do Google. O CorsaBox emite seu próprio access token e refresh cookie, como no login por senha.

## Configuração local

1. No Google Cloud, configure a tela de consentimento e crie um cliente OAuth do tipo **Aplicativo da Web**. Em modo de teste, adicione as contas que poderão entrar.
2. Cadastre exatamente esta URI de redirecionamento autorizada: `http://localhost:8080/login/oauth2/code/google`.
3. Configure no ambiente do backend:

```text
GOOGLE_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID=<client ID do Google>
GOOGLE_CLIENT_SECRET=<segredo do cliente>
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
APP_FRONTEND_URL=http://localhost:5173
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

4. Inicie o backend e o frontend normalmente. O Vite encaminha `/api` e `/oauth2` para o backend. O botão aparece quando `/api/auth/google/config` informa que o recurso está habilitado.

Não coloque o segredo em variáveis `VITE_*`, no repositório ou em URLs. Sem configuração, o recurso fica desabilitado e o login por senha continua disponível.

## Produção

Use `GOOGLE_REDIRECT_URI` com a URL pública HTTPS do **backend**, terminando em `/login/oauth2/code/google`, e cadastre a mesma URI no Google. Configure `APP_FRONTEND_URL` com a origem HTTPS exata do frontend, sem caminho, e autorize essa origem em `APP_CORS_ALLOWED_ORIGINS`.

O Spring guarda o estado/nonce e a identidade pendente em uma sessão temporária com cookie HttpOnly, válida por dez minutos. Ela não autentica chamadas à API. O perfil de produção exige cookie Secure. Prefira frontend e backend no mesmo site, por exemplo `www.corsabox.app` e `api.corsabox.app`, ou um proxy sob a mesma origem. Se estiverem em sites diferentes, `GOOGLE_SESSION_SAMESITE=none` é necessário; navegadores que bloqueiam cookies de terceiros ainda podem impedir o fluxo. O refresh cookie existente também precisa de configuração compatível com a topologia.

Com mais de uma instância do backend, é necessário manter afinidade de sessão durante o fluxo OAuth. A sessão temporária está em memória na instância que iniciou a autenticação.

## Regras e persistência

- Uma conta Google é reconhecida pelo `sub`, não pelo email. Alterações posteriores no email do Google não criam outra conta.
- Só identidades com email verificado pelo Google chegam à conclusão do cadastro.
- No primeiro acesso, nome, sobrenome e username são obrigatórios. Não há senha local nem novo código por email.
- Se o email já existe, a senha atual do CorsaBox é exigida antes do vínculo, com a proteção de tentativas do login existente. Recuperar a senha permite reiniciar o fluxo depois.
- `google_accounts` tem `subject` como chave primária e `user_id` como chave estrangeira única. Um vínculo existente não pode ser transferido ou substituído. A criação do usuário, vínculo e refresh token ocorre na mesma transação.
- A conclusão aceita apenas a origem do frontend configurado e consome a identidade pendente. Tokens nunca passam pela URL de retorno.

O projeto usa `spring.jpa.hibernate.ddl-auto=update` e mantém Flyway desabilitado. A nova tabela segue esse mecanismo existente, sem modificar linhas dos usuários atuais. Para desativar o recurso, defina `GOOGLE_LOGIN_ENABLED=false`; mantenha a tabela para preservar os vínculos.

## Verificação manual com credenciais de teste

1. Cadastre uma conta nova via Google: complete username/nome e confira o acesso ao catálogo, renovação da sessão e logout.
2. Entre novamente: a conta existente deve ser reutilizada.
3. Use uma conta Google cujo email já tenha cadastro por senha: senha incorreta deve falhar; senha correta deve vincular preservando o perfil e a biblioteca.
4. Tente um username ocupado, cancele no Google e deixe uma conclusão expirar: deve ser possível corrigir os dados ou reiniciar pelo login.
5. Confira que o login e o cadastro por email continuam funcionando.

Os testes automatizados usam identidades locais simuladas e H2; não acessam Google nem o banco de produção. A aprovação real da tela de consentimento e a troca de código com o Google precisam ser verificadas após configurar o cliente OAuth.
