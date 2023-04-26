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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server extends UnicastRemoteObject implements Server_itf {

    private HashMap<String, Integer> bindingMap;

    //Les copies maitres
    private HashMap<Integer, Object> objects;

    private HashMap<Integer, AtomicInteger> versions;
    // private HashMap<Integer, Set<Client_itf>> clients;
    private AtomicInteger atomicInteger;
    private Set<Client_itf> clients;

    private Client_itf writer;
    private int barriere = 10;

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


        while (clients.size() >= barriere - 1) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        clients.add(client);

        if (clients.size() == barriere - 1) {
            notifyAll();
        }
        return clients;
    }

    public Set<Client_itf> addWriter(Client_itf client){

        barriere += 1;
        clients.add(client);
        writer = client;

        return clients;
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
            c -> {
                try {
                    c.update(idObjet, newVersion, valeur, /*?????? WriteCallback*/ null);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        );

        return newVersion;
    }

    @Override
    public Set<Client_itf> setMonitor(Moniteur m) throws RemoteException {
        serverMonitor = m;

        while (clients.size() < barriere);

        return clients;
    }

    @Override
    public Moniteur getMonitor() throws RemoteException {
        return serverMonitor;
    }

    /*public int getVersion(Integer id) {
        return versions.get(id);
    }*/

}
