public class Item {
	private String __Name__;
	private int __Price__;
	public int Amount;
	public Item(String ItemName, int ItemPrice) {
		this.__Name__ = ItemName;
		this.__Price__ = ItemPrice;
	}
	public String Name() {
		return this.__Name__;
	}
	public int Price() {
		return this.__Price__;
	}
}
