## IPFS / libp2p Android demo (Peergos Nabu + Jetpack Compose)

Коротко: Android‑приложение на Kotlin, которое через библиотеку **Peergos Nabu**:

- поддерживает соединение с IPFS/libp2p‑нодой,
- регулярно пингует ноду и отображает текущую latency,
- по кнопке делает запрос данных по заранее сконфигурированному CID и показывает результат или ошибку.

CID и адрес ноды задаются в конфигурации (`IpfsConfig`) и **не прошиты в UI**.

---

### Стек

- **Kotlin**
- **Jetpack Compose** + Material 3
- **MVVM** + упрощённый Clean Architecture (`presentation` / `domain` / `data` / `di`)
- **Hilt** для DI
- **Coroutines** (`StateFlow`, `viewModelScope`, `Dispatchers.IO`)
- **Peergos Nabu** (IPFS/libp2p) через `jitpack.io`
- Gradle Version Catalog + KSP

---

### Структура и слои

`app/src/main/java/com/example/test`:

- `App`
  - `@HiltAndroidApp` `Application`,
  - инжектит `IpfsHolder` и вызывает `warmUp()` в `onCreate()`, чтобы инициализация Nabu шла в фоне и не блокировала UI.

- `di/IpfsConfig`
  - центральная конфигурация IPFS‑нод:
    - multiaddress целевой ноды,
    - тестовый CID, который приложение читает по кнопке,
    - таймауты для операций (fetch / ping).

- `di/IpfsHolder`
  - `@Singleton`‑обёртка над `EmbeddedIpfs`:
    - лениво создаёт и запускает локальный IPFS‑нод Nabu в `Dispatchers.IO`,
    - поднимает отдельный каталог под блок‑хранилище во внутренней памяти приложения,
    - гарантирует, что `EmbeddedIpfs` создаётся один раз и шарится по всему приложению,
    - предоставляет `suspend fun get(): EmbeddedIpfs`.

- `di/NetworkModule`
  - настраивает зависимости:
    - `IpfsClient` = `NabuIpfsClient(IpfsHolder, таймауты, тестовый CID)`,
    - `IpfsRepository` = `IpfsRepositoryImpl(IpfsClient)`,
    - `FetchCidUseCase`, `PingNodeUseCase`.

- `data/ipfs`
  - `IpfsClient` — интерфейс IPFS‑клиента:
    - `suspend fun fetchCid(): String` — получить данные по тестовому CID,
    - `suspend fun ping(): Long` — измерить задержку.
  - `NabuIpfsClient` — реализация на Peergos Nabu:
    - через `IpfsHolder` получает `EmbeddedIpfs`,
    - `fetchCid`:
      - декодирует тестовый CID через `Cid.decode(...)`,
      - запрашивает блок через `getBlocks(...)`,
      - возвращает его содержимое как строку (UTF‑8),
    - `ping`:
      - делает лёгкий запрос `getBlocks` к тому же CID и замеряет время выполнения, возвращая latency в мс.

- `data/repository/IpfsRepositoryImpl`
  - инкапсулирует `IpfsClient`,
  - пробрасывает `IllegalArgumentException` (ошибка формата CID),
  - остальные ошибки оборачивает в `RuntimeException("Failed to ...", e)` — удобнее отображать в UI.

- `domain/repository/IpfsRepository`
  - доменный контракт:
    - `suspend fun fetchCid(): String`,
    - `suspend fun ping(): Long`.

- `domain/usecase`
  - `FetchCidUseCase` — тонкая обёртка над `IpfsRepository.fetchCid()`,
  - `PingNodeUseCase` — тонкая обёртка над `IpfsRepository.ping()`.

- `presentation/state/UiState`
  - единое состояние экрана:
    - `latency: Long?`,
    - `cidResult: String`,
    - `loading: Boolean`,
    - `cidError: String?`,
    - `pingError: String?`.

- `presentation/viewmodel/MainViewModel`
  - `@HiltViewModel`,
  - хранит `StateFlow<UiState>`,
  - в `init {}` стартует ping‑цикл во `viewModelScope`:
    - раз в 2 секунды вызывает `PingNodeUseCase`,
    - обновляет `latency` и очищает `pingError` при успехе,
    - при ошибке пишет текст ошибки в `pingError`,
  - `onFetchCid()`:
    - включает `loading`, очищает `cidError`,
    - вызывает `FetchCidUseCase`,
    - при успехе кладёт данные в `cidResult`, выключает `loading`,
    - при `IllegalArgumentException` пишет понятное сообщение о некорректном CID,
    - при других исключениях — сообщение вида “Unexpected error while fetching CID”.

- `presentation/screen/MainScreen`
  - Jetpack Compose экран:
    - показывает заголовок `IPFS / libp2p monitor`,
    - строку `Latency: N ms` или `Latency: —`,
    - при ошибках пинга — компактный красный текст под latency,
    - кнопку **Fetch CID**:
      - выключена во время загрузки,
      - по нажатию вызывает `onFetchCid()` из `ViewModel`,
      - при загрузке показывает `CircularProgressIndicator`,
    - блок `Result`:
      - при `cidError` — текст ошибки красным цветом,
      - иначе — содержимое `cidResult` (или `<no data>`).

---

### Поведение приложения

1. Приложение поднимает локальный IPFS‑нод Nabu в фоне.
2. `MainViewModel` каждые 2 секунды пингует целевую ноду и обновляет latency в UI.
3. Пользователь может нажать **Fetch CID**, чтобы запросить данные по тестовому CID из конфигурации:
   - при успехе увидит содержимое блока,
   - при ошибке — понятное сообщение вместо падения приложения.

---

### Сборка и запуск

1. Открыть проект в **Android Studio**.
2. Убедиться, что установлен **JDK 17+**.
3. Выполнить Gradle Sync.
4. Собрать и запустить модуль `app` на устройстве / эмуляторе с API уровнем не ниже `minSdk` из `app/build.gradle.kts`.

---

### Что приложить к выполненному заданию

- **Скриншот** экрана, на котором:
  - видна текущая latency,
  - виден результат успешного или неуспешного запроса данных по CID.