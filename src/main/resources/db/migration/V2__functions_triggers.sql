-- V2: database functions and triggers

-- Auto-refresh account.last_updated on every update
CREATE OR REPLACE FUNCTION fn_set_last_updated()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.last_updated := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_account_last_updated
    BEFORE UPDATE ON account
    FOR EACH ROW EXECUTE FUNCTION fn_set_last_updated();

-- Ledger is append-only: no UPDATE or DELETE
CREATE OR REPLACE FUNCTION fn_prevent_ledger_modification()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ledger is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_ledger_immutable
    BEFORE UPDATE OR DELETE ON ledger
    FOR EACH ROW EXECUTE FUNCTION fn_prevent_ledger_modification();