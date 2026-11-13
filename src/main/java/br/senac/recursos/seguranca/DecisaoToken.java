package br.senac.recursos.seguranca;

public record DecisaoToken(boolean permitido, MotivoNegacao motivo, Long usuarioId) {
    public static DecisaoToken negar(MotivoNegacao motivo) {
        return new DecisaoToken(false, motivo, null);
    }

    public static DecisaoToken permitir(Long usuarioId) {
        return new DecisaoToken(true, null, usuarioId);
    }
}
