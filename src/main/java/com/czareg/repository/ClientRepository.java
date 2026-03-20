package com.czareg.repository;

import com.czareg.Client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClientRepository {

    private final Map<Long, Client> clients = new HashMap<>();

    public ClientRepository(List<Client> clients) {
        clients.forEach(client -> this.clients.put(client.id(), client));
    }

    public List<Client> getAll() {
        return List.copyOf(clients.values());
    }

    public Optional<Client> find(long clientId) {
        return Optional.ofNullable(clients.get(clientId));
    }
}
