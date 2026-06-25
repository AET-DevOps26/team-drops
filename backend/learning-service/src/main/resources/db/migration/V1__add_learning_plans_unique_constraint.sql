DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_learning_plans_user_title'
    ) THEN
        ALTER TABLE learning_plans
            ADD CONSTRAINT uq_learning_plans_user_title UNIQUE (user_id, title);
    END IF;
END $$;
