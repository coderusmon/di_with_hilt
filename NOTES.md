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

### Вопрос 13. `@ApplicationContext` **[не воспроизводилось]**

```kotlin
@Provides @Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase
```

Без квалификатора Dagger не знает, какой именно `Context` подставить: в графе есть и `Application`, и `Activity`. `@ApplicationContext` — встроенный квалификатор Hilt, который говорит «это контекст приложения, живёт весь процесс». Обычный `Context` неоднозначен ещё и по времени жизни: если в singleton попадёт Activity, она утечёт.

### Вопрос 14. Два `OkHttpClient` и квалификаторы **[код проверен]**

В `Qualifiers.kt` объявлены `@ApiClient`, `@DownloadClient`, `@BaseUrl`, `@IoDispatcher`. `NetworkModule` предоставляет клиенты с read-timeout 10 с и 60 с; `FileDownloader` принимает `@DownloadClient`, а Retrofit — `@ApiClient`. Если убрать квалификатор у одного места инъекции, Dagger видит два одинаковых `OkHttpClient` без квалификатора и падает с `DuplicateBindings`/`missing binding` — тип перестаёт быть однозначным ключом. **[точный текст не копировался]**

Два преимущества собственных квалификаторов перед `@Named("api")`: компилятор проверяет тип аннотации и не допускает опечатку в строке; аннотация документирует назначение и её можно переиспользовать в тестах, не копируя строковые литералы.

### Вопрос 15. `@BaseUrl` и `@IoDispatcher` **[проверено]**

`DispatcherModule` даёт `@IoDispatcher CoroutineDispatcher = Dispatchers.IO`; `NetworkModule` принимает `@BaseUrl String`. `RoomNoteRepository` и `DefaultUserRepository` вызывают `withContext(io)`. В `RoomNoteRepositoryTest` репозиторий создан с `UnconfinedTestDispatcher()` и fake `NoteDao`; тест не использует ни одного Android-класса. Аналогично `UserRepositoryTest` подставляет тестовый диспетчер и `MockWebServer`.

---

## Section 4 — Components & Scopes

### Вопрос 16. Эксперимент со scope **[частично проверено]**

Классы: `AppSession` (`@Singleton`), `ActivitySession` (`@ActivityRetainedScoped`), `ScreenState` (без scope), `ViewModelTracker` (`@ViewModelScoped`), каждый печатает номер создания. Реальные наблюдения на эмуляторе:

| Событие | AppSession | ActivitySession | ScreenState | ViewModelTracker |
|---|---|---|---|---|
| Первый запуск | #1 | #1 | #1 | — |
| Открытие Notes | #1 | #1 | #1 | #1 |
| Поворот экрана | #1 | #1 | #2 | #1 |
| Возврат на Home | #1 | #1 | #3 | #1 |
| Смерть процесса и восстановление | новый #1 (новый процесс) | #1 | #1 | #1 |

`AppSession` живёт весь процесс; `ActivitySession` переживает поворот, потому что Activity retained, а не пересоздаётся с нуля; `ScreenState` без scope создаётся заново при каждой композиции/инъекции; `ViewModelTracker` живёт вместе с ViewModel и переживает поворот. Второй Activity и отдельный ViewModel-экран в этом прогоне не создавались, поэтому две колонки таблицы из задания отмечены как неполные.

### Вопрос 17. Иерархия компонентов

```
SingletonComponent (Application)
└── ActivityRetainedComponent (переживает configuration change)
    └── ActivityComponent (Activity)
        └── FragmentComponent (Fragment)
        └── ViewModelComponent (ViewModel, создаётся от Activity/Fragment)
```

* `@Singleton` не может зависеть от `@ActivityScoped`: родитель не видит детей, и singleton пережил бы Activity (утечка). **Нелегально.**
* `@ActivityScoped` может зависеть от `@Singleton`: ребёнок видит всё, что даёт родитель. **Легально.**
* `@ViewModelScoped` нельзя инжектить в Activity напрямую: компонент ViewModel — ребёнок, его scope не виден Activity. **Нелегально.**
* Поворот переживают `SingletonComponent` и `ActivityRetainedComponent` (и ViewModel); `ActivityComponent` и `FragmentComponent` пересоздаются. Процессную смерть не переживает ничего, кроме сохранённого состояния (`SavedStateHandle`, `SavedStateRegistry`).

### Вопрос 18. Ошибки иерархии **[не воспроизводилось]**

`@Singleton class X(@ActivityScoped y: Y)` даёт ошибку вида «Y is scoped to ActivityComponent but X is scoped to SingletonComponent»: Dagger отказывается строить граф. `@Singleton class X(@ActivityContext context: Context)` даёт ошибку о том, что `@ActivityContext` не может быть предоставлен в SingletonComponent. Обе ошибки предотвращают утечку Activity: singleton хранил бы ссылку на уничтоженную Activity, а значит её View-дерево и контекст.

### Вопрос 19. Модули `:app`, `:feature-notes`, `:data` **[проверено сборкой]**

`:data` содержит модели, Room, Retrofit, репозитории и все Hilt-модули (`NetworkModule`, `DatabaseModule`, `DispatcherModule`, `BindingsModule`, `RepositoryModule`); `:feature-notes` — экраны и `@HiltViewModel`; `:app` — `@HiltAndroidApp`, `MainActivity`, навигация. Зависимости: `:app → :feature-notes → :data`, `:app → :data`.

Если убрать `project(":data")` из `:app`, сборка падает на импортах `uz.ttpu.hiltlab.data.*` в `App`, `MainActivity`, `UsersViewModel`, `HomeScreen` (unresolved reference). Кроме того, `:app` перестаёт видеть Hilt-модули `:data` при агрегации, поэтому даже после ручного добавления отдельных классов граф останется без `NoteRepository`/`UserRepository`.

### Вопрос 20. Выбор scope

| Класс | Scope | Почему |
|---|---|---|
| `UserSession` | `@Singleton` | одна сессия на запуск приложения, общая для экранов |
| `NoteListState` | без scope (или `@ViewModelScoped` внутри VM) | состояние одного экрана; глобальный singleton хранил бы мусор |
| `HttpClient` | `@Singleton` | дорогой (пул соединений, потоки), потокобезопасен |
| `DateFormatter` | без scope | дешёвый и stateless; создание на вызов не проблема |
| `AuthToken` | `@Singleton` | меняется при логине, но должна быть общей; изменяемость требует volatile/синхронизации |

`@Singleton` был бы ошибкой для состояния экрана (утечка UI-данных между экранами), для объекта с Activity-контекстом и для дешёвого stateless-хелпера, где scope добавляет только церемонию. «Просто сделать singleton» — не решение, а компромисс между стоимостью создания, временем жизни и потокобезопасностью.

---

## Section 5 — Hilt in Android

### Вопрос 21. Миграция на Hilt **[проверено]**

В `HiltLab` нет `AppContainer`, фабрик ViewModel, `ApiInstance` и `DatabaseProvider`: всё приходит через `@Inject constructor` и модули. Размер Hilt-версии: `:app` — 8 файлов/323 строки, `:data` — 15/508, `:feature-notes` — 4/305 (итого 27 файлов, 1136 строк main-кода), плюс 25 JVM- и 4 instrumented-теста. Сравнить с веткой `manual-di` нельзя: она не создавалась (см. вопрос 4).

### Вопрос 22. `SavedStateHandle` и смерть процесса **[проверено]**

`NoteDetailViewModel`:

```kotlin
@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    private val formatter: DateFormatter,
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
}
```

Маршрут `note/{noteId}` объявлен с `NavType.LongType`, Hilt сам кладёт аргументы в `SavedStateHandle`. На эмуляторе открыл заметку, свернул приложение, выполнил `adb shell am kill uz.ttpu.hiltlab`, снова запустил: процесс новый (`AppSession #1`), а экран «Заметка» восстановился с тем же `noteId` и текстом. JVM-тест `NoteDetailViewModelTest` передаёт `SavedStateHandle(mapOf("noteId" to 1L))` и проверяет загрузку/сохранение.

### Вопрос 23. Реальный и fake репозиторий **[проверено]**

`NoteRepository` — интерфейс; `RoomNoteRepository` работает с Room, `FakeNoteRepository` хранит две заметки в `MutableStateFlow`. `RepositoryModule` выбирает реализацию по `BuildConfig.USE_FAKE_NOTES`, который задаётся Gradle-свойством `-PuseFakeNotes=true`. Провайдеры принимают `Provider<…>`, поэтому в fake-режиме Room вообще не создаётся (в логе нет `created AppDatabase`).

Скриншоты: `screenshots/notes_real.png` (пустой список из Room) и `screenshots/notes_fake.png` (две демо-заметки). Обе получены на эмуляторе.

### Вопрос 24. `BroadcastReceiver` **[проверено]**

`BootReceiver` аннотирован `@AndroidEntryPoint`, поле `@Inject lateinit var logger: Logger`. После установки/запуска в логе появилось `BootReceiver received: android.intent.action.BOOT_COMPLETED`, то есть инъекция сработала. В Kotlin вызов `super.onReceive(context, intent)` невозможен: `BroadcastReceiver.onReceive` абстрактный, компилятор выдаёт «Abstract member cannot be accessed directly». Hilt генерирует `Hilt_BootReceiver`, который сначала инжектит поля, затем вызывает `BootReceiver.onReceive`; поэтому инъекция происходит до пользовательского кода, а ручной `super` не нужен. Это расходится с формулировкой задания, но соответствует фактическому поведению Hilt/Kotlin.

### Вопрос 25. Общий `DraftViewModel` в nested graph **[не реализовано]**

В текущем срезе нет экранов `EditTitle`/`EditBody`, поэтому эксперимент с `hiltViewModel(navController.getBackStackEntry("edit"))` не проводился. Правильная схема: `navigation(startDestination = "edit/title", route = "edit")` с двумя `composable`; оба вызывают `hiltViewModel<DraftViewModel>(navController.getBackStackEntry("edit"))`, поэтому получают один ViewModel, привязанный к back stack entry графа. При выходе из графа entry уничтожается, ViewModel очищается, `onCleared()` логируется. Это остаётся ручной проверкой.

---

## Section 6 — Testing & Capstone

### Вопрос 26. Замена модулей и тестовая инфраструктура

`@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])` говорит Hilt: в тестовом графе не использовать `DatabaseModule`, а взять провайдеры из тестового модуля (in-memory Room). `@HiltAndroidTest` включает генерацию тестового компонента для класса; `HiltAndroidRule` создаёт/инжектит этот компонент и вызывает `inject()`; кастомный runner подменяет `Application` на `HiltTestApplication`, иначе Hilt не с чем связать тестовый граф. Юнит-тесту класса с `@Inject constructor` ничего этого не нужно: он создаёт объект обычным `new`/конструктором и передаёт fake-зависимости.

### Вопрос 27. Тесты без Hilt **[проверено]**

25 JVM-тестов в `:data` и `:feature-notes` не используют Hilt, Android и эмулятор: `ManualDiTest`, `FakeNoteRepositoryTest`, `RoomNoteRepositoryTest` (fake DAO + test dispatcher), `UserRepositoryTest` (MockWebServer), `NetworkQualifierTest`, `FileDownloader`-тесты, `NotesViewModelTest`, `NoteDetailViewModelTest`. Все классы тестируемы потому, что зависимости приходят через конструктор, а интерфейсы (`NoteRepository`, `UserRepository`, `NoteDao`) позволяют подставить fake. Время `./gradlew testDebugUnitTest` — около 6–13 секунд на прогретом Gradle.

### Вопрос 28. Instrumented-настройка **[проверено]**

В `:app`: `testInstrumentationRunner = "uz.ttpu.hiltlab.HiltTestRunner"`, `androidTestImplementation(libs.hilt.android.testing)`, `kspAndroidTest(libs.hilt.android.compiler)`. `HiltTestRunner` возвращает `HiltTestApplication`. Тесты `NoteRepositoryHiltTest` и `ScopeTest` используют `@HiltAndroidTest` и `HiltAndroidRule`. Все 4 instrumented-теста прошли на эмуляторе. Три намеренные поломки (без runner, без rule, без `inject()`) не воспроизводились; типичные симптомы — `HiltTestApplication` не установлен, `lateinit` не инициализирован, пустой тестовый компонент.

### Вопрос 29. In-memory база и Compose-тест **[проверено]**

`TestDatabaseModule` заменяет `DatabaseModule` и отдаёт `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`. `NotesScreenTest` через `createComposeRule()` рисует `NotesScreen` с заметкой и проверяет `onNodeWithText("Заметка A")` и клик. `NoteRepositoryHiltTest` пишет заметку в in-memory Room и читает её. Повторный запуск instrumented-тестов начинается с пустой базы, потому что in-memory база создаётся заново в каждом процессе.

### Вопрос 30. Capstone

Приложение содержит GitHub users (Retrofit + Room cache) и Notes (Room/fake, `SavedStateHandle`), Hilt-модули сети, базы, диспетчеров и репозиториев, квалификаторы `@ApiClient`, `@DownloadClient`, `@BaseUrl`, `@IoDispatcher`, `@HiltViewModel` с `SavedStateHandle`, замену `DatabaseModule` в instrumented-тестах и `MockWebServer` в JVM-тестах. 25 JVM-тестов + 4 instrumented; README содержит диаграмму компонентов и зависимостей. Ограничения: ветки `manual-di`/`hilt` и nested edit graph не создавались, поэтому соответствующие пункты отмечены выше как невыполненные.
