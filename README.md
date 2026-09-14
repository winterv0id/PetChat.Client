# PetChatClient (Android)

Android-клиент чата, использующий `petchat-client-api` для взаимодействия с сервером.

## Стек

- **Room** — локальная БД
- **Hilt** — Dependency Injection
- **Firebase Cloud Messaging** — push уведомления

## Backend-структура

```
api/
  HubClientManager.java        обёртка над HubClient (petchat-client-api) — токен, жизненный цикл
hilt/modules/                  DI: ApiModule, DatabaseModule, SystemServicesModule
database/
  PetChatDatabase.java         Room-database IMPL
  dao/                         *entity*Dao
  entities/                    Room сущности
  mappers/                     DTO из petchat-client-api -> Room сущности
  management/
    UserEventStoreManager.java EventStore IMPL
services/
  FcmMessagingService.java     обработка push уведомлений от FCM
  FcmTokenManager.java         регистрация/обновление FCM-токена на сервере
security/securestorage/
  SecurePreferences.java       Android Keystore
```

---

## `UserEventStoreManager` — `EventStore` IMPL 

Реализует контракт, который требует `petchat-client-api`.

Ключевые свойства:

- **Одна транзакция** (`db.runInTransaction`) на запись события и продвижение `applied_state` — либо 
  применяется всё, либо (при исключении) не применяется ничего. Тест, который явно проверяет это поведение — 
  `UserEventStoreManagerTest.applySnapshot_FailureMidway_RollsBackTransaction_OldDataSurvives`.
- **Однопоточный `dbExecutor`** — все вызовы `saveAppliedEvent`/`applySnapshot`
  идут строго по очереди, в порядке вызова. Это важно: `Synchronizator (petchat-client-api)`
  не ждет записи одного события перед отправкой следующего, порядок применения на диске 
  гарантируется тем, что все они уходят в один и тот же executor.

## Пагинация истории — `MessagesDao`

```
getMinIndexOfWindowBefore(chatId, beforeIndex, limit)
    -> минимальный index среди limit сообщений до beforeIndex

observeMessagesFrom(chatId, minIndex)
    -> LiveData со всеми сообщениями чата с index >= minIndex, отсортированными по index DESC
```

Экран чата подгружает историю "страницами": берёт текущий `minIndex`, получает через 
`getMinIndexOfWindowBefore` границу следующей более старой страницы, обновляет `minIndex`, 
и `observeMessagesFrom` автоматически подхватывает расширенное окно (`LiveData`).

Если окно исчерпано **локально** (сообщений раньше `beforeIndex` в кэше нет) —
история получается с сервера через API (`GET /chats.getHistory`), 
пока сервер не скажет об отсутствии более старых сообщений.

## `HubClientManager` — жизненный цикл соединения SignalR

```java
public synchronized HubClient getHubInstance() {
    if (hubClient == null) hubClient = create();
    return hubClient;
}

private HubClient create() {
    hubClient = new HubClient(
            BuildConfig.SERVER_API_ADDR,
            () -> SecurePreferences.getStringValue(context, "ApiAccessToken", ""), // Supplier
            eventStore);
    return hubClient;
}

public void startHubConnection() {
        if (hubConnected) return;
        CompletableFuture.runAsync(() -> {
            try {
                Log.d("HubClientManager", "Connecting signalR hub...");
                getHubInstance().connect();
                hubConnected = true;
            } catch (ExecutionException | InterruptedException e) {
                Log.e("HubClientManager", "Connection error!", e);
            }
        }, hubExecutor);
    }

    public synchronized void reconnect() {
        disconnect();
        startHubConnection();
    }

    public void disconnect() {
        if (hubClient == null) return;

        Log.d("HubClientManager", "Disconnecting signalR hub...");
        hubClient.disconnect();
        hubClient = null;
    }
```

`HubClient` принимает `Supplier<String>`, а не саму строку токена — он сам вызывает поставщик
заново при каждом (пере)подключении, включая **автоматические внутренние реконнекты** после 
обрыва сети (см. README `petchat-client-api`).

---

## Тесты

```bash
./gradlew testDebugUnitTest
```

Используется JUnit 4.