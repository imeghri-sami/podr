import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;

public class Client extends UnicastRemoteObject implements Client_itf {

    private static final int RMI_REGISTRY_PORT = 50051;
    private static final String RMI_REGISTRY_HOSTNAME = "localhost";
    private static List<SharedObject> sharedObjects;
    private static Set<Client_itf> clientsParticipants;

    private static Client instanceClient;
    private static Server_itf server;

    private static String name;

    static Moniteur monitor;

    protected Client() throws RemoteException {
        super();
    }

    public static String getIdSite() {
        return name;
    }

    public static void write(Integer id, Object o) {
    }

    @Override
    public void initSO(int idObj, Object valeur) throws RemoteException {
        SharedObject so = new SharedObject(idObj, valeur);
        sharedObjects.add(so);
    }

    @Override
    public void reportValue(int idObj, ReadCallback rcb) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reportValue'");
    }

    @Override
    public void update(int idObj, int version, Object valeur, WriteCallback wcb) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public String getSite() throws RemoteException {
        return name;
    }

    @Override
    public Object getObj(String name) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getObj'");
    }

    @Override
    public int getVersion(String name) throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'getVersion'");
    }

    public static void init(String myName) {
        try {
            server = (Server_itf) Naming.lookup("rmi://"
                    + RMI_REGISTRY_HOSTNAME + ":"
                    + RMI_REGISTRY_PORT
                    + "/server");
            if (server == null)
                System.out.println("Server null");
            // Initialiser le nom du site
            Client.name = myName;
            instanceClient = new Client();
            clientsParticipants = server.addClient(instanceClient);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
    }

    public static SharedObject publish(String string, String string2, boolean b) {
        try {
            server.publish(string, string2, b);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static SharedObject lookup(String objname) {
        try {
            final int objectReference = server.lookup(objname);

            if (objectReference == -1)
                return null;

            SharedObject obj = sharedObjects
                    .stream()
                    .filter(e -> e.getId() == objectReference)
                    .findFirst().orElse(null);
            if (obj == null) {
                // add the shared object to the list
                obj = new SharedObject(objectReference);
                sharedObjects.add(obj);
            }

            return obj;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

}