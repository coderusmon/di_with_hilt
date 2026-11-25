# Practice 15 — Dependency Injection with Hilt

**Проект:** `HiltLab` (многомодульный: `:app`, `:feature-notes`, `:data`)
**Окружение:** Windows, JDK 17, Gradle 8.10.2, AGP 8.8.0, Kotlin 2.1.20, KSP 2.1.20-2.0.1, Hilt 2.57.1, Room 2.7.1, Retrofit 2.11.0, Compose BOM 2024.09.03, эмулятор `Small_Phone_API_36-ext19`.

**Что реально проверено**

| Проверка | Команда | Результат |
|---|---|---|
| Debug-сборка | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| JVM-тесты | `./gradlew testDebugUnitTest` | 25 тестов, 0 падений |
| Instrumented-тесты | `./gradlew :app:connectedDebugAndroidTest` | 4 теста на эмуляторе, 0 падений |
| Режим fake | `./gradlew :app:installDebug -PuseFakeNotes=true` | приложение работает без Room |
| Процессная смерть | `adb shell am kill`, затем запуск | экран заметки восстановлен |

Обозначения: **[проверено]** — наблюдение получено на эмуляторе/сборкой; **[не воспроизводилось]** — ответ дан по слайдам и коду, но соответствующий эксперимент (обычно намеренно сломанная сборка) в этом окружении не прогонялся.

---

## Section 1 — DI Fundamentals & Manual DI

### Вопрос 1. `HardCar` против `Car(engine)` **[проверено]**

`HardCar` сам вызывает `PetrolEngine()` внутри, поэтому в тесте невозможно подменить сломанный двигатель: можно проверить только поведение настоящего `PetrolEngine`, а сценарий «двигатель сломан» требует менять код класса. `Car(engine: Engine)` принимает интерфейс, поэтому тест передаёт `FakeEngine`, который возвращает заранее заданный результат или бросает исключение, — поведение автомобиля проверяется без реального двигателя.

В `data/src/test/.../ManualDiTest.kt` три теста: `hardCarBuildsItsOwnEngine`, `softCarAcceptsAnyEngine` (анонимный `FakeEngine`), `serviceLocatorHidesTheDependency`.

### Вопрос 2. Construct / grab / supplied

| Фрагмент | Способ | Обоснование |
|---|---|---|
| (a) `Room.databaseBuilder(...).build()` внутри класса | **construct** | класс сам создаёт базу, зависимость зашита в него |
| (b) `ApiInstance.service` внутри класса | **grab** | класс тянет готовый объект из глобального локатора |
| (c) `class Repo(private val api: Api)` | **supplied** | зависимость приходит извне через конструктор |
| (d) `LocalContext.current` в composable | **grab** | значение берётся из `CompositionLocal`, а не передаётся параметром |
| (e) `by viewModels()` с кастомной фабрикой | **supplied** | фабрика создаёт ViewModel и передаёт зависимости |
| (f) `@Inject lateinit var logger: Logger` | **supplied** | Hilt подставляет поле после создания Android-объекта |

### Вопрос 3. Ручной `AppContainer` и счётчики **[частично проверено]**

Ручной контейнер в этом проекте не использовался: приложение сразу собрано на Hilt. Его эквивалент выглядел бы так:

```kotlin
class AppContainer(context: Context) {
    val client = OkHttpClient.Builder().build().also { println("created OkHttp") }
    val retrofit = Retrofit.Builder().baseUrl(BASE).client(client).build()
        .also { println("created Retrofit") }
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()
        .also { println("created DB") }
    val repository = RoomNoteRepository(db.noteDao(), Dispatchers.IO)
        .also { println("created Repository") }
}
```

Ожидание: `OkHttpClient`, Retrofit, база и репозиторий создаются по одному разу на процесс; экраны получают их из контейнера, а не создают заново. В Hilt-версии это подтверждено логом: `AppSession #1` один раз, `AppDatabase` один раз (при первом открытии Notes), `ApiClient`/`Retrofit`/`GitHubApi` по одному разу (при первом открытии GitHub users). `DownloadClient` не создавался, потому что `FileDownloader` в UI не вызывается: Hilt ленив и не строит неиспользуемые singleton-зависимости.

