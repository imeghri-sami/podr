import java.io.*;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

            this.obj = valeur;

            this.version = v;

            // Envoyer un ACK au serveur
            wcb.call();
            System.out.println("objet : " + obj);
            System.out.println("version : " + version );

        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void reportValue(ReadCallback rcb) {
        try {
            Client.monitor.feuVert(Client.getIdSite(), 1); // ** Instrumentation
            // int version = versions.get(idObj).get();

            Client_itf client = null;
            /*for (Client_itf c : clientsParticipants) {
                if (version <= c.getVersion(idObj)) {
                    version = c.getVersion(idObj);
                    client = c;
                }

            }*/

            /*clientsParticipants.forEach(c -> {
                try {
                    c.reportValue(idObj, rcb);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });*/

            rcb.call(version, obj);

            // attendre la reponse de n/2 clients

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
            ReadCallback readCallback = new ReadCallbackImpl();
            int numThreads = clientsParticipants.size();
            Thread[] clientThreads = new Thread[numThreads];
            CountDownLatch latch = new CountDownLatch(numThreads);

            int index = 0;

            for( Client_itf c : clientsParticipants ){
                clientThreads[index] = new Thread(() -> {
                    try {
                        c.reportValue(idObj, readCallback);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
                clientThreads[index++].start();
            }

            try {
                latch.await(numThreads/2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Half clients have finished ...");
            //reportValue(readCallback);

            this.version = readCallback.getMaxVersion();
            this.obj = readCallback.getValue();

            assert this.obj != null;

            Client.monitor.signaler("TL", Client.getIdSite(), idObj); // ** Instrumentation
            return this.obj;
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