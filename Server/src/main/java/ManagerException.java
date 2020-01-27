public class ManagerException extends Exception{
    public ManagerException(){}
    public ManagerException(String message){
        super(message);
    }
    public ManagerException(String message, Throwable cause){
        super(message,cause);
    }

}
