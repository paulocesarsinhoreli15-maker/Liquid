# Liquid IPTV (Liquid Glass)

App Android (Kotlin + Jetpack Compose + Media3) com a interface Liquid Glass preto e branco:
tema escuro/claro, efeito de foco (desfoca e tira a cor do resto), transições com desfoque
e a aba **Provedores** (Xtream Codes ou lista M3U).

## Gerar o APK pelo GitHub (sem instalar nada)
1. Crie um repositório no GitHub e envie todos os arquivos desta pasta (inclusive a pasta `.github`).
2. Abra a aba **Actions** > **Gerar APK** > **Run workflow** (ele também roda a cada envio).
3. Quando terminar, abra a execução e baixe **LiquidIPTV-apk** em *Artifacts*.
4. Descompacte o .zip e instale o `app-debug.apk` no Android (permita "fontes desconhecidas").

## Gerar o APK no computador
Android Studio (Koala ou mais novo) > File > Open > esta pasta > Build > Build APK(s).
Pela linha de comando: `./gradlew assembleDebug` (saída em app/build/outputs/apk/debug/).

## Como usar
Aba **Provedores** > Xtream Codes (servidor, usuário e senha — ou cole o link get.php completo)
ou Lista M3U (link) > Testar conexão > Salvar e usar.

O app é só um reprodutor: não fornece conteúdo. Use apenas provedores que você tem direito de usar.
