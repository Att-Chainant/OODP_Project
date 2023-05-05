package MovieExceptions;
public class MovieDataNoLongerExist extends Exception {
	private static final long serialVersionUID = 2108750870835508914L;
	public MovieDataNoLongerExist(String Message) {
        super(Message);
    }
}