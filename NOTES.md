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

