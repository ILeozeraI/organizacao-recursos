package br.senac.recursos.seguranca;

import br.senac.recursos.dominio.Perfil;

public record UsuarioAutenticado(Long id, String nome, Perfil perfil) {
    public boolean eh(Perfil p) {
        return perfil == p;
    }
}
