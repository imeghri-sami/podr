import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReadCallback extends Remote {
    void call(int v, Object value) throws RemoteException;

    int getMaxVersion() throws RemoteException;

    Object getValue() throws RemoteException;
}
