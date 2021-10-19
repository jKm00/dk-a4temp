package no.ntnu.datakomm.chat.server;

import java.security.KeyException;
import java.util.HashMap;
import java.util.Map;

public class ClientRegister {
    private HashMap<String, Client> clients;

    /**
     * Creates a client register
     */
    public ClientRegister() {
        this.clients = new HashMap<>();
    }

    /**
     * Returns a client by the given username
     *
     * @param username the username of the client
     * @return the client with the username given
     */
    public Client getClientByName(String username) {
        return this.clients.get(username);
    }

    /**
     * Adds a client to the register. If a client with the same username
     * is already in the register, the client will not be added and
     * {@code false} will be returned.s
     *
     * @param client the client to add to the register
     * @return {@code true} if client was added, {@code false} otherwise
     */
    public boolean addClient(Client client) {
        boolean clientAdded = false;
        if (!this.clients.containsKey(client.getUsername())) {
            this.clients.put(client.getUsername(), client);
            clientAdded = true;
        }
        return clientAdded;
    }

    /**
     * Checks if the register contains a client with the username given
     *
     * @param username the username to check
     * @return {@code true} if the username is in the register, {@code false}
     * otherwise.
     */
    public boolean contains(String username) {
        return this.clients.containsKey(username);
    }

    /**
     * Returns a map of all the clients in the register
     * @return a map of all clients in the register
     */
    public Map<String, Client> getClients() {
        return this.clients;
    }

    /**
     * Removes a client from the register
     *
     * @param client the client to be removed
     * @return the client that was just removed from the register
     */
    public void removeClient(String username) {
        this.clients.remove(username);
    }
}
