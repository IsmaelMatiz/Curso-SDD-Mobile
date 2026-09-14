# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

---

## Fork-local notes

`AGENTS.md` (imported above) owns the architecture, the layer dependency rules,
the spec-driven workflow, and the Gradle commands. This section records only
what those documents do not state and what is not obvious from a single file.

### Build environment

- AGP 9.3.2 · Kotlin 2.2.10 · compileSdk & targetSdk 37 · minSdk 26 · Java 11
  source/target compatibility. Android Studio must be new enough for AGP 9.x.
- Hilt runs through **KSP**, not kapt. New annotation processors go in the
  `ksp(...)` configuration.
- Every dependency and plugin is declared in `gradle/libs.versions.toml` and
  referenced as `libs.*`. Do not add a coordinate literal to
  `app/build.gradle.kts`; add the catalog entry first.
- Gradle's configuration cache is enabled in `gradle.properties`. Build logic
  that reads project state at execution time will fail the build rather than
  degrade silently.
- `local.properties` holds the machine-local SDK path and is not committed.
- The release build type disables optimization (`optimization { enable = false }`),
  so a release build is not evidence about minification or keep rules.

### Test infrastructure

`app/src/test` and `app/src/androidTest` contain only the generated
`ExampleUnitTest` / `ExampleInstrumentedTest` scaffolding. There is no test for
any production class yet.

Available test dependencies are JUnit 4, Espresso, and `compose-ui-test-junit4`.
There is **no** mocking library, no `kotlinx-coroutines-test`, and no Turbine.
Testing a ViewModel or a repository therefore requires adding dependencies —
treat that as part of the plan and say so, rather than adding it as a drive-by.

### How the reference Dog feature actually fits together

Details that require reading several files at once:

- **Navigation mutates the back stack directly.** `AppNavigation.kt` holds
  `rememberNavBackStack(Dog)`; screens receive lambdas that call
  `backstack.add(DogDetail(id))` and `backstack.removeLastOrNull()`. Screens
  never see a navigation controller.
- **The route key `Dog` and the domain model `Dog` share a name**
  (`core/navigation/Routes.kt` vs `domain/model/Dog.kt`). Any file touching both
  needs an import alias.
- **The two ViewModels load differently, on purpose.** `DogViewModel` fetches in
  `init`. `DogDetailViewModel` exposes `loadDog(id)` and the screen calls it from
  `LaunchedEffect(id)`, because the id arrives as a navigation argument rather
  than through a `SavedStateHandle`. Neither VM survives process death.
- **Search is client-side.** `DogViewModel` caches the full list in `allDogs` and
  `onQueryChange` filters that cache by name or breed. It is not a repository or
  query concern; a spec that moves filtering to the data layer is changing this.
- **File and type names do not line up.** `DogScreen.kt` declares `DogsScreen`;
  `DetailScreen.kt` declares `DogDetailScreen`; `DogViewModel` exposes
  `DogsUiState`. Search by symbol, not by filename.
- **DI builds the repository by hand.** `DataModule` is an `object` module with
  `@Provides` only, and `provideDogRepository` calls `DogRepositoryImpl(api)`
  even though that class has an `@Inject constructor`. There is no `@Binds`
  module in the project to copy from.

### Known deviations in the baseline

Present in the starting code, contradicting the rules in `AGENTS.md`. Do not
copy them into new code, and do not fix them as an unrelated change — fix one
only when a task's scope actually covers it.

- Both ViewModels `catch (e: Exception)` without rethrowing
  `CancellationException`, which the coroutine rule in `AGENTS.md` forbids.
- `jakarta.inject.Inject` and `javax.inject.Inject` are mixed across files.
  Use `javax.inject` in new code, matching Dagger's own convention and
  `DataModule`'s `javax.inject.Singleton`.
- UI strings are hardcoded Spanish literals in the composables; `strings.xml`
  contains only `app_name`, and `stringResource` is used nowhere. Moving to
  string resources is a real scope change, not cleanup.

### Documents

- `docs/PROMPTS.md` holds the course's opening prompt (Room persistence plus
  adding dogs) — it is the intended first feature and is not listed among the
  reference documents in `AGENTS.md`.
- `docs/features/` does not exist yet. The first feature creates it, following
  the `docs/features/<feature-name>/{SPEC,PLAN,TASKS}.md` layout.
- This is a fork of the upstream course repository. `AGENTS.md` and everything
  under `docs/` is shared course material; keep fork-local guidance in this
  file instead, to avoid conflicts when upstream changes.
