# Local secrets

Put local-only credentials in `api-key.env`. This file is ignored by Git and
must never be committed.

Set your provider configuration in the file:

```dotenv
LLM_API_KEY=replace-with-your-real-key
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o-mini
LLM_BASE_URL=
```

Start the local stack from the repository root with both configuration files:

```powershell
docker compose --env-file .env --env-file .secrets/api-key.env up --build
```

If you do not have a root `.env` yet, use `.env.example` instead:

```powershell
docker compose --env-file .env.example --env-file .secrets/api-key.env up --build
```

The second file takes precedence, so the key remains separate from the normal
local settings. To confirm that Compose received the variable without printing
the secret, run:

```powershell
docker compose --env-file .env --env-file .secrets/api-key.env config --quiet
```
