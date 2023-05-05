public class Receipt {
	private String __MovieName__;
	private int[] __Seat__;
	private int[] __Schedule__;
	public Receipt(String MovieName, int[] Seat, int Schedule[]) {
		this.__MovieName__ = MovieName;
		this.__Seat__ = Seat;
		this.__Schedule__ = Schedule;
	}
	public String MovieName() {
		return this.__MovieName__;
	}
	public int[] Seat() {
		return this.__Seat__;
	}
	public int[] Schedule() {
		return this.__Schedule__;
	}
}
