# Матеріали та скріншоти до Лабораторної роботи №1

Покладіть у цю папку (`labs/lab1/`) 4 скріншоти після запуску проєкту:

1. `01-docker-and-oidc-ui.png` — запущені контейнери `garage-postgres` та `garage-keycloak` + відкрита в браузері сторінка `/ui/vehicles` після успішного логіну через Keycloak (`admin_user`).
2. `02-jwt-api-auth.png` — вікно Postman: запит `03. GET /api/vehicles БЕЗ токена` (`401 Unauthorized`) та `04. GET /api/vehicles З JWT` (`200 OK`).
3. `03-rbac-403-forbidden.png` — вікно Postman: перевірка обмеження прав (`07. SecurityFilterChain -> 403 Forbidden` та `09. @PreAuthorize -> 403 Forbidden`).
4. `04-mvn-test-green.png` — термінал IntelliJ IDEA із зеленим результатом виконання тестів (`Tests run: 4, Failures: 0`).
