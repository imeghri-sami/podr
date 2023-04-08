import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Client_itf extends java.rmi.Remote {
	public void initSO(int idObj, Object valeur) throws java.rmi.RemoteException;
	public void reportValue(int idObj, ReadCallback rcb) throws java.rmi.RemoteException;
	public void update(int idObj, int version, Object valeur, WriteCallback wcb) throws java.rmi.RemoteException;
	// instrumentation : fournit un nom pour le site, fixé à l'initialisation
	public String getSite() throws java.rmi.RemoteException;
	public Object getObj(String name) throws java.rmi.RemoteException;
	public int getVersion(String name) throws java.rmi.RemoteException;
}