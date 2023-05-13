import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class WriteCallbackImpl extends UnicastRemoteObject implements WriteCallback{

    private int responseCounter = 0;

    protected WriteCallbackImpl() throws RemoteException {
    }

    @Override
    public synchronized void call() throws RemoteException {
        System.out.println("writeCallback::call : " + this.responseCounter);
        this.responseCounter++;
    }

    @Override
    public int getResponseCounter() {
        return responseCounter;
    }
}
