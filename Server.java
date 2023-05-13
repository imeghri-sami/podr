import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class Server extends UnicastRemoteObject implements Server_itf {

    private HashMap<String, Integer> bindingMap;

    // Les copies maitres
    private HashMap<Integer, Object> objects;

    private HashMap<Integer, AtomicInteger> versions;
    // private HashMap<Integer, Set<Client_itf>> clients;
    private AtomicInteger atomicInteger;
    private Set<Client_itf> clients;

    private Client_itf writer;
    private int barriere = 3;

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
        synchronized (this) {
            if (clients.size() >= barriere) {
                return null;
            }

            clients.add(client);

            while (clients.size() < barriere) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            notifyAll();

            return clients;
        }
    }

    public boolean isWriter(Client_itf client) throws RemoteException {
        return client.equals(writer);
    }

    public Set<Client_itf> addWriter(Client_itf client) {

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
    public synchronized int publish(String name, Object o, boolean reset) throws RemoteException {
        int id = atomicInteger.incrementAndGet();

        if( bindingMap.containsKey(name)) return bindingMap.get(name);

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

        // Incrementer la version du l object qui a comme identifiant idObjet
        int newVersion = versions.get(idObjet).incrementAndGet();
        // Ajouter la nouvelle valeur à la liste des objets
        objects.put(idObjet, valeur);

        WriteCallback responseCllbck = new WriteCallbackImpl();

        // Envoyer des updates à tous les clients et attender la réponse des clients
        clients.forEach(
                c -> {

                        System.out.println("debut update");
                        new Thread(() -> {
                            try {
                                c.update(idObjet, newVersion, valeur, responseCllbck);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                        System.out.println("fin update");
                        System.out.println("recuperer le nombre des reponses reçues ...");

                });

        //synchronized (this) {
            /*   while( responseCllbck.getResponseCounter() < ( barriere / 2 ) ){

             try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }*/
            //notifyAll();
        //}
        System.out.println("fin while server write");
        return newVersion;
    }

    @Override
    public Set<Client_itf> setMonitor(Moniteur m) throws RemoteException {
        serverMonitor = m;

        System.out.println("waiting for clients ... " + barriere);


        while (clients.size() < barriere)
            ;

        for (Client_itf c : clients) {
            c.setMonitor(m);
        }
        return clients;
    }

    @Override
    public Moniteur getMonitor() throws RemoteException {
        return serverMonitor;
    }

    public int getVersion(Integer id) {
        return versions.get(id).get();
    }

}
