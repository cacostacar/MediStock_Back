# Colocar a documentação no MediStock_Back

Este pacote reúne o README, os 12 prints originais autorizados para uso, o DER, o dicionário de dados, os scripts Oracle e um modelo de variáveis de ambiente.

## Copiar os arquivos

1. Extraia o ZIP em uma pasta temporária.
2. Abra `C:\Projetos\MediStock_Back` no Antigravity.
3. Copie `README.md` para a raiz do projeto, ao lado de `pom.xml`. Guarde uma cópia do README anterior caso ele contenha outras informações que você queira preservar.
4. Mescle a pasta `docs` com a pasta de mesmo nome do projeto. Ela deve conter o DER, o dicionário e `evidencias` com as 12 imagens.
5. Mescle `database/oracle` com os scripts da integração. Os scripts do pacote usam um marcador de senha no arquivo 00, próprio para versionamento.
6. Copie `.env.example` para a raiz. Seu `.env` local continua sendo a configuração utilizada pela aplicação.
7. Abra `README.md` na prévia Markdown do Antigravity e confira as imagens. Cada anexo está em uma seção expansível.

Os scripts de criação e carga são material de reprodução do ambiente. Como seu banco já está implantado, copiar os arquivos para o projeto é suficiente para esta etapa de documentação.

## Versionar a documentação

No terminal do projeto:

```powershell
git status --short
git add README.md docs database .env.example
git diff --cached --stat
```

Para entregar a integração completa, confira também as alterações Java realizadas no Antigravity, o `pom.xml` e `application-oracle.properties`. Se ainda estiverem apenas no seu computador, inclua os arquivos correspondentes pelo painel de Controle de Código-Fonte, revisando as diferenças.

O `.env` local deve estar no `.gitignore`; o arquivo destinado ao repositório é `.env.example`.

Quando a seleção de arquivos estiver correta:

```powershell
git commit -m "Documenta integracao Oracle e PL/SQL do MediStock"
git push
```

O `git push` utiliza o remoto e a branch configurados no seu checkout e exige acesso de escrita ao destino.

## Conferir no GitHub

Abra a página inicial do repositório e confira:

- O README aparece com as seções de execução, modelagem, PL/SQL e testes.
- O DER abre corretamente.
- Os 12 anexos exibem suas imagens ao expandir cada seção.
- Os links para o dicionário e os seis scripts SQL funcionam.
- O código Java e a configuração Oracle utilizados nos testes estão incluídos na entrega.

As imagens foram copiadas sem alterações de conteúdo. O anexo 9 foi legendado como registro de histórico de consumo. O anexo 12 mostra a conferência da quantidade válida gravada; a rejeição de quantidade negativa aparece no teste PL/SQL do anexo 8.
