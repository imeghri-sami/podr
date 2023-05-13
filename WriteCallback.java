import java.rmi.RemoteException;

public interface WriteCallback extends java.rmi.Remote{
    void call() throws RemoteException;

    int getResponseCounter() throws RemoteException;
}
