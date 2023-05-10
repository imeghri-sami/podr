import java.io.*;
import java.rmi.RemoteException;
import java.util.Set;

public class SharedObject implements Serializable, SharedObject_itf {

    private static final long serialVersionUID = 1L;
    private Integer idObj;

    private int version;

    public Object obj;

    private Set<Client_itf> clientsParticipants;

    public SharedObject(Integer id, Object obj) {
        this.idObj = id;
        this.obj = obj;
    }

    public SharedObject(Integer id) {
        this.idObj = id;
    }

    public void update(int v, Object valeur, WriteCallback wcb) {
        try {
            Client.monitor.feuVert(Client.getIdSite(), 4); // ** Instrumentation
            // ** attente quadruplée pour les ack, pour exhiber l'inversion de valeurs
            // getIdSite identique à getSite, mais non Remote

            // suite de la méthode update...

        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void reportValue(ReadCallback rcb) {
        try {
            System.out.println("feuvert");
            Client.monitor.feuVert(Client.getIdSite(), 1); // ** Instrumentation
            System.out.println("feuvert'");
            // int version = versions.get(idObj).get();
            Client_itf client = null;
            for (Client_itf c : clientsParticipants) {
                if (version <= c.getVersion(idObj)) {
                    version = c.getVersion(idObj);
                    client = c;
                }

            }


            if (client != null) {
                System.out.println("version --> " + version + "Client name ---> " + client.getSite());
                Object o = client.getObj(idObj);
                this.setObj(o);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    // invoked by the user program on the client node
    // passage par Client pour que les écritures soient demandées en séquence sur le
    // site
    public void write(Object o) {
        try {
            Client.monitor.signaler("DE", Client.getIdSite(), idObj); // ** Instrumentation
            Client.write(idObj, o);
            Client.monitor.signaler("TE", Client.getIdSite(), idObj); // ** Instrumentation
        } catch (RemoteException rex) {
            rex.printStackTrace();
        }
    }

    // pour simplifier (éviter les ReadCallBack actifs simultanés)
    // on évite les lectures concurrentes sur une même copie
    public synchronized Object read() {
        // déclarations méthode read....

        try {
            Client.monitor.signaler("DL", Client.getIdSite(), idObj); // ** Instrumentation

            // corps de la méthode read...
            // First attempt to implement the read method:
            // Client.reportValue(id, null); // Report value is not static
            // Object obj = Client.read(this.idObj);

            reportValue(null);

            System.out.println("read::obj ----> " + obj);
            Client.monitor.signaler("TL", Client.getIdSite(), idObj); // ** Instrumentation
            return obj;
        } catch (RemoteException rex) {
            rex.printStackTrace();
            return null;
        }
    }

    public void setId(int id) {
        this.idObj = id;
    }

    public int getId() {
        return this.idObj;
    }

    public String getVersion() {
        return version + "";
    }

    public Object getObj() {
        return this.obj;
    }

    public void setObj(Object newObj) {
        this.obj = newObj;
    }

    public void setClients(Set<Client_itf> clientsParticipants2) {
        this.clientsParticipants = clientsParticipants2;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}