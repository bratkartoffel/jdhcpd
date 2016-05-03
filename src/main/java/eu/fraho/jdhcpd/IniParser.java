package eu.fraho.jdhcpd;

/*
This file is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This file is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this file. If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Simple class to read and write ini-files.<br>
 * These are pretty simple textfiles which can store informations.
 * 
 * @author sfrankenberger
 */
public class IniParser {
	/**
	 * Inner class representing a section inside an ini file.
	 */
	private class IniSector {
		/**
		 * name of this sector
		 */
		private String name = "";
		
		/**
		 * the fields which this sector contains
		 */
		private HashMap<String, String> fields = null;
		
		/**
		 * Creates a new sector with the given name. If name is 
		 * <code>null</code>, the name is set to {@link IniParser#UNDEFINED_SECTION}.
		 * 
		 * @param name the name of this sector
		 */
		public IniSector(String name) {
			if (name == null || name.equals("")) {
				this.name = UNDEFINED_SECTION;
			}
			else {
				this.name = name;
			}
			
			fields = new HashMap<String, String>();
		}
		
		@Override
		protected void finalize() throws Throwable {
			fields.clear();
			fields = null;
		}
		
		/**
		 * @param name the setting to get
		 * @return the value of the given setting. Returns <code>null</code>
		 *         if the setting is not set.
		 */
		public String get(String name) {
			if (name == null || name.equals("")) {
				return null;
			}
			
			if (ignore_case) {
				name = name.toLowerCase();
			}
			
			return fields.get(name);
		}
		
		/**
		 * Sets the given setting to a specific value.
		 * 
		 * @param name the setting to set
		 * @param value the value to use
		 */
		public void set(String name, Object value) {
			if (name == null || name.equals("")) {
				return;
			}
			if (value == null) {
				value = "";
			}
			
			if (ignore_case) {
				name = name.toLowerCase();
			}
			
			synchronized (this) {
				fields.put(name, value.toString());
			}
		}
		
		@Override
		public String toString() {
			synchronized (this) {
				StringBuilder back = new StringBuilder();
				
				back.append("[" + name + "]");
				for (String k : fields.keySet()) {
					back.append("\n" + k + "=" + fields.get(k));
				}
				back.append("\n\n");
				
				return back.toString();
			}
		}
	}
	
	/**
	 * The name of an undefined section. Should not be used.
	 */
	public static final String UNDEFINED_SECTION = "Undefined";
	
	/**
	 * The sectors of this ini file containing the 
	 * field &lt;-&gt; value pairs.<br>
	 * First parameter defines the unique name of this section,
	 * the second the sector itself.
	 */
	private HashMap<String, IniSector> sectors = null;
	
	/**
	 * Should the sector names be case insensitive?
	 */
	private boolean ignore_case = true;
	
	/**
	 * was anything changed since last write?
	 */
	private boolean dirty = false;
	
	/**
	 * Creates an empty parser which is case insensitive.
	 */
	public IniParser() {
		this(true);
	}
	
	/**
	 * Creates an empty ini parse with the given case
	 * sensitivity.
	 * 
	 * @param ignore_case should the spelling be ignored?
	 */
	public IniParser(boolean ignore_case) {
		sectors = new HashMap<String, IniSector>();
		this.ignore_case = ignore_case;
	}
	
	/**
	 * Parses the given file and initializes this object.
	 * 
	 * @param f the file to parse
	 * @throws IOException if anything goes wrong while reading
	 *         the file.
	 */
	public IniParser(File f) throws IOException {
		this(f, true);
	}

	/**
	 * Parses the given file and initializes this object.
	 * 
	 * @param f the file to parse
	 * @param ignore_case should the spelling be ignored?
	 * @throws IOException if anything goes wrong while reading
	 *         the file.
	 */
	public IniParser(File f, boolean ignore_case) throws IOException {
		this(ignore_case);
		if (!f.exists()) {
			f.createNewFile();
		}
		
		parse(Tools.readTextFile(f));
	}
	
	/**
	 * Returns the value of the field identified by the section name
	 * and the fieldname as an integer. Returns null if the field
	 * doesnt exist or is not a valid integer.
	 * 
	 * @param section the section to use
	 * @param name the field to get
	 * @return an integer representing the field or <code>null</code>
	 *         if the field is not set or is not an integer.
	 */
	public Integer getInt(String section, String name) {
		try {
			String n = getString(section, name);
			
			if (n != null) {
				return Integer.parseInt(n);
			}
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Returns the value of the field identified by the section name
	 * and the fieldname as an integer. If the field doesnt exist or is an
	 * invalid integer, the field is set to the given value.
	 * 
	 * @param section the section to use
	 * @param name the field to get
	 * @param default_value the value to set if not set or invalid
	 * @return an integer representing the field
	 */
	public Integer getInt(String section, String name, int default_value) {
		Integer n = getInt(section, name);
		
		if (n == null) {
			set(section, name, String.valueOf(default_value));
			return default_value;
		}
		
		return n;
	}
	
	public String getString(String section, String name) {
		if (section == null || section == "") {
			section = UNDEFINED_SECTION;
		}
		
		if (ignore_case) {
			section = section.toLowerCase();
		}
		
		IniSector s = sectors.get(section);
		
		if (s == null) {
			return null;
		}
		
		return s.get(name);
	}
	
	public String getString(String section, String name, String default_value) {
		String s = getString(section, name);
		
		if (s == null) {
			set(section, name, default_value);
			return default_value;
		}
		
		return s;
	}
	
	private void parse(String text) {
		String[] lines = text.split("\r\n|\r|\n");
		String aktSektion = null;
		
		for (String line : lines) {
			if (line.startsWith("#") || line.length() < 3) {
				continue;
			}
			
			if (line.startsWith("[") && line.endsWith("]")) {
				aktSektion = line.substring(1, line.length() - 1);
				
				continue;
			}
			
			if (line.indexOf("=") == -1) {
				continue;
			}
			
			if (aktSektion != null) {
				String[] parts = line.split("=", 2);
				set(aktSektion, parts[0], parts[1]);
			}
		}
		
		dirty = false;
		return;
	}
	
	public void save(File f) throws IOException {
		if (!dirty)
			return;
		
		PrintWriter pw = null;
		IOException ex = null;
		try {
			pw = new PrintWriter(new FileOutputStream(f));
			
			pw.print(toString());
			pw.flush();
			pw.close();
		}
		catch (IOException e) {
			ex = e;
		}
		finally {
			Tools.close(pw);
		}
		
		if (ex != null) {
			throw ex;
		}
	}
	
	public void set(String section, String name, String value) {
		dirty = true;
		
		if (section == null || section.equals("")) {
			section = UNDEFINED_SECTION;
		}
		
		if (ignore_case) {
			section = section.toLowerCase();
		}
		
		IniSector akt = sectors.get(section);
		
		if (akt != null) {
			akt.set(name, value);
		}
		else {
			akt = new IniSector(section);
			akt.set(name, value);
			sectors.put(section, akt);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder back = new StringBuilder();
		
		synchronized (sectors) {
			for (IniSector s : sectors.values()) {
				back.append(s.toString());
			}
		}
		
		return back.toString();
	}
}
