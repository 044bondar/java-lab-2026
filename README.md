# Lab 1: Spring Security (OIDC + JWT), CRUD, Liquibase та UI

## 1. Мета і що зроблено
Закладено технічний фундамент семестрового проєкту для предметної області **«Платформа обліку, відновлення та перепродажу автопарку (VAG.CV Fleet & Garage)»**:
* Реалізовано базовий **CRUD REST API** (`Controller → Service → Repository → Entity → DTO`) із автоматичним мапінгом сутностей через **MapStruct** (`VehicleMapper`).
* Налаштовано версіонування та автоматичне створення схеми бази даних через **Liquibase** (`db.changelog-master.yaml`), без ручних SQL-скриптів та без `ddl-auto=create`.
* Додано веб-інтерфейс на **Thymeleaf** (`/ui/vehicles`) для перегляду, додавання та видалення автомобілів.
* Реалізовано **подвійну модель автентифікації** у Spring Security (`SecurityConfig`):
  * **OIDC (`oauth2Login`)** через **Keycloak** із браузерною сесією для UI-користувачів (`/ui/**`).
  * **Stateless JWT (`oauth2ResourceServer`)** через заголовок `Authorization: Bearer <token>` для прямих REST API-запитів (`/api/**`).
* Продемонстровано **два незалежні рівні авторизації (RBAC)**:
  1. На рівні маршрутів у `SecurityFilterChain` (`.requestMatchers("/api/admin/**").hasRole("ADMIN")`).
  2. На рівні методів бізнес-логіки через `@PreAuthorize("hasRole('ADMIN')")` на методі `VehicleService.deleteVehicle(Long id)`.
* Підготовлено готову колекцію запитів **Postman** (`postman/VagCv.lab1.postman_collection.json`) та інтеграційні тести (`Lab1SecurityAndCrudTest`).

---

## 2. Як працює безпека та архітектура в проєкті

### Подвійний `SecurityFilterChain`
У класі `SecurityConfig` зареєстровано два ізольовані ланцюги фільтрів безпеки із різним пріоритетом (`@Order`):
1. **`@Order(1) apiSecurityFilterChain` (для `/api/**`):**
   * Працює у режимі `SessionCreationPolicy.STATELESS` (жодних cookie чи HTTP-сесій).
   * Вимикає CSRF та перевіряє підпис JWT-токенів із заголовка `Authorization: Bearer <token>`.
   * Якщо токен відсутній або невалідний — повертає `401 Unauthorized` (через `HttpStatusEntryPoint`).
   * Якщо ролі в токені недостатньо — повертає `403 Forbidden`.
2. **`@Order(2) uiSecurityFilterChain` (для всіх інших шляхів, зокрема `/ui/**`):**
   * Перехоплює неавтентифіковані браузерні запити та робить редирект на сторінку входу IdP **Keycloak** (`http://localhost:8180/realms/garage-realm`).
   * Після логіну `oidcAuthoritiesMapper()` витягує ролі з `realm_access.roles` ID-токена та перетворює їх у Spring Security `ROLE_ADMIN` / `ROLE_MECHANIC`.

### Два способи обмеження доступу
| Рівень захисту | Де налаштовано | Як працює | Приклад у проєкті |
| :--- | :--- | :--- | :--- |
| **SecurityFilterChain** | `SecurityConfig.apiSecurityFilterChain` | Перевіряє роль ще на рівні URL-фільтра до виклику контролера | `GET /api/admin/stats` вимагає `ROLE_ADMIN` (`MECHANIC` отримує `403 Forbidden`) |
| **Method Security (`@PreAuthorize`)** | `VehicleService.deleteVehicle(Long id)` | Перевіряє роль через AOP-проксі перед виконанням методу сервісу | URL `/api/vehicles/{id}` дозволено для `MECHANIC`, але виклик `DELETE` кидає `AccessDeniedException` (`403 Forbidden`) |

---

## 3. Усі файли проєкту

| Файл / Директорія | Призначення |
| :--- | :--- |
| `pom.xml` | Залежності проєкту: Spring Boot 3.3.5, Web, Thymeleaf, Security (OAuth2 Client + Resource Server), Data JPA, Liquibase, MapStruct, Lombok, H2, PostgreSQL |
| `docker-compose.yml` | Контейнери **PostgreSQL 16** (`5432`) та **Keycloak 24** (`8180`) з автоімпортом realm |
| `keycloak/realm-export.json` | Готова конфігурація Keycloak (`garage-realm`, клієнт `garage-app`, ролі `ADMIN` і `MECHANIC`, користувачі `admin_user` та `mechanic_user`) |
| `src/main/resources/db/changelog/*` | Міграції Liquibase: створення таблиці `vehicles` (`001`) та початкове наповнення даними (`002`) |
| `domain/Vehicle.java`, `VehicleStatus.java` | JPA-сутність автомобіля та enum статусів (`INSPECTION`, `IN_RESTORATION`, `READY_FOR_SALE`, `SOLD`) |
| `repository/VehicleRepository.java` | Spring Data JPA репозиторій для роботи з таблицею `vehicles` |
| `dto/VehicleRequestDto.java`, `VehicleResponseDto.java` | DTO для вхідних запитів (із Bean Validation) та відповідей API (з розрахованим прибутком `expectedProfit`) |
| `mapper/VehicleMapper.java` | Інтерфейс MapStruct для мапінгу `Entity ↔ DTO` |
| `service/VehicleService.java` | Бізнес-логіка CRUD + захист видалення через `@PreAuthorize("hasRole('ADMIN')")` |
| `security/SecurityConfig.java` | Конфігурація двох `SecurityFilterChain` (JWT + OIDC), мапінг ролей, `@EnableMethodSecurity` |
| `security/DevJwtTokenService.java` | Генератор підписаних HS256 JWT-токенів для швидкого тестування API |
| `controller/VehicleRestController.java` | REST API ендпоінти `/api/vehicles` |
| `controller/AdminRestController.java` | Адміністративний ендпоінт `/api/admin/stats` |
| `controller/VehicleUiController.java` | MVC-контролер для Thymeleaf-інтерфейсу `/ui/vehicles` |
| `postman/VagCv.lab1.postman_collection.json` | Готова колекція запитів Postman з автозбереженням токенів та тестами |
| `labs/lab1/` | Папка зі скріншотами демонстрації роботи (UI, OIDC, JWT, 403 Forbidden, тести) |

---

## 4. Тести (`Lab1SecurityAndCrudTest`)

У класі `src/test/java/com/vagcv/garage/Lab1SecurityAndCrudTest.java` автоматично перевіряються всі ключові сценарії:
1. `unauthenticatedUiRequest_redirectsToOidcLogin` — неавтентифікований запит до `/ui/vehicles` повертає `302 Redirect` на Keycloak (AC4).
2. `apiVehicles_requiresValidJwtAndReturnsLiquibaseSeedData` — запит до `/api/vehicles` без токена повертає `401 Unauthorized`, а з валідним JWT повертає `200 OK` і початкові записи з Liquibase (AC2, AC5).
3. `adminEndpoint_enforcesSecurityFilterChainRoleRule` — `MECHANIC` отримує `403 Forbidden` на `/api/admin/stats`, а `ADMIN` отримує `200 OK` (AC6).
4. `createAndDeleteVehicle_enforcesPreAuthorizeOnService` — створення авто повертає `201 Created` з порахованим `expectedProfit`, після чого `MECHANIC` при спробі `DELETE` отримує `403 Forbidden` (через `@PreAuthorize`), а `ADMIN` успішно видаляє запис зі статусом `204 No Content` (AC1, AC7).

---

## 5. Відповідність критеріям прийнятності (Acceptance Criteria)

| Критерій | Статус | Як підтверджено |
| :--- | :---: | :--- |
| **AC1:** Базовий CRUD + MapStruct | Виконано | `VehicleRestController`, `VehicleService`, `VehicleMapper` (`toEntity`, `toDto`, `updateEntityFromDto`) |
| **AC2:** Схема БД керується міграціями | Виконано | `Liquibase` (`db.changelog-master.yaml`) + `hibernate.ddl-auto=validate` |
| **AC3:** Базовий UI існує | Виконано | Thymeleaf сторінки `index.html` та `vehicles.html` (перегляд, створення, видалення авто) |
| **AC4:** OIDC-автентифікація для UI | Виконано | `oauth2Login()` + редирект на Keycloak `garage-realm` |
| **AC5:** JWT-автентифікація для API | Виконано | `oauth2ResourceServer().jwt()` у stateless режимі (`Authorization: Bearer ...`) |
| **AC6:** Обмеження через `SecurityFilterChain` | Виконано | `.requestMatchers("/api/admin/**").hasRole("ADMIN")` → `403 Forbidden` для `MECHANIC` |
| **AC7:** Обмеження через `@PreAuthorize` | Виконано | `@PreAuthorize("hasRole('ADMIN')")` на `VehicleService.deleteVehicle()` → `403 Forbidden` |
| **AC8:** Колекція запитів для захисту | Виконано | `postman/VagCv.lab1.postman_collection.json` та `postman/lab1-intellij-requests.http` |

---

## 6. Запуск, перевірка та скріншоти для здачі

### Крок 1. Підняття інфраструктури (Docker Desktop на Windows)
У терміналі IntelliJ IDEA виконайте:
```powershell
docker-compose up -d
```
* **Keycloak** запуститься на `http://localhost:8180` (логін адміна консолі: `admin` / `admin`).
* Готові тестові користувачі в realm `garage-realm`:
  * **Адмін:** `admin_user` / пароль `admin123` (ролі `ADMIN`, `MECHANIC`)
  * **Механік:** `mechanic_user` / пароль `mech123` (роль `MECHANIC`)

### Крок 2. Запуск застосунку
Запустіть `FleetGarageApplication` в IntelliJ IDEA (за замовчуванням працює зі вбудованою БД H2, або з профілем `-Dspring-boot.run.profiles=postgres` для підключення до PostgreSQL у Docker).

### Крок 3. Перевірка в Postman
Імпортуйте файл `postman/VagCv.lab1.postman_collection.json` у Postman та виконайте запити `01` → `10` по порядку:
* Запити `01` і `02` автоматично збережуть JWT-токени у змінні колекції `{{admin_token}}` та `{{mechanic_token}}`.
* Запит `03` поверне `401 Unauthorized`, `04`–`06` виконають CRUD (`200`/`201`), `07` і `09` продемонструють `403 Forbidden` для `MECHANIC`, а `08` і `10` — успішний доступ для `ADMIN`.

### Крок 4. Прогін тестів
```powershell
.\mvnw.cmd test
```
Очікуваний результат: `Tests run: 4, Failures: 0, Errors: 0`.

### Крок 5. Скріншоти для звіту (папка `labs/lab1/`)
![Скріншот 1: OIDC логін та UI](labs/lab1/01-docker-and-oidc-ui.png)
![Скріншот 2: JWT автентифікація API](labs/lab1/02-jwt-api-auth.png)
![Скріншот 3: Перевірка ролей 403 Forbidden](labs/lab1/03-rbac-403-forbidden.png)
![Скріншот 4: Успішний прогін тестів](labs/lab1/04-mvn-test-green.png)
