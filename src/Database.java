import java.io.File;
import java.util.ArrayList;

public class Database extends FileIO {
	private ArrayList<?> __Collection__;
	public Database(File Storage, ArrayList<?> Collection) {
		this.__File__ = Storage; // extends, FileIO
		this.__Collection__ = Collection;
	}
	@Override
	protected void UPDATE() {
		// **Note: this method do override data from ArrayList to File as a database.
        String FileContent = "";
        for (int Id = 0; Id < __Collection__.size(); Id++) {
        	Object k = __Collection__.get(Id);
        	if (k instanceof Movie) {
        		Movie v = (Movie) k;
        		FileContent += (v.toString() + "\n");
        	}
        }
        if (FileContent != null) {
        	WRITE(FileContent);
        }
	}
}