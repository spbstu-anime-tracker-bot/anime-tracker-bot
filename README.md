# Anime Tracker and Advisor Bot

Telegram-бот для ведения списков аниме и получения персональных рекомендаций на LLM Gigachat.

## Возможности

- Поиск аниме по названию (`/search`)
- Расширенный поиск по жанру, типу и году (`/search_by`)
- Список просмотренных аниме (`/list_viewed`)
- Список отслеживаемых аниме (`/list_to_view`)
- Оценка аниме от 1 до 10
- Персональные рекомендации от Ollama LLM (`/advise`)
- Health Check endpoint (`GET /health`)
- Admin API (`GET /admin/users`)

## Технологический стек

| Технология | Версия | Назначение                           |
|---|---|--------------------------------------|
| Java | 25 | Язык разработки                      |
| Spring Framework | 7.0.7 | Core, MVC, JPA, Events               |
| Spring REST (Spring MVC) | 7.0.7 | REST API (`/health`, `/admin/users`) |
| Spring JPA + Hibernate | 6.6.x | ORM, работа с PostgreSQL             |
| Spring Modulith | 2.0.6 | Модульная архитектура                |
| PostgreSQL | 17 | Основная база данных                 |
| Apache Kafka | 7.6.0 | Асинхронная обработка рекомендаций   |
| Ollama | 0.24.0 | Локальная LLM (модель GigaChat)      |
| Docker / Docker Compose | 28.5.2 | Контейнеризация                      |
| telegrambots | 7.11.0 | Telegram Bot API                     |

## Архитектура модулей (Spring Modulith)

Приложение разбито на 9 модулей:


```
anime          — каталог аниме (Spring JPA + Hibernate)
user           — регистрация пользователей
tracking       — списки просмотренных/отслеживаемых, оценки
recommendation — рекомендательный движок (Kafka + Ollama)
llm            — абстракция над LLM-провайдером (интерфейс LlmService)
bot            — Telegram-бот (telegrambots v7)
admin          — Spring REST Admin API
health         — Spring REST Health Check
config         — конфигурация Kafka
```

Граф зависимостей без циклов верифицируется тестом `ModulithStructureTest`.

## Развёртывание

### Предварительные требования

- Docker и Docker Compose
- Telegram Bot Token ([BotFather](https://t.me/BotFather))
- [Ollama](https://ollama.com/) — запущен локально на хосте (порт 11434)

### 1. Установка и запуск Ollama

```bash
# Скачать и запустить Ollama (https://ollama.com/download)
ollama pull gemma3
ollama serve
```

Ollama должен быть доступен по адресу `http://localhost:11434`.

### 2. Сборка JAR (обязательно перед Docker)

```bash
./gradlew bootJar
```

### 3. Настройка переменных окружения

Создайте файл `.env` в корне проекта:

```env
TELEGRAM_BOT_TOKEN=токен_бота
TELEGRAM_BOT_USERNAME=имя_бота
ADMIN_API_TOKEN=секретный_токен
GIGACHAT_AUTH_KEY=auth_key
GIGACHAT_SCOPE=GIGACHAT_API_PERS
```

Необязательные переменные (есть значения по умолчанию):

```env
DB_URL=jdbc:postgresql://postgres:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

### 4. Подключение базы данных

#### Вариант A: Docker PostgreSQL (рекомендуется)

База данных запускается автоматически. Схемы и таблицы создаются при первом запуске из `docker/init/01_schemas.sql`.

Если есть заполненный дамп — поместите его в папку `docker/init/` с именем `02_data.sql`.

```bash
pg_dump -U postgres -h localhost postgres > docker/init/02_data.sql
```

#### Вариант B: Подключение к внешнему PostgreSQL

Измените переменные в `.env`:

```env
DB_URL=jdbc:postgresql://host.docker.internal:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=ваш_пароль
```

### 5. Запуск через Docker Compose

```bash
docker-compose up -d
```

Порядок запуска: PostgreSQL → Kafka → приложение (с ожиданием healthcheck).

### 6. Локальный запуск (без Docker)

Требуются локально запущенные PostgreSQL и Kafka.

```bash
export TELEGRAM_BOT_TOKEN=ваш_токен
export TELEGRAM_BOT_USERNAME=имя_бота
export ADMIN_API_TOKEN=секрет
./gradlew bootRun
```

## API

### Health Check

```
GET /health
```

Пример ответа (все сервисы работают):
```json
{
  "status": "UP",
  "postgresql": "UP",
  "kafka": "UP",
  "ollama": "UP"
}
```

HTTP 200 при `"status": "UP"`, HTTP 503 при `"status": "DEGRADED"`.

### Список пользователей

```
GET /admin/users
Authorization: Bearer <ADMIN_API_TOKEN>
```

Пример ответа:
```json
[
  {
    "telegramId": 123456789,
    "name": "Иван",
    "viewedCount": 42,
    "toViewCount": 7,
    "ratedCount": 30
  }
]
```

HTTP 401 при отсутствии или неверном токене.

## Команды бота

| Команда | Описание |
|---|---|
| `/start` | Регистрация и приветствие |
| `/search <название>` | Поиск по названию (японскому или английскому) |
| `/search_by <параметры>` | Расширенный поиск (жанр, тип, год через запятую) |
| `/list_viewed` | Список просмотренных аниме |
| `/list_to_view` | Список отслеживаемых аниме |
| `/advise` | Получить персональные рекомендации от LLM |

### Примеры `/search_by`

```
/search_by Action
/search_by TV, 2020
/search_by Comedy, Movie, 2019
/search_by Action, 2021
```

Параметры определяются автоматически: год (1900–2030), тип (TV/Movie/OVA/ONA/Special/Music), жанр (из базы данных).

## Как работает `/advise`

1. Если есть кэшированные рекомендации — показываются мгновенно.
2. Если нет просмотренных аниме — показываются 10 самых популярных.
3. Иначе — запрос отправляется в Kafka, воркер обращается к Ollama.
4. После генерации пользователь получает уведомление с результатом.

Рекомендации автоматически пересчитываются при добавлении нового аниме в просмотренные или изменении оценки.

## Схема базы данных

Две схемы PostgreSQL:

- `anime` — каталог: таблицы `anime`, `genre`, `"animeGenres"`
- `"user"` — пользовательские данные: `"user"`, `"listViewed"`, `"listToView"`, `"recommendationRequests"`, `"recommendationCache"`

Схемы создаются автоматически из `docker/init/01_schemas.sql`.