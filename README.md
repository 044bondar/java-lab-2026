# Звіт до Лабораторної роботи №3
**Тема:** Розробка та застосування кастомних анотацій у Spring Boot застосунку  
**Проєкт:** VAG.CV Fleet & Garage Platform (`com.vagcv.garage`)  
**Гілка репозиторію:** `Lab3`

---

## 1. Мета роботи
Спроєктувати, реалізувати та протестувати три кастомні анотації різних архітектурних механізмів Spring Boot / Jakarta EE у предметній області обліку та перепродажу автомобілів:
1. **Bean Validation Constraint** (`@ProfitableResaleDeal`) — декларативна валідація бізнес-інваріантів на рівні класу DTO.
2. **Композитна мета-анотація** (`@FleetCreatedPostMapping`) — усунення дублювання веб-конфігурації контролера через композицію та `@AliasFor`.
3. **Кастомний MVC Argument Resolver** (`@CurrentOperator`) — автоматичне витягування контексту поточного оператора із запиту без дублювання коду в ендпоінтах.

---

## 2. Архітектура та реалізація кастомних анотацій

### 2.1. Тип 1: Bean Validation Constraint — `@ProfitableResaleDeal`
* **Файли:**
    * `src/main/java/com/vagcv/garage/annotation/ProfitableResaleDeal.java`
    * `src/main/java/com/vagcv/garage/annotation/ProfitableResaleDealValidator.java`
    * Застосовано до: `src/main/java/com/vagcv/garage/dto/VehicleRequestDto.java`
* **Механізм роботи:**  
  Анотація оголошена з `@Target(ElementType.TYPE)` та `@Constraint(validatedBy = ProfitableResaleDealValidator.class)`, що дозволяє валідатору аналізувати взаємозв'язок між кількома полями DTO одночасно (`purchasePrice`, `targetResalePrice`, `status`).
* **Бізнес-правила:**
    1. Очікуваний чистий спред (`targetResalePrice - purchasePrice`) повинен бути не меншим за конфігурований параметр `minProfit` (за замовчуванням `200.00`).
    2. Якщо автомобіль створюється/оновлюється у статусі `SOLD`, ціна продажу не може бути нижчою за ціну закупівлі (заборона фіксації збиткового продажу).
    3. У разі порушення стандартне повідомлення вимикається (`disableDefaultConstraintViolation()`) і формується деталізований `ConstraintViolation` із прив'язкою до поля `targetResalePrice` та розрахунком мінімально необхідної ціни продажу.

---

### 2.2. Тип 2: Композитна анотація (Composed Meta-Annotation) — `@FleetCreatedPostMapping`
* **Файли:**
    * `src/main/java/com/vagcv/garage/annotation/FleetCreatedPostMapping.java`
    * Застосовано у: `src/main/java/com/vagcv/garage/controller/VehicleRestController.java` (`createVehicle`)
* **Механізм роботи:**  
  Об'єднує дві анотації, які регулярно застосовуються разом для POST-ендпоінтів створення сутностей:
    * `@RequestMapping(method = RequestMethod.POST)`
    * `@ResponseStatus(HttpStatus.CREATED)`
      Для прозорого налаштування маршрутів використано `@AliasFor(annotation = RequestMapping.class, attribute = "value")` та `@AliasFor(..., attribute = "path")`, що забезпечує ідентичну поведінку з окремим застосуванням оригінальних анотацій.

---

### 2.3. Тип 3: Кастомний резолвер аргументів Spring MVC — `@CurrentOperator`
* **Файли:**
    * `src/main/java/com/vagcv/garage/annotation/CurrentOperator.java`
    * `src/main/java/com/vagcv/garage/annotation/CurrentOperatorArgumentResolver.java`
    * Реєстрація у MVC: `src/main/java/com/vagcv/garage/config/WebMvcConfig.java`
    * Застосовано у: `VehicleRestController.createVehicle(..., @CurrentOperator String operator)`
* **Механізм роботи:**  
  `CurrentOperatorArgumentResolver` реалізує інтерфейс `HandlerMethodArgumentResolver` і підключається через `WebMvcConfigurer.addArgumentResolvers(...)`:
    1. `supportsParameter(...)` повертає `true` виключно для параметрів типу `String`, позначених анотацією `@CurrentOperator`.
    2. `resolveArgument(...)` послідовно перевіряє:
        * автентифікований `Authentication` у `SecurityContextHolder` (витягує `auth.getName()`);
        * HTTP-заголовок `X-Operator` (якщо запит виконано без JWT-контексту);
        * значення `fallback` з атрибута анотації (за замовчуванням `"api_user"`).
    3. Отримане ім'я оператора передається безпосередньо в бізнес-сервіс `vehicleService.createVehicle(requestDto, operator)` і фіксується в полі `createdBy` збереженої сутності `Vehicle`.

---

## 3. Верифікація Acceptance Criteria (Матриця покриття тестами)

Усі 8 критеріїв приймання перевірено автоматизованими тестами у класі `src/test/java/com/vagcv/garage/unit/Lab3CustomAnnotationsTest.java`:

| Критерій | Механізм | Тестовий метод у `Lab3CustomAnnotationsTest` | Очікуваний та фактичний результат |
| :--- | :--- | :--- | :--- |
| **AC 1** | Bean Validation (`@Constraint`) | `profitableResaleDeal_ValidMargin_ShouldPassValidation` | Jakarta `Validator` автоматично знаходить `ProfitableResaleDealValidator` без ручного створення екземпляра |
| **AC 2** | Bean Validation (позитивний кейс) | `profitableResaleDeal_ValidMargin_ShouldPassValidation` | При маржі `700.00 >= 200.00` список `ConstraintViolation` порожній (`isEmpty() == true`) |
| **AC 3** | Bean Validation (негативний кейс) | `profitableResaleDeal_InsufficientMargin_ShouldFailWithClearMessage` / `createVehicleEndpoint_InvalidDeal_ShouldReturnBadRequest` | Виникає порушення на полі `targetResalePrice` із точним поясненням (`80.00 < 200.00`, поріг `6700.00`), HTTP-запит повертає `400 Bad Request` |
| **AC 4** | Meta-Annotation (`@AliasFor`) | `createVehicleEndpoint_ShouldApplyMetaAnnotationAndInjectOperator` | Композитна анотація `@FleetCreatedPostMapping` коректно маршрутизує `POST /api/vehicles` |
| **AC 5** | Meta-Annotation (`MockMvc`) | `createVehicleEndpoint_ShouldApplyMetaAnnotationAndInjectOperator` | Повертається статус `201 Created`, ідентичний до окремого використання `@PostMapping` + `@ResponseStatus(HttpStatus.CREATED)` |
| **AC 6** | `supportsParameter(...)` | `currentOperatorResolver_SupportsParameter_ShouldCheckAnnotationAndType` | Повертає `true` для `@CurrentOperator String operator` і `false` для `@RequestBody VehicleRequestDto` |
| **AC 7** | `resolveArgument(...)` | `currentOperatorResolver_ResolveArgument_ShouldExtractAuthenticatedUser` | Коректно витягує `"bohdan_admin"` із `SecurityContextHolder` |
| **AC 8** | Інтеграція в контролері | `createVehicleEndpoint_ShouldApplyMetaAnnotationAndInjectOperator` | Резолвлене значення `"taras_mechanic"` передається у `vehicleService.createVehicle(...)` і повертається у JSON (`$.createdBy`) |

---

## 4. Інструкція із запуску та перевірки

```bash
# Переключення на гілку лабораторної роботи
git checkout Lab3

# Запуск усіх модульних та інтеграційних тестів (включно з Lab3CustomAnnotationsTest)
./mvnw test