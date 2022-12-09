package ru.sao.solar.coronaljets.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * //https://poi.apache.org/components/spreadsheet/quick-guide.html
 */

public class Excel {

	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * read data by columns
	 * first row is a header
	 */
	
	public static LinkedHashMap<String, String[]> readCSV(File file, String csvDelimiter) {
		
		LinkedHashMap<String, String[]> map = new LinkedHashMap<String, String[]>();
		
	    String line;                            // To hold each valid data line.
	    String[] columnNames = new String[0];   // To hold Header names.
	    int dataLineCount = 0;                  // Count the file lines.
	    StringBuilder sb = new StringBuilder(); // Used to build the output String.
	    String ls = System.lineSeparator();     // Use System Line Seperator for output.
	    
	    List<String> valuesList = new ArrayList<String>();

	    // 'Try With Resources' to auto-close the reader
	    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
	        while ((line = br.readLine()) != null) {
	            // Skip Blank Lines (if any).
	            if (line.trim().equals("")) {
	                continue;
	            }
	            dataLineCount++;
	            // Deal with the Header Line. Line 1 in most CSV files is the Header Line.
	            if (dataLineCount == 1) {
	                /* The Regular Expression used in the String#split()
	                   method handles any delimiter/spacing situation.*/
	                columnNames = line.split("\\s{0,}" + csvDelimiter + "\\s{0,}");
	                continue;   // Don't process this line anymore. Continue loop.
	            }
	            // Split the file data line into its respective columnar slot.
	            String[] lineParts = line.split("\\s{0,}" + csvDelimiter + "\\s{0,}");
	            /* Iterate through the Column Names and buld a String
	               using the column names and its' respective data along
	               with a line break after each Column/Data line.     */
	            
	            for (int i = 0; i < columnNames.length; i++) {
	                sb.append(columnNames[i]).append(": ").append(lineParts[i]).append(ls);
	                //headerList.add(columnNames[i]);
	                valuesList.add(lineParts[i]);
	            }
	            // Display the data record in Console.
	            //System.out.println(sb.toString());  
	            /* Clear the StringBuilder object to prepare for 
	               a new string creation.     */
	            sb.delete(0, sb.capacity());        
	        }
	    }
	    // Trap these Exceptions
	    catch (FileNotFoundException ex) {
	        System.err.println(ex.getMessage());
	    }
	    catch (IOException ex) {
	        System.err.println(ex.getMessage());
	    }
	    
	    dataLineCount -= 1;
	    String[] values = valuesList.toArray(new String[0]);
	    
	    for (int i=0; i<columnNames.length; i++) {
	    	String key = columnNames[i];
    		String[] valuesCol = new String[dataLineCount];
    		
	    	for (int j=0; j<dataLineCount; j++) {
	    		valuesCol[j] = values[i+columnNames.length*j];
	    	}
	    	if (key != null) {
    			map.put(key, valuesCol);
    		}
	    }		
	    return map;
	}
	
	public static LinkedHashMap<String, String[]> readXLSX(File file) {
		
		if(!file.exists() || file.isDirectory()) {
			logger.error("Error: ", new Exception("File '" + file.getAbsolutePath() +"' not found"));
			throw new RuntimeException();
		}
		
		LinkedHashMap<String, String[]> map = new LinkedHashMap<String, String[]>();

		//an old way
		/*InputStream inputStream = null;
		Workbook workbook = null;
		try {
			inputStream = new FileInputStream(file);
			//inputStream = new FileInputStream(new File(pathFilename));
			inputStream = MethodHandles.lookup().lookupClass()
			.getResourceAsStream(pathFilename);
			workbook = new XSSFWorkbook(inputStream);
		
			inputStream.close();
			workbook.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}*/
		
		Workbook workbook = null;
		try {
			workbook = WorkbookFactory.create(file);
		} catch (EncryptedDocumentException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		Sheet sheet = workbook.getSheetAt(0);
		//Iterator<Row> rowIter = sheet.iterator();

		int numberOfColumns = sheet.getRow(0).getLastCellNum();

		for (int i = 0; i < numberOfColumns; i++) {
			List<String> valuesList = new ArrayList<String>();
			String key = null;
			for (Row r : sheet) {
				Cell cell = r.getCell(i);
				if (cell != null) {

					if (cell.getRowIndex() == 0) {
						if (cell.getCellType() == CellType.STRING) {
							key = cell.getStringCellValue();
						} else if (cell.getCellType() == CellType.NUMERIC) {
							key = Double.toString(cell.getNumericCellValue());
						}
					} else {

						switch (cell.getCellType()) {
						case STRING:
							Hyperlink link = cell.getHyperlink();
							if (link != null) {
								//System.out.println(link.getAddress());
								valuesList.add(link.getAddress());
							} else {
								//System.out.println(cell.getRichStringCellValue().getString());
								valuesList.add(cell.getStringCellValue());
							}

							break;
						case NUMERIC:
							if (DateUtil.isCellDateFormatted(cell)) {
								//System.out.println(cell.getDateCellValue());
								valuesList.add(cell.getDateCellValue().toString());
							} else {
								//System.out.println(cell.getNumericCellValue());
								valuesList.add(String.valueOf(cell.getNumericCellValue()));
							}
							break;
						case BOOLEAN:
							//System.out.println(cell.getBooleanCellValue());
							valuesList.add(String.valueOf(cell.getBooleanCellValue()));
							break;
						case FORMULA:
							//System.out.println(cell.getCellFormula());
							valuesList.add(String.valueOf(cell.getCellFormula()));
							break;
						case BLANK:
							//System.out.println();
							valuesList.add("");
							break;
						default:
							//System.out.println();
							valuesList.add(cell.getStringCellValue());
							break;
						}

						/*if (cell.getCellType() == CellType.NUMERIC) {
							valuesList.add(cell.getNumericCellValue());
						}*/
					}
				}
			}

			String[] values = valuesList.toArray(new String[0]);
			if (key != null) {
				map.put(key, values);
			}

		}
		return map;
	}
	
	//DataFormatter formatter = new DataFormatter();
	// Get iterator to all the rows in current sheet
			/*Iterator<Row> rowIterator = mySheet.iterator();
			
			// Traversing over each row of XLSX file
			while (rowIterator.hasNext()) {
			    Row row = rowIterator.next();
			
			    // For each row, iterate through each columns
			    Iterator<Cell> cellIterator = row.cellIterator();
			    while (cellIterator.hasNext()) {
			
			        Cell cell = cellIterator.next();
			
					switch (cell.getCellType()) {
					case Cell.CELL_TYPE_STRING:
					    System.out.print(cell.getStringCellValue() + "\t");
					    break;
					case Cell.CELL_TYPE_NUMERIC:
					    System.out.print(cell.getNumericCellValue() + "\t");
					    break;
					case Cell.CELL_TYPE_BOOLEAN:
					    System.out.print(cell.getBooleanCellValue() + "\t");
					    break;
					default :
					
					}
			    }
			    System.out.println("");
			}*/
	
	/*Workbook wb = null;
	try {
		wb = WorkbookFactory.create(file);
	} catch (EncryptedDocumentException e) {
		logger.error(e.getMessage());
	} catch (IOException e) {
		logger.error(e.getMessage());
	}
	
	Sheet sheet = wb.getSheetAt(0);
	
	for (Row row : sheet) {
		for (Cell cell : row) {
			
			if (row.getRowNum() >0 ) {
				
				//CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());
		        //System.out.print(cellRef.formatAsString());
		        //System.out.print(" - ");
		        // get the text that appears in the cell by getting the cell value and applying any data formats (Date, 0.00, 1.23e9, $1.23, etc)
		        //String text = formatter.formatCellValue(cell);
	
	
		        //System.out.println(text);
		        
				// Alternatively, get the value and format it yourself
		        switch (cell.getCellType()) {
		            case STRING:
		            	Hyperlink link = cell.getHyperlink();
		            	if(link != null){
		            	    //System.out.println(link.getAddress());
		            	    dataMap.put(link.getAddress(), new ArrayList<>());
		            	} else {
		            		//System.out.println(cell.getRichStringCellValue().getString());
		            		dataMap.put(cell.getStringCellValue(), new ArrayList<>());
		            	}
		            	
		                break;
		            case NUMERIC:
						if (DateUtil.isCellDateFormatted(cell)) {
						    //System.out.println(cell.getDateCellValue());
						    dataMap.put(cell.getDateCellValue().toString(), new ArrayList<>());  
						} else {
						    //System.out.println(cell.getNumericCellValue());
						    dataMap.put(String.valueOf(cell.getNumericCellValue()), new ArrayList<>());  
						}
		                break;
		            case BOOLEAN:
		                //System.out.println(cell.getBooleanCellValue());
		                dataMap.put(String.valueOf(cell.getBooleanCellValue()), new ArrayList<>());
		                break;
		            case FORMULA:
		                //System.out.println(cell.getCellFormula());
		            	dataMap.put(String.valueOf(cell.getCellFormula()), new ArrayList<>());
		                break;
		            case BLANK:
		                //System.out.println();
		            	dataMap.put("", new ArrayList<>());
		                break;
		            default:
		                //System.out.println();
		                dataMap.put(cell.getStringCellValue(), new ArrayList<>());
		                break;
		        }
			}
			
			
		}
	}*/

}
