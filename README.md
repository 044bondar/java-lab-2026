# Звіт до Лабораторної роботи №4
**Тема:** Aspect-Oriented Programming (AOP) у Spring Boot  
**Проєкт:** VAG.CV Fleet & Garage Platform (`com.vagcv.garage`)  
**Гілка репозиторію:** `Lab4`

---

## 1. Мета роботи
Реалізувати наскрізну функціональність (cross-cutting concerns) у вигляді 3 пар «власна анотація + Spring AOP аспект» різних типів Advice (`@Around`, `@AfterReturning`, `@Before`), продемонструвати різницю між зовнішнім викликом через Spring Proxy та внутрішнім самовикликом (`self-invocation`), а також покрити аспекти модульними та інтеграційними тестами.

---

## 2. Реалізовані анотації та аспекти (`com.vagcv.garage.aop`)

### 2.1. `@TrackExecutionSla` + `ExecutionSlaAspect` (`@Around`)
* **Призначення:** моніторинг часу виконання бізнес-операцій із контролем порогу SLA (`maxMillis`).
* **Де застосовано:** `VehicleService.createVehicle(...)` (`maxMillis = 250L`).
* **Логіка роботи:** вимірює тривалість виконання `joinPoint.proceed()` через `System.nanoTime()`, записує метрику `SlaMetric` у потокобезпечний реєстр `DealAuditRegistry` навіть у разі викидання винятку (блок `finally`) та логує попередження при перевищенні ліміту SLA.

### 2.2. `@AuditDealOperation` + `DealAuditAspect` (`@AfterReturning`)
* **Призначення:** аудит ключових змін статусу автомобіля на майданчику.
* **Де застосовано:** `VehicleService.markReadyForSale(Long vehicleId)` (`action = "MARK_READY_FOR_SALE"`).
* **Логіка роботи:** після успішного повернення `VehicleResponseDto` витягує поточного користувача із `SecurityContextHolder` та зберігає запис `AuditEntry` (дія, ID авто, VIN, оператор, мітка часу) у `DealAuditRegistry`.

### 2.3. `@GuardHighValueResale` + `HighValueResaleGuardAspect` (`@Before`)
* **Призначення:** контроль доступу перед виставленням преміальних автомобілів дорожче заданого порогу (`threshold = 10000.00`).
* **Де застосовано:** `VehicleService.createVehicle(...)`.
* **Логіка роботи:** до виконання методу перевіряє аргумент `VehicleRequestDto.targetResalePrice`. Якщо сума перевищує поріг, а поточний користувач не має ролі `ROLE_ADMIN`, аспект блокує виконання винятком `AccessDeniedException`.

---

## 3. Демонстрація проблеми самовиклику (`self-invocation`)
У класі `VehicleService` реалізовано два шляхи виклику анотованого методу `markReadyForSale(Long vehicleId)`:
1. **Зовнішній виклик** (`vehicleService.markReadyForSale(id)` із контролера або тесту): звернення проходить через обгортку **Spring AOP Proxy**, тому `@AuditDealOperation` спрацьовує та створює запис у `DealAuditRegistry`.
2. **Внутрішній самовиклик** (`vehicleService.prepareAndPublishInternal(id)` $\rightarrow$ `this.markReadyForSale(id)`): виклик виконується напряму через посилання `this` на цільовий об'єкт, оминаючи проксі. Статус авто оновлюється у БД без помилок, але `DealAuditAspect` **не активується** (свідома демонстрація обмеження proxy-based AOP).

---

## 4. Верифікація тестами

| Тестовий клас | Тип тесту | Що перевіряє |
| :--- | :--- | :--- |
| `ExecutionSlaAspectUnitTest` | Прямий Unit-тест без Spring (`mock(ProceedingJoinPoint.class)`) | Виклик `proceed()`, обчислення часу, фіксацію порушення SLA (`slaViolated = true`), роботу блоку `finally` при винятку |
| `Lab4AopProxyAndSelfInvocationTest` | Інтеграційний тест (`@SpringBootTest`) | Спрацювання `@AuditDealOperation` через проксі, відсутність спрацювання при `this.markReadyForSale()`, блокування операції аспектом `@GuardHighValueResale` для `ROLE_MECHANIC` |