-- Dados FICTÍCIOS para demonstração e testes. Senha de todos: senha123
-- Domínio .invalid é reservado para exemplos (nenhum e-mail real).
INSERT INTO usuario (nome, email, senha_hash, perfil) VALUES
 ('Ana Solicitante',    'ana@exemplo.invalid',    '$2a$10$e8hR498EuAHSCxk5tY/NRuVYVztYDHUt7Rf415sqtaN43.crf3v0G', 'SOLICITANTE'),
 ('Bruno Solicitante',  'bruno@exemplo.invalid',  '$2a$10$e8hR498EuAHSCxk5tY/NRuVYVztYDHUt7Rf415sqtaN43.crf3v0G', 'SOLICITANTE'),
 ('Rita Responsavel',   'rita@exemplo.invalid',   '$2a$10$e8hR498EuAHSCxk5tY/NRuVYVztYDHUt7Rf415sqtaN43.crf3v0G', 'RESPONSAVEL'),
 ('Alex Administrador', 'admin@exemplo.invalid',  '$2a$10$e8hR498EuAHSCxk5tY/NRuVYVztYDHUt7Rf415sqtaN43.crf3v0G', 'ADMINISTRADOR');

INSERT INTO recurso (nome, tipo, capacidade, localizacao, competencia, restrito) VALUES
 ('Sala 101',               'SALA',      40, 'Bloco A - 1º andar', NULL,                FALSE),
 ('Laboratório 3',          'SALA',      25, 'Bloco B - 2º andar', NULL,                TRUE),
 ('Auditório',              'SALA',     200, 'Bloco C - térreo',   NULL,                TRUE),
 ('Projetor Epson 01',      'MATERIAL', NULL, 'Almoxarifado',      NULL,                FALSE),
 ('Notebook Dell 07',       'MATERIAL', NULL, 'Almoxarifado',      NULL,                FALSE),
 ('Prof. Carla Mendes',     'PROFESSOR', NULL, NULL,               'Qualidade de Software', FALSE),
 ('Prof. Diego Rocha',      'PROFESSOR', NULL, NULL,               'Banco de Dados',    FALSE);
