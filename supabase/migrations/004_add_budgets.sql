-- 004_add_budgets.sql — budgets table for Expenses budgeting feature
create table if not exists public.budgets (
    id text primary key,
    type text not null,
    category text,
    amount double precision not null,
    recurring boolean not null,
    year_month text,
    week_block integer,
    start_date text,
    end_date text,
    label text,
    last_modified bigint not null,
    is_deleted boolean not null default false
);

create index if not exists idx_budgets_type on public.budgets (type);
create index if not exists idx_budgets_year_month on public.budgets (year_month);

-- Match the RLS posture of every other DaySync table: enabled with a single
-- permissive policy (single-user app; anon key is embedded in the client).
alter table public.budgets enable row level security;
create policy "Allow all" on public.budgets for all to public using (true) with check (true);
