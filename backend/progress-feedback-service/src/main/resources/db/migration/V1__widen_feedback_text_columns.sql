DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'feedback'
          AND column_name = 'message'
    ) THEN
        ALTER TABLE feedback ALTER COLUMN message TYPE TEXT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'feedback'
          AND column_name = 'corrected_answer'
    ) THEN
        ALTER TABLE feedback ALTER COLUMN corrected_answer TYPE TEXT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'user_answers'
          AND column_name = 'answer_text'
    ) THEN
        ALTER TABLE user_answers ALTER COLUMN answer_text TYPE TEXT;
    END IF;
END $$;
