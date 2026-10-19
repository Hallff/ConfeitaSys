package br.com.confeitasys.dto;

public record OkResposta(boolean ok) {
    public static final OkResposta OK = new OkResposta(true);
}
