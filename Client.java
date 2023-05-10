import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Client extends UnicastRemoteObject implements Client_itf {

    private static final int RMI_REGISTRY_PORT = 50051;
    private static final String RMI_REGISTRY_HOSTNAME = "localhost";
    private static HashMap<Integer, SharedObject> sharedObjects;
    private static Set<Client_itf> clientsParticipants;

    //private static HashMap<Integer, AtomicInteger> versions; // Ajout d'une map de correspondance entre les shared
    // objects et leurs versions. Util??
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

    public static void write(Integer id, Object o) throws RemoteException {
        //if (server.isWriter(instanceClient))
            server.write(id, o);
    }

    /*
     * reads the shared object of id id
     * 
     * public static Object read(Integer id) {
     * SharedObject so = sharedObjects.get(id);
     * 
     * so.read();
     * 
     * try {
     * instanceClient.reportValue(id, null); // note: cannot call the method
     * reportValue (logically) because
     * // there is no callback
     * } catch (Exception e) {
     * e.getMessage();
     * }
     * return sharedObjects.get(id);
     * }
     */

    @Override
    public void initSO(int idObj, Object valeur) throws RemoteException {
        SharedObject so = new SharedObject(idObj, valeur);
        so.setClients(clientsParticipants);
        sharedObjects.put(idObj, so);
        //versions.put(idObj, new AtomicInteger(0));
    }

    /*
     * Update : the method reportValue now does the same thing as lookFor but
     * updates the shared object at the same time
     * Problem : doesn't call the method report value of shared object
     */
    @Override
    public void reportValue(int idObj, ReadCallback rcb) throws RemoteException {

        SharedObject so = sharedObjects.get(idObj);
        so.reportValue(rcb);

        /*
         * int version = versions.get(idObj).get();
         * Client_itf client = null;
         * for (Client_itf c : clientsParticipants) {
         * if (version < c.getVersion(idObj)) {
         * version = c.getVersion(idObj);
         * client = c;
         * }
         * 
         * }
         * if (client != null) {
         * Object o = client.getObj(idObj);
         * SharedObject so = sharedObjects.get(idObj);
         * so.setObj(o);
         * versions.get(so).set(version);
         * }
         */

    }

    /*
     * This method will look for the client who has the highest version of an object
     * and return the object
     * 
     * public static Object lookFor(int idObj) {
     * int version = 0;
     * Client_itf client = null;
     * for (Client_itf c : clientsParticipants) {
     * if (version < c.getVersion(idObj)) {
     * version = c.getVersion(idObj);
     * client = c;
     * }
     * 
     * }
     * return client.getObj(idObj);
     * 
     * }
     */

    @Override
    public void update(int idObj, int version, Object valeur, WriteCallback wcb) throws RemoteException {
       // wcb.ok();
        SharedObject so = sharedObjects.get(idObj);
        so.setObj(valeur);
        //versions.get(idObj).set(version);
        so.setVersion(version);

        System.out.println("objet : " + so.getObj());
        System.out.println("version : " + so.getVersion() );
    }

    @Override
    public String getSite() throws RemoteException {
        return name;
    }

    @Override
    public Object getObj(String name) throws RemoteException {
        int id = server.lookup(name);
        Object o = sharedObjects.get(id).getObj();
        return o;
    }

    @Override
    public Object getObj(int id) throws RemoteException {
        Object o = sharedObjects.get(id).getObj();
        return o;
    }

    /* Get la version d'un objet chez le client à partir de son nom. Util?? */
    @Override
    public int getVersion(String name) throws RemoteException {
        int idObj = server.lookup(name);
        return Integer.parseInt(sharedObjects.get(idObj).getVersion());
    }

    @Override
    public void setMonitor(Moniteur m) throws RemoteException {
        monitor = m;
    }

    @Override
    public int getVersion(Integer idObj) throws RemoteException {
        return Integer.parseInt(sharedObjects.get(idObj).getVersion());
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
            sharedObjects = new HashMap<>();
            instanceClient = new Client();
            //versions = new HashMap<>();
            clientsParticipants = server.addClient(instanceClient);
            if ( clientsParticipants == null) System.exit(0);


            //monitor = server.getMonitor();
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
    }

    public static SharedObject publish(String string, String obj, boolean b) {
        int id = 0;
        try {
            id = server.publish(string, obj, b);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        return sharedObjects.get(id);
    }

    public static SharedObject lookup(String objname) {
        try {
            final int objectReference = server.lookup(objname);

            if (objectReference == -1)
                return null;

            SharedObject obj = sharedObjects.values()
                    .stream()
                    .filter(e -> e.getId() == objectReference)
                    .findFirst().orElse(null);
            if (obj == null) {
                // add the shared object to the list
                obj = new SharedObject(objectReference);
                sharedObjects.put(objectReference, obj);
            }

            return obj;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

}