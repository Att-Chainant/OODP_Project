package MovieExceptions;
public class MovieDataAlreadyExist extends Exception {
	private static final long serialVersionUID = -1338660652787224983L;
	public MovieDataAlreadyExist(String Message) {
        super(Message);
    }
}