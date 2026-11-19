# Лабораторна робота №2 — Unit-тестування (JUnit 5, Mockito, JaCoCo)

**Проєкт:** VAG.CV Fleet & Garage Platform  
**Стек:** Java 21, JUnit 5 (Jupiter), Mockito, AssertJ, JaCoCo  
**Гілка репозиторію:** `Lab2`

---

## 1. Мета роботи
Реалізувати набір ізольованих модульних (unit) тестів для бізнес-логіки, маперів і сервісів безпеки без підняття контексту Spring Boot, застосувати мок-об'єкти (`@Mock`, `@InjectMocks`, `ArgumentCaptor`), параметризовані тести (`@ParameterizedTest`), а також сформувати звіт покриття коду тестами за допомогою плагіна **JaCoCo**.

---

## 2. Реалізована бізнес-логіка та тестові набори

У сервісі `VehicleService` додано методи фінансової оцінки проєктів перепродажу авто **VAG.CV**:
* `calculateProjectedProfit(Long vehicleId, BigDecimal repairCosts)` — розрахунок чистого прибутку з урахуванням ціни закупки, цільової ціни продажу та витрат на ремонт.
* `calculateProfitMarginPercent(Long vehicleId, BigDecimal repairCosts)` — обчислення відсоткової маржинальності угоди.
* `isProfitableDeal(Long vehicleId, BigDecimal repairCosts, BigDecimal minDesiredProfit)` — перевірка рентабельності угоди.
* `markReadyForSale(Long vehicleId)` — переведення авто у статус `READY_FOR_SALE` з валідацією поточного стану та ціни.

### Тестові класи (`src/test/java/com/vagcv/garage/unit/`):

| Тестовий клас | Об'єкт тестування | Перевірені сценарії |
| :--- | :--- | :--- |
| `VehicleServiceUnitTest` | `VehicleService` | Отримання списку та авто за ID, винятки `ResourceNotFoundException` і `IllegalArgumentException`, перевірка унікальності VIN, верифікація збереження через `ArgumentCaptor`, параметризований тест `@CsvSource` для розрахунку прибутку, обчислення відсоткової маржі, зміна статусу та блокування вже проданих авто (`IllegalStateException`). |
| `VehicleMapperUnitTest` | `VehicleMapper` | Перетворення `VehicleRequest -> Vehicle` та `Vehicle -> VehicleResponse` з перевіркою всіх полів. |
| `DevJwtTokenServiceUnitTest` | `DevJwtTokenService` | Генерація та декодування підписаного JWT-токена, перевірка `subject`, списку ролей і часу дії. |

---

## 3. Скріншоти виконання

### 3.1. Успішне виконання Unit-тестів у IntelliJ IDEA
![Unit Tests Passed](01-unit-tests-passed.png)

### 3.2. Звіт покриття коду тестами (JaCoCo)
![JaCoCo Coverage Report](02-jacoco-coverage-report.png)

---

## 4. Висновки
У межах лабораторної роботи №2 створено ізольовані unit-тести на базі JUnit 5 та Mockito без залежності від Docker, PostgreSQL чи Spring ApplicationContext. Налаштовано плагін JaCoCo для автоматичного підрахунку покриття коду інструкціями та гілками під час виконання фази `mvn test`.