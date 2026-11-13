-- Esquema do sistema de Organização de Recursos
-- btree_gist permite combinar igualdade (recurso_id) com sobreposição de intervalo (&&)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL UNIQUE,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL CHECK (perfil IN ('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR'))
);

CREATE TABLE sessao (
    token       VARCHAR(64) PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario(id),
    criada_em   TIMESTAMPTZ NOT NULL,
    expira_em   TIMESTAMPTZ NOT NULL,
    revogada    BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE recurso (
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(120) NOT NULL,
    tipo         VARCHAR(20)  NOT NULL CHECK (tipo IN ('SALA','MATERIAL','PROFESSOR')),
    capacidade   INTEGER      CHECK (capacidade IS NULL OR capacidade >= 0),
    localizacao  VARCHAR(120),
    competencia  VARCHAR(120),
    restrito     BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE manutencao (
    id          BIGSERIAL PRIMARY KEY,
    recurso_id  BIGINT       NOT NULL REFERENCES recurso(id),
    inicio      TIMESTAMPTZ  NOT NULL,
    fim         TIMESTAMPTZ  NOT NULL,
    motivo      VARCHAR(255) NOT NULL,
    CHECK (fim > inicio)
);

CREATE TABLE reserva (
    id              BIGSERIAL PRIMARY KEY,
    recurso_id      BIGINT       NOT NULL REFERENCES recurso(id),
    professor_id    BIGINT       REFERENCES recurso(id),
    solicitante_id  BIGINT       NOT NULL REFERENCES usuario(id),
    inicio          TIMESTAMPTZ  NOT NULL,
    fim             TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    finalidade      VARCHAR(255),
    criada_em       TIMESTAMPTZ  NOT NULL,
    versao          BIGINT       NOT NULL DEFAULT 0,
    CHECK (fim > inicio),
    -- RF6: proteção contra dupla reserva no BANCO (vale mesmo com requisições simultâneas).
    -- Intervalo semiaberto [inicio, fim): terminar às 10h e outra começar às 10h NÃO é conflito.
    CONSTRAINT reserva_sem_sobreposicao_recurso EXCLUDE USING gist (
        recurso_id WITH =,
        tstzrange(inicio, fim, '[)') WITH &&
    ) WHERE (status IN ('SOLICITADA','APROVADA','EM_USO')),
    -- RF5: a agenda do professor também não pode ter sobreposição.
    CONSTRAINT reserva_sem_sobreposicao_professor EXCLUDE USING gist (
        professor_id WITH =,
        tstzrange(inicio, fim, '[)') WITH &&
    ) WHERE (status IN ('SOLICITADA','APROVADA','EM_USO') AND professor_id IS NOT NULL)
);

-- RF10: histórico auditável. Sem FK para reserva de propósito: o histórico
-- sobrevive à exclusão de uma reserva ainda não iniciada.
CREATE TABLE auditoria_reserva (
    id               BIGSERIAL PRIMARY KEY,
    reserva_id       BIGINT       NOT NULL,
    usuario_id       BIGINT       NOT NULL,
    instante         TIMESTAMPTZ  NOT NULL,
    estado_anterior  VARCHAR(20),
    estado_novo      VARCHAR(20)  NOT NULL,
    motivo           VARCHAR(255) NOT NULL
);
CREATE INDEX idx_auditoria_reserva ON auditoria_reserva(reserva_id);

-- RF9: retirada e devolução de materiais
CREATE TABLE movimentacao_material (
    id          BIGSERIAL PRIMARY KEY,
    reserva_id  BIGINT      NOT NULL REFERENCES reserva(id),
    tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('RETIRADA','DEVOLUCAO')),
    usuario_id  BIGINT      NOT NULL REFERENCES usuario(id),
    instante    TIMESTAMPTZ NOT NULL,
    observacao  VARCHAR(255)
);

-- RF12: tentativas recusadas por sobreposição (conflitos evitados)
CREATE TABLE conflito_evitado (
    id            BIGSERIAL PRIMARY KEY,
    recurso_id    BIGINT      NOT NULL,
    professor_id  BIGINT,
    inicio        TIMESTAMPTZ NOT NULL,
    fim           TIMESTAMPTZ NOT NULL,
    registrado_em TIMESTAMPTZ NOT NULL
);

-- RF11: notificações enviadas à API externa (simulada)
CREATE TABLE notificacao (
    id          BIGSERIAL PRIMARY KEY,
    reserva_id  BIGINT       NOT NULL,
    evento      VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    detalhe     VARCHAR(255),
    instante    TIMESTAMPTZ  NOT NULL
);
