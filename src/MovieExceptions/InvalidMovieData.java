package MovieExceptions;
public class InvalidMovieData extends Exception {
	private static final long serialVersionUID = -437301955317525160L;
	public InvalidMovieData(String Message) {
        super(Message);
    }
}