import java.io.*;
import java.util.zip.*;
/* Compression / Decompression
 * 5793 bytes: zlib @used
 * 5831 bytes: gzip
 * so, zlib is better for this case.
*/

public abstract class FileIO {
	protected File __File__;
	public String READ(boolean Decompress) { // self
		return READFILE(__File__, Decompress);
	}
	public boolean WRITE(String Content) { // self
		return WRITEFILE(__File__, Content);
	}
	public String READFILE(File v, boolean Decompress) {
		// Success: String
		// Failed: null
		try { //::Input
			FileInputStream FIN = new FileInputStream(v);
	        byte[] Content = new byte[FIN.available()];
	        FIN.read(Content);
	        FIN.close();
			if (Decompress) {
				ByteArrayInputStream bais = new ByteArrayInputStream(Content);
		        Inflater inflater = new Inflater(true);
		        InflaterInputStream iis = new InflaterInputStream(bais, inflater);
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        byte[] buffer = new byte[1024];
		        int len;
		        while ((len = iis.read(buffer)) > 0) {
		            baos.write(buffer, 0, len);
		        }
		        baos.close();
		        return new String(baos.toByteArray());
			} else {
				return Content.length > 0 ? new String(Content) : null;
			}
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		return null; // Failed
	}
	public boolean WRITEFILE(File v, String Content) {
		// Success: true
		// Failed: false
		try (FileOutputStream FOS = new FileOutputStream(v)) { //::Output
			if(Content == null) {
				return true;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
	        DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
	        dos.write(Content.getBytes());
	        dos.close();
			FOS.write(baos.toByteArray());
			return true;
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		return false; // Failed
	}
	protected abstract void UPDATE(); /* specific method: 
		because for each database has their own collection so need to be override by own collection. */
}