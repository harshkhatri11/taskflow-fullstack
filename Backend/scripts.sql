create table users
(
    id         BIGINT GENERATED ALWAYS AS identity primary key,
    name       varchar        not null,
    email      varchar unique not null,
    password   varchar        not null,
    role       varchar        not null check (role in ('ADMIN', 'MANAGER', 'EMPLOYEE')),
    created_at timestamptz default now()
)

create table projects
(
    id          Bigint generated always as identity primary key,
    title       varchar not null,
    description varchar,
    status      varchar not null check (status in ('ACTIVE', 'COMPLETED', 'ARCHIVED')),
    manager_id  bigint references users (id) on delete restrict,
    created_at  timestamptz default now()
)

create table tasks
(
    id             Bigint generated always as identity primary key,
    title          varchar not null,
    description    varchar,
    status         varchar not null check (status in ('TODO', 'IN_PROGRESS', 'DONE')),
    priority       varchar not null check (priority in ('LOW', 'MEDIUM', 'HIGH')),
    due_date       date    not null,
    project_id     bigint references projects (id) on delete cascade,
    assigned_to_id bigint  references users (id) on delete set null,
    created_at     timestamptz default now()
)

create table comments
(
    id          Bigint generated always as identity primary key,
    content     varchar not null,
    task_id     bigint  not null references tasks (id) on delete cascade,
    author_id   bigint  references users (id) on delete set null,
    author_name varchar not null,
    created_at  timestamptz default now()
)