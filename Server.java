import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends UnicastRemoteObject implements Server_itf {

    private HashMap<String, Integer> bindingMap;

    private HashMap<Integer, Object> objects;

    private HashMap<Integer, AtomicInteger> versions;
    // private HashMap<Integer, Set<Client_itf>> clients;
    private AtomicInteger atomicInteger;
    private Set<Client_itf> clients;
    private final int barriere = 10;

    private Moniteur serverMonitor;

    protected Server() throws RemoteException {
        bindingMap = new HashMap<>();
        objects = new HashMap<>();
        versions = new HashMap<>();

        atomicInteger = new AtomicInteger(0);

        clients = new HashSet<>();
    }


    static final int RMI_REGISTRY_PORT = 50051;
    static final String RMI_REGISTRY_HOSTNAME = "localhost";
    public static void main(String[] args) {
        try {
            Server server = new Server();
            LocateRegistry.createRegistry(RMI_REGISTRY_PORT);
            Naming.rebind("rmi://" + RMI_REGISTRY_HOSTNAME + ":" + RMI_REGISTRY_PORT + "/server", server);
            System.out.println("RMI registry started");

        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Set<Client_itf> addClient(Client_itf client) throws RemoteException {
        if (clients.size() >= barriere) {
            return null;
        } else {
            clients.add(client);
            while (clients.size() < barriere) {
                clients.add(client);
            }
            return clients;
        }
    }

    @Override
    public int lookup(String name) throws RemoteException {
        Integer id = bindingMap.get(name);
        return id == null ? -1 : id;
    }

    @Override
    public int publish(String name, Object o, boolean reset) throws RemoteException {
        int id = atomicInteger.incrementAndGet();
        bindingMap.put(name, id);
        objects.put(id, o);
        versions.put(id, new AtomicInteger(0));

        for (Client_itf client : clients) {
            client.initSO(id, o);
        }

        return id;
    }

    @Override
    public String[] list() throws RemoteException {
        return bindingMap.keySet().toArray(new String[0]);
    }

    @Override
    public int write(int idObjet, Object valeur) throws RemoteException {

        int newVersion = versions.get(idObjet).incrementAndGet();

        objects.put(idObjet, valeur);


        clients.forEach(
            c -> c.update(idObjet, newVersion, valeur, /*?????? WriteCallback*/ null)
        );

        return newVersion;
    }

    @Override
    public Set<Client_itf> setMonitor(Moniteur m) throws RemoteException {
        serverMonitor = m;
        // attendre que les clients soient prets
        while (clients.size() < barriere);
        return clients;
    }

    @Override
    public Moniteur getMonitor() throws RemoteException {
        return serverMonitor;
    }

}
