import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IObserver extends Remote{
    void eventHappened() throws RemoteException;
    //void ticketsBought(Sale sale) throws ManagerException, RemoteException;

}
