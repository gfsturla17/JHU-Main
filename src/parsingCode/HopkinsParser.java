package parsingCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.websocket.Session;

import components.Block;
import components.Person;
import components.Rotation;
import components.Service;
import jxl.Workbook;


public class HopkinsParser {


	private ArrayList<String> blockNames;
	private ArrayList<Person> people;
	private ArrayList<Service> services;
	private ArrayList<Rotation> rotations;
	private double[] multipliers;
	public Session session;
	
	private static final int yearlyInternRowInd = 11;
	private static final int yearlySARowInd = 11;
	private static final int yearlyJARowInd = 15;
	private static final int overallReqsInd = 7;
	
	private static final int numMultipliers = 6;
	private static final int multiplierColumn = 2;


	public HopkinsParser(Session session){
		this.session = session;
	}

	public ArrayList<String> getBlockNames(){
		return blockNames;
	}

	public ArrayList<Person> getPeople(){
		return people;
	}

	public ArrayList<Service> getServices(){
		return services;
	}

	public void parseFile(String file) throws IOException{

		XLSParser myParser = new XLSParser(file);	
		Workbook wb = myParser.openWorkbook();

		//Extract the contents of the workbook.
		ArrayList<ArrayList<String>> tbl_BlockRequirements = myParser.extractWorksheetContents(wb.getSheet(2),200,200);
		ArrayList<ArrayList<String>> tbl_ResidentInternInformation = myParser.extractWorksheetContents(wb.getSheet(0),200,200);
		ArrayList<ArrayList<String>> tbl_ResidentRequirements = myParser.extractWorksheetContents(wb.getSheet(1),200,200);
		ArrayList<ArrayList<String>> tbl_MasterSchedule = myParser.extractWorksheetContents(wb.getSheet(3),200,200);


		//Is this a JAR/SAR or intern schedule?
		boolean jarsarSheet = true;
		if (tbl_ResidentInternInformation.get(1).get(1).equals("Year")){
			if (tbl_ResidentInternInformation.get(3).get(1).equals("1")){
				jarsarSheet = false;
			} else {
				jarsarSheet = true;
			}
		} else {
			System.err.println("Unable to find firm constraint column");
		}

		// Get the block names
		blockNames = getBlockNames(jarsarSheet,tbl_BlockRequirements);

		// Create the services, rotations, and blocks
		getServicesAndRotations(jarsarSheet,tbl_BlockRequirements);
		
		// Get the resident requirements
		HashMap<String,HashMap<String, Integer[]>> residentRequirements = getResidentRequirements(jarsarSheet,tbl_ResidentRequirements);

		//Create the list of people to be scheduled.
		people = createPeople(jarsarSheet,tbl_ResidentInternInformation,residentRequirements);

		// Read in Master Schedule
		readInMasterSchedule(tbl_MasterSchedule);
		
		//Get multipliers
		multipliers = parseMultipliers(tbl_ResidentRequirements);

	}

	/**
	 * @author Giovanni
	 * @param residentRequirements
	 * @return
	 * @throws IOException 
	 *
	 * @since May 20, 2016
	 */
	private double[] parseMultipliers(
			ArrayList<ArrayList<String>> tbl_ResidentRequirements) throws IOException {
		double[] multipliers = new double[numMultipliers];
		for (int i = 0; i < numMultipliers; i ++){
			try {
				System.out.println(tbl_ResidentRequirements.get(i).get(multiplierColumn));
				multipliers[i] = Double.parseDouble(tbl_ResidentRequirements.get(i).get(multiplierColumn));
			} catch (Exception e){
				System.err.println("Unable to find multiplier in row " + (i+1) + " column "+ multiplierColumn);
				session.getBasicRemote().sendText("Unable to find multiplier in row " + (i+1) + " column "+ multiplierColumn+"~Error");
//				System.exit(0);
			}
		}
		return multipliers;
	}
	
	public double[] getMultipliers(){
		return multipliers;
	}

	private void readInMasterSchedule(ArrayList<ArrayList<String>>  tbl_MasterSchedule) throws IOException{
		Service specialService = new Service("X");
		Rotation specialRotation = new Rotation("X",specialService);

		for (Person person : people){

			//Find this person in the table
			int rowInd = -1;
			for (int i = 0; i < tbl_MasterSchedule.size(); i ++){
				String name = "";
				try {
					name = tbl_MasterSchedule.get(i).get(0);
				} catch (Exception e){
					System.err.println("Unable to find person named " + person.getName());
					session.getBasicRemote().sendText("An Error occurred~Error");
					System.exit(0);
				}
				if (person.getName().equals(name)){
					rowInd = i;
					break;
				}
			}
			if (rowInd == -1){
				System.err.println("Unable to find person");
				session.getBasicRemote().sendText("An Error occurred~Error");
				System.exit(0);
			}

			//Get the information about the blocks
			int numberOfPreAssignments = 0;
			for (int j = 1; j < tbl_MasterSchedule.get(rowInd).size(); j ++){
				String blockName = tbl_MasterSchedule.get(0).get(j);
				String rotationName = tbl_MasterSchedule.get(rowInd).get(j);

				if (tbl_MasterSchedule.get(rowInd).get(j).equals("x")){
					System.out.println("Lowercase \"x\" detected at row " + (rowInd+1) + " column " + (j+1) +". Please capitalize.");
					session.getBasicRemote().sendText("An Error occurred~Error");
					System.exit(0);
				}


				if (tbl_MasterSchedule.get(rowInd).get(j).equals("")){
					//Need an assignment
				} else if (tbl_MasterSchedule.get(rowInd).get(j).equals("X")){
					//Person was scheduled to something that we should not consider.

					Block specialBlock = new Block(blockName,0,1000,-1,-1,"Any",specialService,specialRotation);

					System.out.println("Pre-Assignment: " + person.getName() + " to " + specialBlock.getBlockName() + " - " + specialBlock.getRotation().getName());
					person.addBlock(specialBlock);
					person.setBlockNotChangeable(specialBlock);
					specialBlock.addPerson(person);

					numberOfPreAssignments++;
				} else {

					//Person was scheduled to something that is relevant. Find the correct block.


					//Find the block with this name and rotation.
					boolean foundBlock = false;
					for (Rotation r : rotations){
						if (r.getName().equals(rotationName)){
							for (Block b : r.getBlockList()){
								if (b.getBlockName().equals(blockName)){

									System.out.println("Pre-Assignment: " + person.getName() + " to " + b.getBlockName() + " - " + b.getRotation().getName());
									person.addBlock(b);
									person.setBlockNotChangeable(b);
									b.addPerson(person);

									foundBlock = true;
									break;
								}
							}
							if (foundBlock){
								break;
							}
						}
					}
					if (!foundBlock){
						//Check to see why typo exists
						for (Rotation r : rotations){
							if (r.getName().equalsIgnoreCase(rotationName)){
								for (Block b : r.getBlockList()){
									if (b.getBlockName().equalsIgnoreCase(blockName)){

										System.out.println("Pre-Assignment: " + person.getName() + " to " + b.getBlockName() + " - " + b.getRotation().getName());
										person.addBlock(b);
										person.setBlockNotChangeable(b);
										b.addPerson(person);

										foundBlock = true;
										break;
									}
								}
								if (foundBlock){
									break;
								}
							}
						}
						if (!foundBlock){
							System.err.println("Unable to find pre-assigned rotation \"" + rotationName + "\" for block " + blockName +" and person " + person.getName()  + " (Possible typo).");
						} else {
							System.err.println("Unable to find pre-assigned rotation \"" + rotationName + "\" for block " + blockName +" and person " + person.getName()  + " (Capitalization Error).");
						}
						session.getBasicRemote().sendText("An Error occurred~Error");
						System.exit(0);

					}

					if (blockName.contains("Vac")){
						//Person already had a vacation scheduled, so ignore their other preferences.
						person.setIgnoreVacationPreference(true);
					}


					numberOfPreAssignments++;
				}
			}

			//Determine how many 
			person.setNumberOfPreAssignments(numberOfPreAssignments);
		}
	}

	private ArrayList<Service> getServicesAndRotations(Boolean jarsarSheet, ArrayList<ArrayList<String>> tbl_BlockRequirements) throws IOException{
		rotations = new ArrayList<Rotation>();
		services = new ArrayList<Service>();
		HashMap<String, Service> serviceNames = new HashMap<String,Service>();
		for (int j = 0; j < 2; j ++){
			/**
			 * First iteration is for the non-holiday information.
			 * Second iteration is for the holiday information.
			 */


			/**
			 * Find the first index for the blocks (it differs between JARS/SARS and interns.
			 * Also, determine whether this is a jar/sar schedule or an intern schedule.
			 */
			int firm_ind = -1;
			int ind = 0;
			if (jarsarSheet){
				if (j == 0){
					firm_ind = 11;
				} else {
					ind = 39;
					firm_ind = 39+8;
				}
			} else {
				if (j == 0){
					firm_ind = 7;
				} else {
					ind = 35;
					firm_ind = 39;
				}
			}

			int firstBlockInd = firm_ind+1;
			int maxBlockInd = firstBlockInd + 26;
			if (j == 1){
				maxBlockInd = firstBlockInd + 2;
			}

			for (int i = 3; i < tbl_BlockRequirements.size(); i++){

				String serviceName = null;
				try {
					serviceName = tbl_BlockRequirements.get(i).get(ind);
				} catch (Exception e){
					//No more services
					break;
				}	
				if (serviceName.length() == 0){
					break;
				}

				Service sv;
				if (serviceNames.keySet().contains(serviceName)){
					//Find the service object for this name;
					sv = serviceNames.get(serviceName);
				} else {
					sv = new Service(serviceName);
					serviceNames.put(serviceName,sv);
					services.add(sv);
				}

				String rotationName = tbl_BlockRequirements.get(i).get(ind+1);
				Rotation rt = new Rotation(rotationName,sv);

				rotations.add(rt);
				sv.addRotation(rt);

				ArrayList<Block> blockList = new ArrayList<Block>();
				//What are the preferences for the block?
				int block_min=-1;
				int block_max=-1;
				String block_type = "";
				int k_max = 1;
				if (jarsarSheet){
					k_max = 3;
				}
				//Only one pair of min/max should be populated.
				for (int k = 0; k < k_max; k ++){
					//Holiday vs. non-holiday ind
					int MinMaxPeopleInd = (k+1)*2;										
					if (j == 1){ 
						if (jarsarSheet){
							MinMaxPeopleInd = (k+1)*2+39;
						} else {
							MinMaxPeopleInd = (k+1)*2+35;
						}
					}


					if (!tbl_BlockRequirements.get(i).get(MinMaxPeopleInd).equals("")){
						block_min = Integer.parseInt(tbl_BlockRequirements.get(i).get(MinMaxPeopleInd));
						block_max = Integer.parseInt(tbl_BlockRequirements.get(i).get(MinMaxPeopleInd+1));
						block_type = tbl_BlockRequirements.get(1).get(MinMaxPeopleInd);
						break;
					}
				}
				if (block_min == -1){
					System.err.println("Unable to parse min people for rotation " + rotationName);
					session.getBasicRemote().sendText("An Error occurred~Error");
					System.exit(0);
					
				}
				if (block_max == -1){
					System.err.println("Unable to parse max people for rotation " + rotationName);
					session.getBasicRemote().sendText("An Error occurred~Error");
					System.exit(0);
				}

				//Get the block-duration preference
				int durationType = -1;
				if (j == 0){
					int numPreferenceTypes = 3;
					for (int k = 0; k < numPreferenceTypes; k ++){
						int offset = 4;
						if (jarsarSheet){
							offset = 8;
						}
						int ind1 = offset+k;

						if (tbl_BlockRequirements.get(i).get(ind1).equals("x")){
							System.out.println("Lowercase \"x\" detected at row " + (i+1) + " column " + (ind1+1) +". Please capitalize.");
							session.getBasicRemote().sendText("An Error occurred~Error");
							System.exit(0);							
						}

						if (tbl_BlockRequirements.get(i).get(ind1).equals("X")){
							if (k == 0){
								durationType = 1;
								break;
							} else if (k == 1){
								durationType = 2;
								break;
							} else if (k == 2){
								durationType = 4;
								break;
							} else {
								System.out.println("Unable to parse the duration preference for rotation " + rotationName);
								session.getBasicRemote().sendText("An Error occurred~Error");
								System.exit(0);
							}
						}
					}
				}

				//Firm Constraint
				int firm_constraint = -1; 
				if (!tbl_BlockRequirements.get(i).get(firm_ind).equals("")){
					firm_constraint = Integer.parseInt(tbl_BlockRequirements.get(i).get(firm_ind));
				}

				Block priorBlock = null;
				for (int k = firstBlockInd; k < maxBlockInd; k ++){

					if (tbl_BlockRequirements.get(i).get(k).equals("x")){
						System.out.println("Lowercase \"x\" detected at row " + (i+1) + " column " + (k+1) +". Please capitalize.");
						session.getBasicRemote().sendText("An Error occurred~Error");
						System.exit(0);
					}


					if (!tbl_BlockRequirements.get(i).get(k).equals("X")){
						//Need to schedule this block.

						//Block Name
						String blockName = tbl_BlockRequirements.get(0).get(k);

						Block block = new Block(blockName,block_min,block_max,durationType,firm_constraint,block_type,sv,rt);
						blockList.add(block);

						if (priorBlock != null){
							block.addPriorBlock(priorBlock);
							priorBlock.addPostBlock(block);
						}

						priorBlock = block;

					}
				}
				rt.addBlockList(blockList);
			}
		}
		return services;
	}

	private HashMap<String,HashMap<String, Integer[]>> getResidentRequirements(boolean jarsarSheet,ArrayList<ArrayList<String>> tbl_ResidentRequirements) throws IOException {

		HashMap<String, HashMap<String, Integer[]>> myMap = new HashMap<String, HashMap<String, Integer[]>>();
		if (jarsarSheet){
			//There should be three sets of requirements.

			//Get overall requirements
			HashMap<String,Integer[]> overallReqs = getThisRequirement(overallReqsInd,tbl_ResidentRequirements);

			//Get 3rd Year Requirements
			HashMap<String,Integer[]> thirdYearReqs = getThisRequirement(yearlySARowInd,tbl_ResidentRequirements);

			//Get 2nd Year Requirements
			HashMap<String,Integer[]> secondYearReqs = getThisRequirement(yearlyJARowInd,tbl_ResidentRequirements);

			myMap.put("Overall",overallReqs);
			myMap.put("3rd Year",thirdYearReqs);
			myMap.put("2nd Year",secondYearReqs);

		} else {


			//Get overall requirements
			HashMap<String,Integer[]> overallReqs = getThisRequirement(overallReqsInd,tbl_ResidentRequirements);

			//Get 2nd Year Requirements
			HashMap<String,Integer[]> internReqs = getThisRequirement(yearlyInternRowInd,tbl_ResidentRequirements);

			myMap.put("Overall",overallReqs);
			myMap.put("Intern",internReqs);

		}

		return myMap;
	}

	public HashMap<String,Integer[]> getThisRequirement(int ind,ArrayList<ArrayList<String>> tbl_ResidentRequirements) throws IOException {
		HashMap<String,Integer[]> reqs = new HashMap<String,Integer[]>();
		for (int j = 2; j < tbl_ResidentRequirements.get(0).size(); j ++){
			if (tbl_ResidentRequirements.get(ind).get(j).equals("")){
				//No more requirements
				break;
			} else {			

				String name = tbl_ResidentRequirements.get(ind).get(j);

				/*
				 * Does this requirement exist as service?
				 */
				boolean found = false;
				for (Service sv : services){
					if (sv.getName().equals(name)){
						found = true;
					}
				}
				if (!found){
					System.err.println("Unable to find any rotation for service \"" + name + "\" on the block requirements worksheet.");
					session.getBasicRemote().sendText("An Error occurred~Error");
					System.exit(0);
				}

				int max = Integer.parseInt(tbl_ResidentRequirements.get(ind+1).get(j));
				int min = Integer.parseInt(tbl_ResidentRequirements.get(ind+2).get(j));
				Integer[] minMax = new Integer[]{min,max};
				reqs.put(name, minMax);
			}
		}
		return reqs;
	}

	public ArrayList<Person> createPeople(Boolean jarsarSheet, ArrayList<ArrayList<String>> tbl_ResidentInternInformation, HashMap<String, HashMap<String, Integer[]>> residentRequirements) throws IOException{
		/**
		 * Create the People.
		 */

		int firstBlockInd = -1;
		for (int j = 0; j < tbl_ResidentInternInformation.get(0).size(); j ++){
			if (!tbl_ResidentInternInformation.get(0).get(j).equals("")){
				firstBlockInd = j;
				break;
			}
		}
		if (firstBlockInd == -1){
			System.err.println("Unable to find the first block");
		}

		//Check vacation preferences
		boolean foundChristmas = false;
		boolean foundNewYears = false;
		for (int j = firstBlockInd; j < tbl_ResidentInternInformation.get(0).size(); j++){
			String blockName = tbl_ResidentInternInformation.get(0).get(j);
			if (blockName.equals("Christmas")){
				foundChristmas = true;
			} else if (blockName.equals("New Years")){
				foundNewYears = true;
			}
		}
		if (!foundChristmas){
			System.err.println("Christmas or New Years not found in the vacation preferences worksheet. Please add.");
			session.getBasicRemote().sendText("An Error occurred~Error");
			System.exit(0);
		} else if (!foundNewYears){
			System.err.println("Christmas or New Years not found in the vacation preferences worksheet. Please add.");
			session.getBasicRemote().sendText("An Error occurred~Error");
			System.exit(0);
		}

		ArrayList<Person> people = new ArrayList<Person>();
		for (int i = 3; i < tbl_ResidentInternInformation.size(); i++){
			String name = null;
			try {
				name = tbl_ResidentInternInformation.get(i).get(0);
			} catch (Exception e){
				//No more residents
				break;
			}
			if (name.equals("")){
				break;
			}

			//Get the year
			int year = Integer.parseInt(tbl_ResidentInternInformation.get(i).get(1));

			//Get the firm
			int firm = 0;
			String firm_String = tbl_ResidentInternInformation.get(i).get(2);
			if (firm_String.length() == 0){
				//No firm assignment - should only be interns.
			} else if (firm_String.length() == 1) {
				firm = Integer.parseInt(firm_String);
			}

			boolean MP_Flag = false;
			String MP_Flag_String = tbl_ResidentInternInformation.get(i).get(3);
			if (MP_Flag_String.contains("X")){
				MP_Flag = true;
			}

			boolean UH_Flag = false;
			String UH_Flag_String = tbl_ResidentInternInformation.get(i).get(4);
			if (UH_Flag_String.contains("X")){
				UH_Flag = true;
			}

			String str = null;
			HashMap<String, Integer[]> yearlyReqs = new HashMap<String,Integer[]>();
			HashMap<String, Integer[]> overallReqs = new HashMap<String,Integer[]>();
			HashMap<String,Integer> overallReqTally = new HashMap<String,Integer>();
			if (jarsarSheet){
				if ((year == 2 || year == 3) && !MP_Flag){
					if (year == 2){
						str = "2nd Year";
					} else if (year == 3) {
						str = "3rd Year";
					}
					//Get the yearly requirements.
					boolean foundYearly = false;
					for (String key : residentRequirements.keySet()){
						if (key.equals(str)){
							yearlyReqs = residentRequirements.get(key);
							foundYearly = true;
							break;
						}
					}
					//Get the overall requirements.
					boolean foundOverall = false;
					for (String key : residentRequirements.keySet()){
						if (key.equals("Overall")){
							overallReqs = residentRequirements.get(key);
							foundOverall = true;
							break;
						}
					}
					if (!foundYearly || !foundOverall){
						System.err.println("Error parsing yearly or overall requirements.");
						session.getBasicRemote().sendText("An Error occurred~Error");
						System.exit(0);
					}
				}

			} else {
				//Get the yearly requirements.
				str = "Intern";
				//Get the yearly requirements.
				boolean foundYearly = false;
				for (String key : residentRequirements.keySet()){
					if (key.equals(str)){
						yearlyReqs = residentRequirements.get(key);
						foundYearly = true;
						break;
					}
				}
				//Get the overall requirements.
				boolean foundOverall = false;
				for (String key : residentRequirements.keySet()){
					if (key.equals("Overall")){
						overallReqs = residentRequirements.get(key);
						foundOverall = true;
						break;
					}
				}
				if (!foundYearly || !foundOverall){
					System.err.println("Error parsing yearly or overall requirements.");
					session.getBasicRemote().sendText("An Error occurred~Error");
					System.exit(0);
				}

			}

			//Get Vacation preferences.
			HashMap<String,Integer> vacationPrefs = new HashMap<String,Integer>();
			for (int j = firstBlockInd; j < tbl_ResidentInternInformation.get(i).size(); j++){

				String blockName = tbl_ResidentInternInformation.get(0).get(j);
				int val = 0;

				//If it is Christmas or New Years, make this bad - we handle those with "off"
				if (blockName.equals("Christmas") || blockName.equals("New Years")){
					if (!tbl_ResidentInternInformation.get(i).get(j).equals("")){
						System.err.println("Vacation preference listed for Christmas or New Years. Please remove.");
						session.getBasicRemote().sendText("An Error occurred~Error");
						System.exit(0);
					}
					val = -1;
				}



				if (tbl_ResidentInternInformation.get(i).get(j).equals("")){
					//No preference
				} else {
					val = (int) Integer.parseInt(tbl_ResidentInternInformation.get(i).get(j));
				}
				vacationPrefs.put(blockName,val);
			}


			if (!MP_Flag){
				for (int j = 5; j < firstBlockInd; j++){
					String reqName = tbl_ResidentInternInformation.get(2).get(j);
					String entry = tbl_ResidentInternInformation.get(i).get(j);
					try {
						int val = (int) Integer.parseInt(entry);
						overallReqTally.put(reqName,val);
					} catch (Exception e){
						System.err.println("Unable to parse entry \""+ entry +"\" for " + name);
						session.getBasicRemote().sendText("An Error occurred~Error");
						System.exit(0);
					}
				}
			}

			//Create Person
			people.add(new Person(name,year,firm,MP_Flag,UH_Flag,yearlyReqs,overallReqs,overallReqTally,vacationPrefs));
		}
		return people;
	}

	public ArrayList<String> getBlockNames(Boolean jarsarSheet, ArrayList<ArrayList<String>> tbl_BlockRequirements) throws IOException{
		//Get the block names
		ArrayList<String> blockNames = new ArrayList<String>();
		for (int j = 0; j < 2; j ++){

			/**
			 * Find the first index for the blocks (it differs between JARS/SARS and interns.
			 * Also, determine whether this is a jar/sar schedule or an intern schedule.
			 */
			int firm_ind = -1;
			if (jarsarSheet){
				if (j == 0){
					firm_ind = 11;
				} else {
					firm_ind = 39+8;
				}
			} else {
				if (j == 0){
					firm_ind = 7;
				} else {
					firm_ind = 39;
				}
			}

			int firstBlockInd = firm_ind+1;
			int maxBlockInd = firstBlockInd + 26;
			if (j == 1){
				maxBlockInd = firstBlockInd + 2;
			}		

			//Get all the block names
			boolean foundChristmas = false;
			boolean foundNewYears = false;
			for (int i = firstBlockInd; i < maxBlockInd; i ++){
				if (!tbl_BlockRequirements.get(0).get(i).equals("")){
					String str = tbl_BlockRequirements.get(0).get(i);

					if (str.equals("Christmas")){
						foundChristmas = true;
					} else if (str.equals("New Years")){
						foundNewYears = true;
					}

					System.out.println(i + ": " + str);
					blockNames.add(str);
				}
			}
			if (!foundChristmas && j == 1){
				System.err.println("Unable to find Christmas on the block requirements worksheet.");
				session.getBasicRemote().sendText("An Error occurred~Error");
				System.exit(0);
			} else if (!foundNewYears && j == 1){
				System.err.println("Unable to find New Years on the block requirements worksheet.");
				session.getBasicRemote().sendText("An Error occurred~Error");
				System.exit(0);
			}
		}
		return blockNames;
	}

}
