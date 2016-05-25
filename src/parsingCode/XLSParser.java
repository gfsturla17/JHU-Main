package parsingCode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
//import jxl.write.Label;
//import jxl.write.WritableSheet;
//import jxl.write.WritableWorkbook;
//import jxl.write.WriteException;
//import jxl.write.biff.RowsExceededException;

/**
 * The class is designed to parse the data from the input excel file.
 * ONLY reads .xls files (NOT .xlsx)
 *
 * Adapted from Giancarlo's Boeing Parser
 */

public class XLSParser {

	private String input_file;

	public XLSParser(String xlsFile){
		this.input_file = xlsFile;
	}

	/**
	create handle to open notebook
	 **/
	public Workbook openWorkbook(){
		Workbook workbook = null;
		try{
			File file = new File(input_file);
			System.out.println("Attempting to open " + input_file);
			workbook = Workbook.getWorkbook(file);
			System.out.println("Successfully opened " + input_file);
		} catch (BiffException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return workbook;
	}

	
	/**
	 * Read entire worksheet
	 */
	public ArrayList<ArrayList<String>> extractWorksheetContents(Sheet sheet, int maxRows, int maxCols){
		ArrayList<ArrayList<String>> tbl = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < maxRows; i ++){
			ArrayList<String> row = new ArrayList<String>();
			for (int j = 0; j < maxCols; j ++){
				try {
					String cell = sheet.getCell(j,i).getContents();
					row.add(cell);
					System.out.print(cell);
				} catch (Exception e){
					break;
				}
			}
			tbl.add(row);
			System.out.println("");
		}
		return tbl;
	}

	/**
	reads contents until an empty row is read
	 **/
	private ArrayList<String[]> extractTable(Sheet sheet, int start_row, int start_col, int num_columns){
		ArrayList<String[]> table = new ArrayList<String[]>();

		outerloop:
			for(int r = start_row; r < sheet.getRows(); r++){
				String[] info = new String[num_columns];
				for(int c = 0; c < num_columns; c++){
					Cell cell = sheet.getCell(c+start_col, r);
					String content = cell.getContents();
					if(content.isEmpty()){
						break outerloop;
					}
					info[c] = content;
				}
				table.add(info);
			}

		return table;
	}


	public static void printArray(ArrayList<String[]> array){
		String output = "";
		output += "[";
		for(String[] info : array){
			output += "[";
			for(String data : info){
				output += "\"" + data + "\",";
			}
			output = output.substring(0, output.length()-1);
			output += "],";
		}
		output = output.substring(0, output.length()-1);
		output += "]";
		System.out.println(output);
	}




	public static void main(String[] args){

		XLSParser parser = new XLSParser("./test.xls");

		Workbook workbook = parser.openWorkbook();

		//hello world from first sheet
		Sheet firstSheet = workbook.getSheet(0);
		String h = firstSheet.getCell(1, 1).getContents();  		//getCell (col,row)
		String w= firstSheet.getCell(1, 2).getContents();
		String e= firstSheet.getCell(2, 2).getContents();
		System.out.println(h+w+e);


		//read table from 2nd sheet
		Sheet secondSheet = workbook.getSheet(1);
		ArrayList<String[]> table = parser.extractTable(secondSheet, 6, 0, 5);
		XLSParser.printArray(table);

		workbook.close();
	}
}

///**
//sample writing Excel
// **/
//public void writeRandomSealantOrdering(){
//
//	try {
//		File file = new File("src/excel/input_parameters.xls");
//		Workbook workbook = Workbook.getWorkbook(file);
//		WritableWorkbook writable = Workbook.createWorkbook(file, workbook);
//		WritableSheet sheet = writable.getSheet(1);
//		
//		int col = SEALANT_ORDER_INDEX[0];
//		int start_row = SEALANT_ORDER_INDEX[1];
//
//		for(int i = 0; i < ordering.size() - 1; i++){
//			String releasing = ordering.get(i);
//			String constrained = ordering.get(i+1);
//
//			Label releasing_label = new Label(col, start_row + i, releasing);
//			sheet.addCell(releasing_label);
//
//			Label constrained_label = new Label(col+1, start_row + i, constrained);
//			sheet.addCell(constrained_label);
//		}
//		
//
//		writable.write();
//		writable.close();
//	} catch (BiffException e) {
//		e.printStackTrace();
//	} catch (IOException e) {
//		e.printStackTrace();
//	} catch (RowsExceededException e) {
//		e.printStackTrace();
//	} catch (WriteException e) {
//		e.printStackTrace();
//	}
//
//}