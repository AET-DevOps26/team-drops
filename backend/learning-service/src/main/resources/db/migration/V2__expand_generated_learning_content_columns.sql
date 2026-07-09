ALTER TABLE learning_plans
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN goal TYPE TEXT;

ALTER TABLE lessons
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN topic TYPE TEXT;

ALTER TABLE exercises
    ALTER COLUMN question TYPE TEXT,
    ALTER COLUMN expected_answer TYPE TEXT;
