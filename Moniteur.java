import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Moniteur extends Remote {
	public void feuVert(String site, int facteur) throws RemoteException; 
	// minimal (indistinct, pas de trace) 

	public void signaler(String événement, String site, int idRegistre) throws RemoteException; 
	// événement = "DE","TE","DL",TL"
}