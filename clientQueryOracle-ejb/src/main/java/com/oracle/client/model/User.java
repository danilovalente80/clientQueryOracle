package com.oracle.client.model;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for user authentication and session management.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idUtente;
    private String username;
    private String nome;
    private String cognome;
    private String email;
    private Date dataInizio;
    private Date dataFine;
    private boolean attivo;

    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public Long getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(Long idUtente) {
        this.idUtente = idUtente;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(Date dataInizio) {
        this.dataInizio = dataInizio;
    }

    public Date getDataFine() {
        return dataFine;
    }

    public void setDataFine(Date dataFine) {
        this.dataFine = dataFine;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public String getNomeCompleto() {
        if (nome != null && cognome != null) {
            return nome + " " + cognome;
        }
        return username;
    }

    public boolean isValid() {
        if (!attivo) {
            return false;
        }

        Date now = new Date();

        // Check data inizio
        if (dataInizio != null && now.before(dataInizio)) {
            return false;
        }

        // Check data fine
        if (dataFine != null && now.after(dataFine)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", attivo=" + attivo +
                ", valid=" + isValid() +
                '}';
    }
}
