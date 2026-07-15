-- Đồng bộ lại TẤT CẢ sequence (SERIAL) với dữ liệu hiện có trong schema public.
--
-- Lý do: DB được nạp/restore (vd Supabase dump) bằng ID tường minh mà không đẩy sequence,
-- nên nextval trả về ID đã tồn tại → mọi INSERT mới (insight, booking, payment, user...)
-- có thể vi phạm khóa chính. Migration này setval mỗi sequence = MAX(id) hiện có,
-- an toàn và idempotent (chạy lại nhiều lần vẫn đúng).
DO $$
DECLARE
    rec RECORD;
    max_id BIGINT;
BEGIN
    FOR rec IN
        SELECT
            n.nspname AS schema_name,
            t.relname AS table_name,
            a.attname AS column_name
        FROM pg_class s
        JOIN pg_depend d    ON d.objid = s.oid AND d.deptype = 'a'
        JOIN pg_class t     ON t.oid = d.refobjid
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = d.refobjsubid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE s.relkind = 'S'
          AND n.nspname = 'public'
    LOOP
        EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I.%I',
                       rec.column_name, rec.schema_name, rec.table_name)
        INTO max_id;

        EXECUTE format('SELECT setval(pg_get_serial_sequence(%L, %L), GREATEST(%s, 1))',
                       rec.schema_name || '.' || rec.table_name,
                       rec.column_name,
                       max_id);
    END LOOP;
END $$;
