# PS2 EMULATOR PRO

Projeto Android inicial para o **PS2 EMULATOR PRO** — *SEU PLAYSTATION 2 NA PALMA DA SUA MÃO.*

> **Estado real:** este repositório contém uma interface Android funcional de demonstração e a estrutura inicial para integração nativa. Não contém um núcleo de emulação de PlayStation 2. Portanto, não executa jogos, não implementa compatibilidade, não cria save states e não deve ser apresentado como emulador funcional. A tela de jogos informa essa limitação antes de qualquer tentativa de iniciar um título.

## O que já está implementado

- Projeto Android em Kotlin com Jetpack Compose, tema escuro e navegação Início/Jogos/Desempenho/Controles/Configurações.
- Seleção de arquivos ISO por Storage Access Framework (SAF), concessão persistente de acesso e biblioteca local pesquisável com favoritos e tamanho quando disponibilizado pelo provedor.
- Seleção e armazenamento da referência de uma BIOS do usuário. O app não inclui nem distribui BIOS ou jogos.
- Painel com dados que o Android fornece: memória, armazenamento, núcleos lógicos, modelo de SoC quando disponível, estado/carga e temperatura da bateria quando disponível, e recursos Vulkan/OpenGL ES reportados pelo sistema.
- Preferências visuais e controles de demonstração identificados como não conectados ao motor de jogo.
- Esqueleto C++/JNI em `app/src/main/cpp`; é somente uma fronteira de integração, não contém Emotion Engine, Graphics Synthesizer, VU, áudio ou qualquer código de emulação.

## Gerar APK de desenvolvimento

Abra o projeto no Android Studio (com Android SDK, JDK 17 e NDK/CMake instalados), sincronize o Gradle e execute **Build > Build APK(s)**.

## Próximo trabalho necessário para um emulador real

1. Selecionar e auditar um núcleo de PS2 que permita uso e distribuição compatíveis com sua licença; revisar atribuições e obrigações de código-fonte.
2. Implementar a ponte JNI e o ciclo de vida do núcleo, com renderização Vulkan/OpenGL ES, áudio, entrada e gerenciamento de memória.
3. Ligar SAF ao carregador real de imagens suportadas pelo núcleo, consultar identificação/região no núcleo e reportar compatibilidade sem inferências falsas.
4. Implementar memory cards e save states somente se suportados pelo núcleo escolhido.
5. Testar em dispositivos ARM64 reais, adicionar testes e publicar builds assinadas de forma segura.

BIOS e jogos devem ser fornecidos pelo usuário e utilizados somente quando obtidos legalmente. A seleção de um arquivo no app não valida sua origem nem significa que ele seja executável.
