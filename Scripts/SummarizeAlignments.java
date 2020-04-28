import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;


public class SummarizeAlignments {

	private Hashtable annotations;
	private Hashtable[] allResults = null;

	public SummarizeAlignments( String[] args ) {

		// Display usage info
		if (args.length < 2) {
			usage();
		}
		
		// Read in the annotations for the hairpins
		readAnnotations( args[0] );
		System.out.println("Read in annotations for " + annotations.size() + " hairpins...");
		
		
		// Now, the hard part - need to read in the .map files and deal with them
		String[] fileNames = args[1].split(",");
		
		System.out.println(fileNames.length + " files to parse...");
		allResults = new Hashtable[ fileNames.length ];
		
		int fNum = 0;
		for (int i = 0; i < fileNames.length; i++ ) {
			File f = new File( fileNames[i] );
			String outFile = f.getAbsolutePath().replace(".map", ".counts");
			String outFile2 = f.getAbsolutePath().replace(".map", ".ambig");

			Hashtable results = readResults( f.getAbsolutePath(), outFile2 );
			
			allResults[ fNum++ ] = writeResults( results, outFile );
		}
		
		printMatrix( allResults, "merged_results.txt", fileNames );

	}
	
	
	public void printMatrix( Hashtable[] results, String outFile, String[] files ) {
		try {
			BufferedWriter out = new BufferedWriter( new FileWriter( new File( outFile )));
			
			// Print out filenames on first row
			out.write("TRC.ID\t" + (String)annotations.get("header"));
			for (int i = 0; i < files.length; i++ ) {
				File f = new File( files[i] );
				String fName = f.getCanonicalPath();
				fName = fName.substring( fName.lastIndexOf("/")+1, fName.lastIndexOf("."));
				out.write( "\t" + fName );
			}
			out.newLine();
			
			annotations.remove("header");
			for (Iterator it = annotations.keySet().iterator(); it.hasNext(); ) {
				String hp = (String)it.next();
				
				out.write( hp + "\t" + (String)annotations.get( hp ) );
				for (int i = 0; i < files.length; i++) {
					Hashtable result = results[i];
					
					if (result.containsKey( hp )) {
						out.write("\t" + ((Integer)result.get(hp)).intValue());
					} else {
						out.write("\t" + 0);
					}
				}
				out.newLine();
			}
			
			
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public Hashtable writeResults( Hashtable results, String outFile ) {
		
		Hashtable h = new Hashtable();
		int failedMatches = 0, trashedReads = 0;
		
		System.out.println("Writing to " + outFile);
		
		try {
			BufferedWriter out = new BufferedWriter( new FileWriter( new File( outFile )));
			
			for (Iterator it = results.keySet().iterator(); it.hasNext(); ) {
				String trcid = (String)it.next();
				Read num = (Read)results.get( trcid );
				
				String[] trcids = trcid.split(";");
				for (int i = 0; i < trcids.length; i++) {
					String clone = trcids[i];
					out.write(clone + "\t" + num.getCount());
					out.newLine();

					if (!h.containsKey(clone))
						h.put( clone, new Integer(num.getCount()) );
					else
						h.put( clone, new Integer( ((Integer)h.get(clone)).intValue() + num.getCount() ));
				}
				

			}
			
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.err.println(failedMatches + " aligned transcripts failed to match idx.");
		//System.err.println("Number of reads trashed due to mismatched alignment idx:  " + trashedReads);
		
		return h;
	}

	public Hashtable readResults( String inFile, String outFile ) {
		
		System.out.println("Reading from " + inFile);
		
		int numReads = 0;
		Hashtable reads = new Hashtable();
		Hashtable results = new Hashtable();
		
		try {
			BufferedWriter out = new BufferedWriter( new FileWriter( new File( outFile )));
			BufferedReader in = new BufferedReader( new FileReader( new File( inFile )));
			String line = null;
			
			while ((line = in.readLine()) != null) {
				
				String[] data = line.split("\t");

				if ( !annotations.containsKey( data[2] )) {
					out.write( line );
					out.newLine();
				}
				
				
				if (!results.containsKey( data[2] ))
					results.put( data[2] , new Read());
				else {
					((Read)results.get( data[2] )).incrCount();
				}
				
					numReads++;
				
			}
			
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Read in " + numReads + " representing " + results.size() + " TRC_IDs...");
		return results;
	}
	
	class Read {
		private int count;
		
		public Read() {
			count = 1;
		}
		
		public int getCount() {
			return count;
		}
		
		public void incrCount() {
			count++;
		}
	}

	
	private void readAnnotations( String inFile ) {
		annotations = new Hashtable();
		
		try {
			BufferedReader in = new BufferedReader( new FileReader( new File( inFile )));
			String line = null;
			
			int lNum = 0;
			while((line = in.readLine()) != null) {
				String[] data = line.split("\t");
				
				if (lNum++ == 0)
					annotations.put("header", line.substring( line.indexOf("\t", 0)+1, line.length()));
				else
					annotations.put(data[0], line.substring( line.indexOf("\t", 0)+1, line.length()));
			}
			
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void usage() {
		System.out.println("");
		System.out.println("Usage:  SummarizeAlignments <annotationsFile> <map1.map,...,mapN.map>");
		System.out.println("        annotationsFile:  TRC_ID assumed to be in first column");
		System.out.println("                          First row is header row");
		System.out.println("");
		System.out.println("        map1.map - Bowtie output files.  ID (col3) assumed to be TRC_ID, column ");
		System.out.println("");
		System.exit(0);
	}
	
	class Sequence {
		private String seq;
		private Vector clone_id;
		private int subpool;
		
		public Sequence( String seq, String clone_id ) {
			this.seq = seq;
			
			this.clone_id = new Vector();
			addCloneIDs( clone_id );
		}

		private void addCloneIDs( String clone_id ) {
			String[] data = clone_id.split(";");
			for (int i = 0; i < data.length; i++)
				this.clone_id.add( data[i] );
		}
		
		public String getSeq() {
			return seq;
		}

		public void addClone_ID( String clone_id ) {
			if (!this.clone_id.contains( clone_id ))
				this.clone_id.add( clone_id );
		}
		
		public String getClone_id() {
			return getCloneIDString();
		}

		private String getCloneIDString() {
			String str = "";
			for (Iterator it = clone_id.iterator(); it.hasNext(); ) {
				str = str.concat( (String)it.next() );
				
				if (it.hasNext())
					str += ";";
			}
			return str;
		}
		
		public int getSubpool() {
			return subpool;
		}
		
		public boolean equals( Object s2 ) {
			if (this.subpool == ((Sequence)s2).getSubpool()) 
				return true;
			else
				return false;
		}
		
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SummarizeAlignments s = new SummarizeAlignments(args);
	}

}
