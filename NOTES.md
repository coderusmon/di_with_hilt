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

### Вопрос 4. Ветка `manual-di`

Отдельная ветка `manual-di` с переписыванием Lecture 21 Notes app не создавалась: в репозитории один проект `HiltLab`, и ручное переписывание другого приложения не входило в фактически выполненный объём. Поэтому числа «сколько файлов/строк» для этой миграции не приводятся — их нельзя честно измерить без самой миграции.

### Вопрос 5. Граф зависимостей и боли ручного DI

```
MainActivity ──> AppSession, ActivitySession, ScreenState, Logger
NotesRoute ──> NotesViewModel ──> NoteRepository ──> NoteDao ──> AppDatabase ──> Context
NoteDetailRoute ──> NoteDetailViewModel ──> NoteRepository, SavedStateHandle, DateFormatter
UsersRoute ──> UsersViewModel ──> UserRepository ──> GitHubApi ──> Retrofit ──> OkHttpClient
                                                  └──> UserDao ──> AppDatabase ──> Context
```

Три боли из слайда 7, которые видны на графе:

1. **Factory boilerplate.** Каждому ViewModel нужна фабрика с шестью параметрами; при добавлении `DateFormatter` правится каждая фабрика. Hilt убирает это полностью.
2. **Ручное управление жизнями.** Кто закрывает `AppDatabase`, когда создавать `OkHttpClient`, почему `Context` нельзя хранить дольше Activity. Hilt генерирует компоненты и проверяет это на этапе сборки, то есть **переносит** ответственность на граф, но не отменяет необходимость выбирать scope.
3. **Порядок инициализации.** `AppContainer` должен быть создан до первого экрана; при тестах его нужно подменять вручную. Hilt убирает ручной порядок, но **оставляет** требование правильно объявить `@InstallIn` и зависимости.

---

## Section 2 — Setting Up Hilt

### Вопрос 6. Gradle-настройка и две поломки **[настройка проверена, ошибки не воспроизводились]**

В корневом `build.gradle.kts` плагин подключён с `apply false`, в модулях — `alias(libs.plugins.hilt)`, `ksp(libs.hilt.android.compiler)` и `implementation(libs.hilt.android)`; версия задана в `gradle/libs.versions.toml` (`hilt = "2.57.1"`). Компилятор — `com.google.dagger:hilt-android-compiler:2.57.1`, как в слайдах.

Две типичные поломки:

* **Плагин есть, компилятора нет.** Hilt-плагин включает байткод-трансформацию, но без `hilt-android-compiler` некому генерировать `Hilt_App`/`Hilt_MainActivity`; сборка падает на ссылках на сгенерированные классы и на агрегации Hilt (сообщение вида «The Hilt Android Gradle plugin is applied but the … compiler dependency is not detected»).
* **Компилятор есть, плагина нет.** KSP генерирует код, но плагин не выполняет трансформацию `App extends Hilt_App`, поэтому приложение падает/не собирается с сообщением о том, что Hilt Gradle plugin не применён.

Точный текст ошибок в `NOTES.md` не скопирован: намеренно ломать рабочий проект в этом прогоне не стал, чтобы не оставить репозиторий в нерабочем состоянии (это ограничение указано в начале).

### Вопрос 7. `@HiltAndroidApp` и `Hilt_App` **[проверено]**

`App : Application` аннотирован `@HiltAndroidApp`, в манифесте указано `android:name=".App"`. Hilt сгенерировал `app/build/generated/hilt/component_sources/debug/uz/ttpu/hiltlab/Hilt_App.java` (54 строки): абстрактный класс наследует `Application`, реализует `GeneratedComponentManagerHolder`, лениво строит `DaggerApp_HiltComponents_SingletonC`, а в `onCreate()` вызывает `hiltInternalInject()` и только затем `super.onCreate()`. Плагин подменяет базовый класс `App` на `Hilt_App`.

Без строки в манифесте система создаёт обычный `Application`, и `@AndroidEntryPoint`-активность падает с `IllegalStateException`: Hilt Activity должен быть прикреплён к `@HiltAndroidApp`-приложению. Без аннотации Hilt не генерирует `Hilt_App`, поэтому сборка не находит сгенерированный базовый класс. **[не воспроизводилось]**

### Вопрос 8. Полевая инъекция и две ошибки **[код проверен, ошибки не воспроизводились]**

В `MainActivity` поля `@Inject lateinit var logger/appSession/activitySession/screenState` не `private`. Если сделать поле `private`, Hilt не может обратиться к нему из сгенерированного кода и сборка падает с сообщением «@Inject fields cannot be private». Если убрать `@AndroidEntryPoint`, Hilt не генерирует `Hilt_MainActivity` и не инжектит поля; обращение к `logger` даёт `UninitializedPropertyAccessException: lateinit property logger has not been initialized`.

Полевая инъекция вынуждена для Activity/Fragment/Receiver: система создаёт их сама, конструктор недоступен. Везде, где объект создаём мы (ViewModel, репозиторий, модуль), предпочтителен `@Inject constructor`: зависимости явные, класс тестируется без Hilt и не требует lateinit.

### Вопрос 9. Цепочка `A(B)`, `B(C)`, `C` **[не воспроизводилось]**

При `@Inject constructor` у всех трёх Dagger строит граф снизу вверх: сначала `C`, затем `B(c)`, затем `A(b)`, поэтому println в конструкторах даёт `C, B, A`. Два поля `@Inject lateinit var a: A` в одной Activity — это два независимых запроса; без scope каждый даёт новый объект, то есть ссылки разные. Если пометить `A` как `@Singleton` или `@ActivityRetainedScoped`, обе ссылки будут указывать на один экземпляр.

### Вопрос 10. Как читать ошибку Dagger

В ошибке Dagger важен путь `A → B → C`: первая строка называет тип, который не удалось предоставить, ниже — место запроса (конструктор/поле), а стрелки показывают всю цепочку до «листа». Если в цепочке есть интерфейс или класс без `@Inject`/`@Provides`, это **missing binding**: для интерфейса нужен `@Binds` (или `@Provides`), для чужого класса — `@Provides` в модуле. Если Dagger пишет `Found a dependency cycle`, два класса прямо или косвенно зависят друг от друга: разрывают цикл через `Provider`/`Lazy`, выделяют третью зависимость или меняют дизайн. Оба класса ошибок — ошибки сборки, а не runtime.

---

## Section 3 — Modules, Bindings & Qualifiers

### Вопрос 11. `@Binds` для `Logger` **[проверено сборкой]**

`interface Logger`; `LogcatLogger` и `FileLogger` имеют `@Inject constructor()`. В `BindingsModule`:

```kotlin
@Binds abstract fun bindLogger(impl: LogcatLogger): Logger
```

Замена `LogcatLogger` на `FileLogger` в этой строке — единственное изменение, все потребители (`MainActivity`, `BootReceiver`, ViewModel) продолжают получать `Logger`. В проекте используется `LogcatLogger`; `FileLogger` оставлен как вторая реализация для демонстрации.

### Вопрос 12. `@Provides` для сети и ленивость **[проверено]**

`NetworkModule` содержит `@Provides @Singleton` для `OkHttpClient` (API), второго `OkHttpClient` (download), `Retrofit`, `GitHubApi` и `@BaseUrl`. В каждом провайдере стоит `println("created …")`. Реальный лог при первом открытии GitHub users:

```
created ApiClient
created Retrofit
created GitHubApi
```

При повторном переходе на экран новых строк нет — singleton-объекты переиспользуются. `DownloadClient` не создаётся, пока никто не запросил `FileDownloader`; это подтверждает ленивую инициализацию Hilt.

