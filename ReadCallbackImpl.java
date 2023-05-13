import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.TreeMap;

public class ReadCallbackImpl extends UnicastRemoteObject implements ReadCallback {
    private TreeMap<Integer, Object> objects = new TreeMap<>();

    protected ReadCallbackImpl() throws RemoteException {
    }

    @Override
    public void call(int v, Object value) throws RemoteException {
        System.out.println("v = " + v + ", value = " + value);
        if ( !objects.containsKey(v) ) objects.put(v, value);
    }

    @Override
    public int getMaxVersion() {
        return objects.lastKey();
    }
    @Override
    public Object getValue(){
        return objects.lastEntry().getValue();
    }
}
