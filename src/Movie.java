import java.util.HashMap;

public class Movie implements Cloneable {
	private String __Name__;
	private HashMap<String, Boolean> __Genre__;
	private int __LengthInMinutes__;
	private HashMap<String, Boolean> __Director__;
	public String UUID; // use for booking.
	public int[] SeatIdx; // use for booking.
    public Movie(String Name, String[] Genre, int LengthInMinutes, String[] Director) {
        this.__Name__ = Name;
        this.__Genre__ = new HashMap<String, Boolean>();
        this.__LengthInMinutes__ = LengthInMinutes;
        this.__Director__ = new HashMap<String, Boolean>();
        for (String MovieType: Genre) {
        	__Genre__.put(MovieType, true);
        }
        for (String DirectorName: Director) {
        	__Director__.put(DirectorName, true);
        }
    }
    public String Name() {
    	return this.__Name__;
    }
    public int LengthInMinutes() {
    	return this.__LengthInMinutes__;
    }
    public String Director() {
    	String[] MovieDirector = this.__Director__.keySet().toArray(new String[this.__Director__.keySet().size()]);
    	return String.join(" and ", MovieDirector);
    }
    public HashMap<String, Boolean> Genres() {
    	return __Genre__;
    }
    @Override
    public String toString() {
    	String[] MovieGenre = this.__Genre__.keySet().toArray(new String[this.__Genre__.keySet().size()]);
    	String[] MovieDirector = this.__Director__.keySet().toArray(new String[this.__Director__.keySet().size()]);
    	return String.format("%s, %s, %d, %s",
    			this.__Name__, String.join("/", MovieGenre), this.__LengthInMinutes__, String.join(" and ", MovieDirector));
    }
    private int[] __SchedulePeriod__;
    public void SetSchedule() {
    	this.__SchedulePeriod__ = null;
    }
    public void SetSchedule(int Start, int Finished) { // <Finished> is approximation (in unit of hour chunk)
    	this.__SchedulePeriod__ = new int[] {Start, Finished};
    }
    public int[] GetSchedule() {
    	return __SchedulePeriod__;
    }
    @Override
    public Movie clone() throws CloneNotSupportedException {
        return (Movie) super.clone();
    }
}