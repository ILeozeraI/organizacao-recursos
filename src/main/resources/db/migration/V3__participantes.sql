-- RN-09 (TDD): número de participantes da reserva (opcional).
ALTER TABLE reserva ADD COLUMN participantes INTEGER CHECK (participantes IS NULL OR participantes > 0);
