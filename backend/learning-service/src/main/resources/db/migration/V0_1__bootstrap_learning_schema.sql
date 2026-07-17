CREATE TABLE IF NOT EXISTS learning_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    goal TEXT NOT NULL,
    language VARCHAR(255) NOT NULL,
    level VARCHAR(255) NOT NULL,
    duration VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    progress INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    topic TEXT NOT NULL,
    order_number INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS exercises (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    difficulty VARCHAR(255) NOT NULL,
    expected_answer TEXT
);

CREATE TABLE IF NOT EXISTS lesson_content_blocks (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    order_number INTEGER NOT NULL,
    type VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    subtitle VARCHAR(255),
    text TEXT,
    points_json TEXT
);
