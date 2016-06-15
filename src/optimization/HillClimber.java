package optimization;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import org.apache.commons.io.FileUtils;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

import services.MailService;
import components.Block;
import components.Person;
import components.Rotation;
import components.Service;


public class HillClimber {
	private Session session;
	private ArrayList<Service> services;
	private ArrayList<Person> people;
	private ArrayList<String> blockNames;
	private double PersonConstraintMultiplier;
	private double blockMinMaxPeopleConstraintMultiplier;
	private double blockFirmConstraintMultiplier;
	private double blockYearConstraintMultiplier;
	private double blockDurationMultiplier;
	private double VacationPreferenceMultiplier;
	private double preAssignmentMultiplier;
	
	private double randomizerMultiplier;
	private boolean fastSchedule;
	private double bestScheduleSoFar;
	
	private ArrayList<Double> scoreList;
	
	private static final int maxNumAssignments = 28;
	
	private String fileName;
	private static final String directory = "/home/ubuntu/Tercio/JHU/";

	private ArrayList<Block> grandBlockList;
	private HashMap<String,ArrayList<Block>> blockLookup;

	public HillClimber(ArrayList<Service> services, ArrayList<Person> people, ArrayList<String> blockNames, double[] multipliers, String fileName, boolean fastSchedule, double bestScheduleSoFar, Session session, ArrayList<Double> scoreList){
		this.session = session;
		this.services = services;
		this.people = people;
		this.blockNames = blockNames;
		this.fileName = fileName;

		blockMinMaxPeopleConstraintMultiplier	= multipliers[0];
		PersonConstraintMultiplier 				= multipliers[1];
		blockFirmConstraintMultiplier 			= multipliers[2];
		blockYearConstraintMultiplier 			= multipliers[3];
		blockDurationMultiplier					= multipliers[4];
		VacationPreferenceMultiplier 			= multipliers[5];
		
		/**
		 * When generating a quick and dirty schedule, the user will likely want to 
		 * compare small differences as a function of the multiplier values above. 
		 * As such, we don't want to randomly initialize anything. Rather, we want
		 * the scheduler to be deterministic.
		 */
		this.fastSchedule = fastSchedule;
		if (fastSchedule){
			randomizerMultiplier 				= 0;
		} else {
			randomizerMultiplier 				= 10;
		}
		this.bestScheduleSoFar = bestScheduleSoFar;

		//Get all of the blocks.
		grandBlockList = new ArrayList<Block>();
		ArrayList<Rotation> grandRotationList = new ArrayList<Rotation>();
		for (int i = 0; i < services.size(); i++){
			ArrayList<Rotation> rotations = services.get(i).getRotations();
			for (int j = 0; j < rotations.size(); j ++){
				grandRotationList.add(rotations.get(j));
				ArrayList<Block> blockList = rotations.get(j).getBlockList();
				for (int k = 0; k < blockList.size(); k++){
					grandBlockList.add(blockList.get(k));
				}
			}
		}
		//Order the grand block list according to the order in blockNames
		ArrayList<Block> newBlockList = new ArrayList<Block>();
		for (String name : blockNames){
			for (Block block : grandBlockList){
				if (block.getBlockName().equals(name)){
					newBlockList.add(block);
				}
			}
		}
		if (newBlockList.size() != grandBlockList.size()){
			System.err.println("Error Sorting");
			System.exit(0);
		}
		grandBlockList = newBlockList;

		blockLookup = new HashMap<String,ArrayList<Block>>();
		for (String blockName : blockNames){
			ArrayList<Block> blockList = new ArrayList<Block>();
			for (Block block : grandBlockList){
				if (block.getBlockName().equalsIgnoreCase(blockName)){
					//The block names are the same
					blockList.add(block);
				}
			}
			blockLookup.put(blockName, blockList);
		}
		
		
		this.scoreList = scoreList;
	}

	public ArrayList<Double> solve() throws IOException, java.text.ParseException {

		/**
		 * TODO: Progress bars for these steps?
		 */
		// Step 1 of 4
		initialSolution();		//Generate initial solution
		session.getBasicRemote().sendText("Done Parsing~Parsing");
		session.getBasicRemote().sendText("Generate Intial Solution~25");
		
		// Step 2 of 4
		twoByOneOptAcrossBlocks();		//Perform 2-opt (2 people x 1 Blocks)
		session.getBasicRemote().sendText("Perform 2-opt (2 people x 1 Blocks)~50");

		// Step 3 of 4
		threeByOneOptAcrossBlocks();	//Perform 3-opt (3 people x 1 Blocks)
		session.getBasicRemote().sendText("Perform 3-opt (3 people x 1 Blocks)~75");

		// Step 4 of 4
		twoByTwoOptAcrossBlocks();		//Perform 2-opt (2 people x 2 Blocks)


		/////////////////////////////////////////////////////////////////////////////////
		//////////               PRINT OUT FINAL SCHEDULE                  //////////////
		/////////////////////////////////////////////////////////////////////////////////
		
		printSchedule("(Schedule Final)");
		return scoreList;
	}
	
	private double getOverallScore(){
		double personalViolationsCounter = (double) getNumPersonalViolations();
		double blockYearViolationsCounter = (double) getNumBlockYearViolations();
		double blockMinMaxViolationsCounter = (double) getNumBlockMinMaxViolations();
		double blockDurationViolationsCounter = (double) getNumBlockDurationViolations();
		//Get Vacation Score
		double vacScore = 0;
		for (Person person : people){
			vacScore += person.getTotalVacationScore();
		}

		//Get Firm Constraint
		//How much does this respect the firm constraint?
		double firmConstraintPoints = 0;
		for (Block block : grandBlockList){
			if (block.getFirmConstraint() == -1){
				firmConstraintPoints +=  0;	//Firm constraint inactive
			} else {
				for (Person person : block.getPeople()){
					if (block.getFirmConstraint() != person.getFirm()) {
						firmConstraintPoints += -1;	//Firm constraint broken
					} else {
						firmConstraintPoints +=  0;	//Firm constraint satisfied.
					}
				}
			}
		}

		vacScore = vacScore*VacationPreferenceMultiplier;
		personalViolationsCounter = personalViolationsCounter*PersonConstraintMultiplier;
		blockDurationViolationsCounter = blockDurationViolationsCounter*blockDurationMultiplier;
		blockYearViolationsCounter = blockYearViolationsCounter*blockYearConstraintMultiplier;
		blockMinMaxViolationsCounter = blockMinMaxViolationsCounter*blockMinMaxPeopleConstraintMultiplier;
		firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;

		double score = -personalViolationsCounter-blockYearViolationsCounter-blockMinMaxViolationsCounter
				-blockDurationViolationsCounter+vacScore+firmConstraintPoints;
		System.out.println("Score:  " + score);
		return score;
	}

	private void initialSolution() throws IOException, java.text.ParseException {


		/////////////////////////////////////////////////////////////////////////////
		/////               Perform the initial assignment.                     /////
		/////////////////////////////////////////////////////////////////////////////
		System.out.println("Optimizing...");
		while (true){
			double max_score = Double.NEGATIVE_INFINITY;
			int bestBlock = -1;
			int bestPerson = -1;
			for (int i = 0; i < grandBlockList.size(); i++){
				for (int j = 0; j < people.size(); j ++){

					Person person = people.get(j);
					Block block = grandBlockList.get(i);

					if (person.getSchedule().containsKey(block.getBlockName())){
						//Person is already scheduled for this two-week period.
						continue;
					}

					double score = getScore(block,person);
					if  (fastSchedule){
						score += randomizerMultiplier*person.getRandomizer();
					}

					//Find the assignment that has the biggest benefit.
					if (score > max_score){
						max_score = score;
						bestBlock = i;
						bestPerson = j;
					}
				}
			}

			if (max_score > Double.NEGATIVE_INFINITY){
				//Make the assignment
				Block block = grandBlockList.get(bestBlock);
				Person person = people.get(bestPerson);

				String str = "Assigning " + person.getName() + " to " + block.getRotation().getName() + " " + block.getBlockName();
				System.out.println(str);

				block.addPerson(person);
				person.addBlock(block);


//				tryAdditionalAssignment(person,block);
					
			} else {
				break;
			}	
		}

		System.out.print("Printing Schedule...");
		printSchedule("(Initial Schedule)");
		System.out.println("Done");

	}
	
	public void tryAdditionalAssignment(Person person, Block block){
		/**
		 * Now that we've added this person and this block, see if we should add this same person to
		 * the next block for this service/rotation.
		 */
		if (block.getDurationType() == -1){
			return;
		}
		if (block.getDurationType() >= getNumberInARow(person,block)){
			return;
		}

		Rotation rt = block.getRotation();

		//What is the next block?
		String nextBlockName = null;
		for (int i = 0; i < blockNames.size()-1; i ++){
			if (blockNames.get(i).equals(block.getBlockName())){
				nextBlockName = blockNames.get(i+1);
				break;
			}
		}
		if (nextBlockName == null){
			return;
		}
		if (person.isScheduled(nextBlockName)){
			return;
		}

		Block nextBlock = null;
		for (Block b : rt.getBlockList()){
			if (b.getBlockName().equals(nextBlockName)){
				nextBlock = b;
				break;
			}
		}
		if (nextBlock == null){
			return;
		}
		if (!person.isChangeable(nextBlock)){
			return;
		}
		

		double reqScore_initial = person.getNumYearlyViolations() + person.getNumOverallViolations();
		person.addBlock(nextBlock);
		double reqScore_final = person.getNumYearlyViolations() + person.getNumOverallViolations();
		person.removeBlock(nextBlock);

		int numPeople = nextBlock.getPeople().size();
		if (numPeople >= nextBlock.getBlockMax()){			//This block has too many people
			return;
		} 

		if (reqScore_initial <= reqScore_final){
			nextBlock.addPerson(person);
			person.addBlock(nextBlock);
			String str = "Additionally Assigning " + person.getName() + " to " + nextBlock.getRotation().getName() + " " + nextBlock.getBlockName();
			System.out.println(str);
			tryAdditionalAssignment(person, nextBlock);
		}
	}
	
	public double getScore(Block block, Person person){

		//How much does this help the block?
		double blockYearPoints = 0;
		if ((block.getBlockType().equalsIgnoreCase("2nd Year") && person.getYear() != 2) ||
				(block.getBlockType().equalsIgnoreCase("3rd Year") && person.getYear() != 3)){
			if (person.getYear() != 4){
				blockYearPoints = -1;
			}
		}

		double blockMinMaxPoints = 0;
		int numPeople = block.getPeople().size();
		if (numPeople >= block.getBlockMax()){			//This block has too many people
			blockMinMaxPoints = block.getBlockMax()-numPeople-1;
		} else if (numPeople < block.getBlockMin()){	//This block doesn't have enough people
			blockMinMaxPoints = block.getBlockMin()-numPeople;
		} else {
			blockMinMaxPoints = 0;
		}


		//How much does this respect the firm constraint?
		double firmConstraintPoints = 0;
		if (block.getFirmConstraint() == -1){
			firmConstraintPoints =  0;	//Firm constraint inactive
		} else if (block.getFirmConstraint() != person.getFirm()) {
			firmConstraintPoints = -1;	//Firm constraint broken
		} else {
			firmConstraintPoints =  0;	//Firm constraint satisfied.
		}

		//Are we switching?
		double durationPoints = 0;
		Block priorBlock = block.getPriorBlock();
		if (priorBlock != null){
			if (!priorBlock.getRotation().equals(block.getRotation())){
				//Not the same rotation
				System.out.println("Will this happen?");
			} else if (!priorBlock.getPeople().contains(person)){
				//Same rotation, but this person isn't in the previous block.
				durationPoints = -1;
			} else {
				//Same rotation, and this person is in the previous block.
				int numInARow = getNumberInARow(person,block);
				if (numInARow <= block.getDurationType()){
					durationPoints = 1;
				} else {
					durationPoints = -1*10;
				}
			}
		}

		//How many pre-assignments does this person have?
		double numberOfPreAssignments = (double) person.getNumberOfPreAssignments();

		//How much would this peron's vacation preferences be improved?
		double vacationPoints = 0;
		if (block.getService().getName().equalsIgnoreCase("Vac Spring") 
				|| block.getService().getName().equalsIgnoreCase("Vac Fall")){
			vacationPoints = person.getVacationScore(block.getBlockName(),true);
		}

		//How much would this person's requirements improve if we assigned this block?
		double reqScore_initial = person.getNumYearlyViolations() + person.getNumOverallViolations();
		person.addBlock(block);
		double reqScore_final = person.getNumYearlyViolations() + person.getNumOverallViolations();
		person.removeBlock(block);
		double reqScore = -(reqScore_final-reqScore_initial)*PersonConstraintMultiplier;


		blockYearPoints = blockYearPoints*blockYearConstraintMultiplier;
		blockMinMaxPoints = blockMinMaxPoints*blockMinMaxPeopleConstraintMultiplier;
		firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;
		durationPoints = durationPoints*blockDurationMultiplier;
		numberOfPreAssignments = -numberOfPreAssignments*preAssignmentMultiplier;
		vacationPoints = vacationPoints*VacationPreferenceMultiplier;
		
		double score = firmConstraintPoints + vacationPoints + blockYearPoints + blockMinMaxPoints + reqScore + durationPoints + numberOfPreAssignments;
		return score;
	}

	private void oneByNOpt() throws IOException, java.text.ParseException{
		/////////////////////////////////////////////////////////////////////////////
		/////           Hill Climbing (1-Opt for Bad Assignments)               /////
		/////////////////////////////////////////////////////////////////////////////
		double changeCounter = 0;
		boolean changeFound = true;
		while (changeFound){
			System.out.println("*********************************************");
			System.out.println("*****           1 x n               *********");
			System.out.println("*********************************************");
			changeFound = false;
			for (int i = 0; i < people.size(); i ++){
				Person person_i = people.get(i);	
				if (person_i.getNumberOfPreAssignments()==maxNumAssignments){ continue; }
				System.out.println(i + " of " + people.size());
				HashMap<String, Block> schedule_i = person_i.getSchedule();
				for (int x = 0; x < blockNames.size(); x ++){	

					//The current block we are assigned to for this 2-week stint.
					Block block_i_x = schedule_i.get(blockNames.get(x));	

					if (!person_i.isChangeable(block_i_x)){
						continue;
					}

					double personalViolationsCounter = (double) getNumPersonalViolations();
					double blockYearViolationsCounter = (double) getNumBlockYearViolations();
					double blockMinMaxViolationsCounter = (double) getNumBlockMinMaxViolations();
					double blockDurationViolationsCounter = (double) getNumBlockDurationViolations();

					//Get Vacation Score
					double vacScore = 0;
					//Are the vacation requirements satisfied?
					if (person_i.getNumVacationViolations() == 0){
						vacScore += person_i.getVacationScore(block_i_x.getBlockName(),false);
					}

					//How much does this respect the firm constraint?
					double firmConstraintPoints = 0;
					if (block_i_x.getFirmConstraint() == -1){
						firmConstraintPoints +=  0;	//Firm constraint inactive
					} else if (block_i_x.getFirmConstraint() != person_i.getFirm()) {
						firmConstraintPoints += -1;	//Firm constraint broken
					} else {
						firmConstraintPoints +=  0;	//Firm constraint satisfied.
					}

					personalViolationsCounter = personalViolationsCounter*PersonConstraintMultiplier;
					blockDurationViolationsCounter = blockDurationViolationsCounter*blockDurationMultiplier;
					blockYearViolationsCounter = blockYearViolationsCounter*blockYearConstraintMultiplier;
					blockMinMaxViolationsCounter = blockMinMaxViolationsCounter*blockMinMaxPeopleConstraintMultiplier;
					vacScore = vacScore*VacationPreferenceMultiplier;
					firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;

					double score_o = -personalViolationsCounter-blockDurationViolationsCounter
							-blockYearViolationsCounter-blockMinMaxViolationsCounter+vacScore+firmConstraintPoints;

					block_i_x.removePerson(person_i);
					person_i.removeBlock(block_i_x);

					double bestScore = Double.NEGATIVE_INFINITY;
					Block bestBlock = null;
					for (Block newBlock : blockLookup.get(block_i_x.getBlockName())){

						newBlock.addPerson(person_i);
						person_i.addBlock(newBlock);

						////////////////////////////////////////////////////////////////////////////////////////////////////
						personalViolationsCounter = (double) getNumPersonalViolations();
						blockYearViolationsCounter = (double) getNumBlockYearViolations();
						blockMinMaxViolationsCounter = (double) getNumBlockMinMaxViolations();
						blockDurationViolationsCounter = (double) getNumBlockDurationViolations();

						//Get Vacation Score
						vacScore = 0;
						//Are the vacation requirements satisfied?
						if (person_i.getNumVacationViolations() == 0){
							vacScore += person_i.getVacationScore(newBlock.getBlockName(),false);
						}

						//How much does this respect the firm constraint?
						firmConstraintPoints = 0;
						if (newBlock.getFirmConstraint() == -1){
							firmConstraintPoints +=  0;	//Firm constraint inactive
						} else if (newBlock.getFirmConstraint() != person_i.getFirm()) {
							firmConstraintPoints += -1;	//Firm constraint broken
						} else {
							firmConstraintPoints +=  0;	//Firm constraint satisfied.
						}

						personalViolationsCounter = personalViolationsCounter*PersonConstraintMultiplier;
						blockDurationViolationsCounter = blockDurationViolationsCounter*blockDurationMultiplier;
						blockYearViolationsCounter = blockYearViolationsCounter*blockYearConstraintMultiplier;
						blockMinMaxViolationsCounter = blockMinMaxViolationsCounter*blockMinMaxPeopleConstraintMultiplier;
						vacScore = vacScore*VacationPreferenceMultiplier;
						firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;

						double score = -personalViolationsCounter-blockDurationViolationsCounter
								-blockYearViolationsCounter-blockMinMaxViolationsCounter+vacScore+firmConstraintPoints;

						newBlock.removePerson(person_i);
						person_i.removeBlock(newBlock);
						////////////////////////////////////////////////////////////////////////////////////////////////////

						if (score > bestScore){
							bestScore = score;
							bestBlock = newBlock;
						}
					}

					if (bestScore > score_o){
						//Switch

						person_i.addBlock(bestBlock);
						bestBlock.addPerson(person_i);

						//Get number of personal violations.
						personalViolationsCounter = getNumPersonalViolations();
						blockDurationViolationsCounter = getNumBlockDurationViolations();

						changeFound = true;
						changeCounter++;

						System.out.println("Person: " + personalViolationsCounter + "| Duration: "+ blockDurationViolationsCounter+" | Score Diff: " + (bestScore-score_o) + " | New Score: " + bestScore + " | Base Score: " + score_o);
						printSchedule("(New Schedule)");
					} else {
						person_i.addBlock(block_i_x);
						block_i_x.addPerson(person_i);
					}

				}
			}
		}
	}

	private void twoByOneOptAcrossBlocks() throws IOException, java.text.ParseException{
		/////////////////////////////////////////////////////////////////////////////
		/////           Hill Climbing (2-Opt for Bad Assignments)               /////
		/////////////////////////////////////////////////////////////////////////////
		boolean changeFound = true;
		while (changeFound){
			changeFound = false;
			oneByNOpt();
			System.out.println("*********************************************");
			System.out.println("*****           2 x 1               *********");
			System.out.println("*********************************************");
			for (int i = 0; i < people.size(); i ++){
				Person person_i = people.get(i);	
				if (person_i.getNumberOfPreAssignments()==maxNumAssignments){ continue; }

				System.out.println(i + " of " + people.size());

				HashMap<String, Block> schedule_i = person_i.getSchedule();
				for (int j = (i+1); j < people.size(); j++){
					Person person_j = people.get(j);	
					if (person_j.getNumberOfPreAssignments()==maxNumAssignments){ continue; }

					HashMap<String, Block> schedule_j = person_j.getSchedule();

					for (int x = 0; x < blockNames.size(); x ++){	

						Block block_i_x = schedule_i.get(blockNames.get(x));	
						Block block_j_x = schedule_j.get(blockNames.get(x));

						if (!person_j.isChangeable(block_j_x) || !person_i.isChangeable(block_i_x)){
							continue;
						}
						if (block_i_x.getPeople().contains(person_j) ||	block_j_x.getPeople().contains(person_i)) {
							continue;
						}

						ArrayList<Block> blockList = new ArrayList<Block>();
						blockList.add(block_i_x);
						blockList.add(block_j_x);

						boolean duplicate = false;
						for (int z = 0; z < blockList.size(); z ++){
							Block block_z = blockList.get(z);
							for (int zz = z+1; zz < blockList.size(); zz++){
								Block block_zz = blockList.get(zz);
								if (block_z.equals(block_zz)) {
									duplicate = true;
									break;
								}
							}
							if (duplicate) { break;	}
						}
						if (duplicate) { continue; }

						//Undo the assignments
						block_i_x.removePerson(person_i);
						block_j_x.removePerson(person_j);

						person_i.removeBlock(block_i_x);
						person_j.removeBlock(block_j_x);

						ArrayList<ArrayList<Person>> listofPersonLists = new ArrayList<ArrayList<Person>>();

						ArrayList<Person> personList = new ArrayList<Person>();
						personList.add(person_i); //Block i_x
						personList.add(person_j); //Block j_x
						listofPersonLists.add(personList);

						personList = new ArrayList<Person>();
						personList.add(person_j); //Block i_x
						personList.add(person_i); //Block j_x
						listofPersonLists.add(personList);

						double[] score_vec = new double[listofPersonLists.size()];
						for (int z = 0; z < listofPersonLists.size(); z ++){
							personList = listofPersonLists.get(z);						

							//Assign
							for (int zz = 0; zz < personList.size(); zz++){
								Person person = personList.get(zz);
								Block block = blockList.get(zz);
								person.addBlock(block);
								block.addPerson(person);
							}

							double personalViolationsCounter = (double) getNumPersonalViolations();
							double blockYearViolationsCounter = (double) getNumBlockYearViolations();
							double blockMinMaxViolationsCounter = (double) getNumBlockMinMaxViolations();
							double blockDurationViolationsCounter = (double) getNumBlockDurationViolations();

							//Get Vacation Score
							double vacScore = 0;
							for (int zz = 0; zz < personList.size(); zz++){
								Person person = personList.get(zz);
								Block block = blockList.get(zz);

								//Are the vacation requirements satisfied?
								if (person.getNumVacationViolations() == 0){
									vacScore += person.getVacationScore(block.getBlockName(),false);
								}
							}

							//Get Firm Constraint
							//How much does this respect the firm constraint?
							double firmConstraintPoints = 0;
							for (int zz = 0; zz < personList.size(); zz++){
								Block block = blockList.get(zz);
								Person person = personList.get(zz);
								if (block.getFirmConstraint() == -1){
									firmConstraintPoints +=  0;	//Firm constraint inactive
								} else if (block.getFirmConstraint() != person.getFirm()) {
									firmConstraintPoints += -1;	//Firm constraint broken
								} else {
									firmConstraintPoints +=  0;	//Firm constraint satisfied.
								}
							}

							personalViolationsCounter = personalViolationsCounter*PersonConstraintMultiplier;
							blockDurationViolationsCounter = blockDurationViolationsCounter*blockDurationMultiplier;
							blockYearViolationsCounter = blockYearViolationsCounter*blockYearConstraintMultiplier;
							blockMinMaxViolationsCounter = blockMinMaxViolationsCounter*blockMinMaxPeopleConstraintMultiplier;
							vacScore = vacScore*VacationPreferenceMultiplier;
							firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;

							score_vec[z] = -personalViolationsCounter-blockYearViolationsCounter
									-blockMinMaxViolationsCounter-blockDurationViolationsCounter+vacScore+firmConstraintPoints;

							//Unassign
							for (int zz = 0; zz < personList.size(); zz++){
								Person person = personList.get(zz);
								Block block = blockList.get(zz);
								person.removeBlock(block);
								block.removePerson(person);
							}
						}

						double maxScore = Double.NEGATIVE_INFINITY;
						int maxInd = 0;
						for (int z = 0; z < score_vec.length; z ++){
							if (score_vec[z] >= maxScore){
								maxInd = z;
								maxScore = score_vec[z];
							}
						}

						//Make assignments
						personList = listofPersonLists.get(maxInd);
						for (int z = 0; z < personList.size(); z ++){	
							Person person = personList.get(z);
							Block block = blockList.get(z);
							person.addBlock(block);
							block.addPerson(person);
							//								System.out.println("New: " + person.getName() +  " | " + block.getService().getName() + ", " + block.getRotation().getName() + ", " + block.getBlockName());
						}

						//Get number of personal violations.
						int personalViolationsCounter = getNumPersonalViolations();
						//						int blockViolationsCounter = getNumBlockViolations();
						int blockDurationViolationsCounter = getNumBlockDurationViolations();

						if (maxInd > 0){							
							if (maxScore-score_vec[0] > 0){
								System.out.println("*** Change Detected ***");
								changeFound = true;
								System.out.println("Person: " + personalViolationsCounter + "| Duration: "+ blockDurationViolationsCounter+" | Score Diff: " + (maxScore-score_vec[0]) + " | New Score: " + maxScore + " | Base Score: " + score_vec[0]);
								printSchedule("(New Schedule)");
							}
						}
					}
				}
			}
		}
	}

	private void twoByTwoOptAcrossBlocks() throws IOException, java.text.ParseException {

		/////////////////////////////////////////////////////////////////////////////
		/////           Hill Climbing (2-Opt for Bad Assignments)               /////
		/////////////////////////////////////////////////////////////////////////////
		boolean changeFound = true;
		while (changeFound){
			oneByNOpt();
			System.out.println("*********************************************");
			System.out.println("*****           2 x 2               *********");
			System.out.println("*********************************************");
			changeFound = false;
			for (int i = 0; i < people.size(); i ++){
				Person person_i = people.get(i);	
				if (person_i.getNumberOfPreAssignments()==maxNumAssignments){ continue; }

				System.out.println(i + " of " + people.size());

				HashMap<String, Block> schedule_i = person_i.getSchedule();
				for (int j = (i+1); j < people.size(); j++){
					Person person_j = people.get(j);	
					if (person_j.getNumberOfPreAssignments()==maxNumAssignments){ continue; }

					HashMap<String, Block> schedule_j = person_j.getSchedule();

					for (int x = 0; x < blockNames.size(); x ++){	

						Block block_i_x = schedule_i.get(blockNames.get(x));	
						Block block_j_x = schedule_j.get(blockNames.get(x));


						for (int y = x; y < blockNames.size(); y ++){

							Block block_i_y = schedule_i.get(blockNames.get(y));	
							Block block_j_y = schedule_j.get(blockNames.get(y));

							if (!person_j.isChangeable(block_j_x) || !person_j.isChangeable(block_j_y) ||
									!person_i.isChangeable(block_i_x) || !person_i.isChangeable(block_i_y)){
								continue;
							}

							/**
							 * If we switch (i,j) and (x,y), then will the overall score improve?
							 */

							if (block_i_x.getPeople().contains(person_j) ||
									block_i_y.getPeople().contains(person_j) ||
									block_j_x.getPeople().contains(person_i) ||
									block_j_y.getPeople().contains(person_i)){
								continue;
							}

							ArrayList<Block> blockList = new ArrayList<Block>();
							blockList.add(block_i_x);
							blockList.add(block_j_x);
							blockList.add(block_i_y);
							blockList.add(block_j_y);

							boolean duplicate = false;
							for (int z = 0; z < blockList.size(); z ++){
								Block block_z = blockList.get(z);
								for (int zz = z+1; zz < blockList.size(); zz++){
									Block block_zz = blockList.get(zz);
									if (block_z.equals(block_zz)) {
										duplicate = true;
										break;
									}
								}
								if (duplicate) { break;	}
							}
							if (duplicate) { continue; }

							//Undo the assignments
							block_i_x.removePerson(person_i);
							block_i_y.removePerson(person_i);
							block_j_x.removePerson(person_j);
							block_j_y.removePerson(person_j);

							person_i.removeBlock(block_i_x);
							person_i.removeBlock(block_i_y);
							person_j.removeBlock(block_j_x);
							person_j.removeBlock(block_j_y);

							ArrayList<ArrayList<Person>> listofPersonLists = new ArrayList<ArrayList<Person>>();

							ArrayList<Person> personList = new ArrayList<Person>();
							personList.add(person_i); //Block i_x
							personList.add(person_j); //Block j_x
							personList.add(person_i); //Block i_y
							personList.add(person_j); //Block j_y
							listofPersonLists.add(personList);

							personList = new ArrayList<Person>();
							personList.add(person_j); //Block i_x
							personList.add(person_i); //Block j_x
							personList.add(person_j); //Block i_y
							personList.add(person_i); //Block j_y
							listofPersonLists.add(personList);

							personList = new ArrayList<Person>();
							personList.add(person_j); //Block i_x
							personList.add(person_i); //Block j_x
							personList.add(person_i); //Block i_y
							personList.add(person_j); //Block j_y
							listofPersonLists.add(personList);

							personList = new ArrayList<Person>();
							personList.add(person_i); //Block i_x
							personList.add(person_j); //Block j_x
							personList.add(person_j); //Block i_y
							personList.add(person_i); //Block j_y
							listofPersonLists.add(personList);

							double[] score_vec = new double[listofPersonLists.size()];
							for (int z = 0; z < listofPersonLists.size(); z ++){
								personList = listofPersonLists.get(z);						

								//Assign
								for (int zz = 0; zz < personList.size(); zz++){
									Person person = personList.get(zz);
									Block block = blockList.get(zz);
									person.addBlock(block);
									block.addPerson(person);
								}

								double personalViolationsCounter = (double) getNumPersonalViolations();
								double blockYearViolationsCounter = (double) getNumBlockYearViolations();
								double blockMinMaxViolationsCounter = (double) getNumBlockMinMaxViolations();
								double blockDurationViolationsCounter = (double) getNumBlockDurationViolations();

								//Get Vacation Score
								double vacScore = 0;
								for (int zz = 0; zz < personList.size(); zz++){
									Person person = personList.get(zz);
									Block block = blockList.get(zz);

									//Are the vacation requirements satisfied?
									if (person.getNumVacationViolations() == 0){
										vacScore += person.getVacationScore(block.getBlockName(),false);
									}
								}

								//Get Firm Constraint
								//How much does this respect the firm constraint?
								double firmConstraintPoints = 0;
								for (int zz = 0; zz < personList.size(); zz++){
									Block block = blockList.get(zz);
									Person person = personList.get(zz);
									if (block.getFirmConstraint() == -1){
										firmConstraintPoints +=  0;	//Firm constraint inactive
									} else if (block.getFirmConstraint() != person.getFirm()) {
										firmConstraintPoints += -1;	//Firm constraint broken
									} else {
										firmConstraintPoints +=  0;	//Firm constraint satisfied.
									}
								}

								personalViolationsCounter = personalViolationsCounter*PersonConstraintMultiplier;
								blockDurationViolationsCounter = blockDurationViolationsCounter*blockDurationMultiplier;
								blockYearViolationsCounter = blockYearViolationsCounter*blockYearConstraintMultiplier;
								blockMinMaxViolationsCounter = blockMinMaxViolationsCounter*blockMinMaxPeopleConstraintMultiplier;
								vacScore = vacScore*VacationPreferenceMultiplier;
								firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;

								score_vec[z] = -personalViolationsCounter-blockYearViolationsCounter
										-blockMinMaxViolationsCounter-blockDurationViolationsCounter+vacScore+firmConstraintPoints;

								//Unassign
								for (int zz = 0; zz < personList.size(); zz++){
									Person person = personList.get(zz);
									Block block = blockList.get(zz);
									person.removeBlock(block);
									block.removePerson(person);
								}
							}

							double maxScore = Double.NEGATIVE_INFINITY;
							int maxInd = 0;
							for (int z = 0; z < score_vec.length; z ++){
								if (score_vec[z] >= maxScore){
									maxInd = z;
									maxScore = score_vec[z];
								}
							}

							//Make assignments
							personList = listofPersonLists.get(maxInd);
							for (int z = 0; z < personList.size(); z ++){	
								Person person = personList.get(z);
								Block block = blockList.get(z);
								person.addBlock(block);
								block.addPerson(person);
							}

							//Get number of personal violations.
							int personalViolationsCounter = getNumPersonalViolations();
							int blockDurationViolationsCounter = getNumBlockDurationViolations();

							if (maxInd > 0){							
								if (maxScore-score_vec[0] > 0){
									System.out.println("*** Change Detected ***");
									changeFound = true;
									System.out.println("Person: " + personalViolationsCounter + "| Duration: "+ blockDurationViolationsCounter+" | Score Diff: " + (maxScore-score_vec[0]) + " | New Score: " + maxScore + " | Base Score: " + score_vec[0]);
									printSchedule("(New Schedule)");
								}
							}
						}
					}
				}
			}
		}
	}

	private void threeByOneOptAcrossBlocks() throws IOException, java.text.ParseException{
		/////////////////////////////////////////////////////////////////////////////
		/////           Hill Climbing (2-Opt for Bad Assignments)               /////
		/////////////////////////////////////////////////////////////////////////////
		boolean changeFound = true;
		while (changeFound){
			oneByNOpt();
			System.out.println("*********************************************");
			System.out.println("*****           3 x 1               *********");
			System.out.println("*********************************************");
			changeFound = false;
			for (int i = 0; i < people.size(); i ++){
				Person person_i = people.get(i);	
				if (person_i.getNumberOfPreAssignments()==maxNumAssignments){ continue; }

				System.out.println(i + " of " + people.size());

				HashMap<String, Block> schedule_i = person_i.getSchedule();
				for (int j = (i+1); j < people.size(); j++){
					Person person_j = people.get(j);	
					if (person_j.getNumberOfPreAssignments()==maxNumAssignments){ continue; }
					HashMap<String, Block> schedule_j = person_j.getSchedule();

					for (int k = (j+1); k < people.size(); k++){
						Person person_k = people.get(j);	
						if (person_k.getNumberOfPreAssignments()==maxNumAssignments){ continue; }
						HashMap<String, Block> schedule_k = person_j.getSchedule();

						for (int x = 0; x < blockNames.size(); x ++){	

							Block block_i_x = schedule_i.get(blockNames.get(x));	
							Block block_j_x = schedule_j.get(blockNames.get(x));
							Block block_k_x = schedule_k.get(blockNames.get(x));

							if (!person_i.isChangeable(block_i_x)  || !person_j.isChangeable(block_j_x) || 
									!person_k.isChangeable(block_k_x)){
								continue;
							}
							if (block_i_x.getPeople().contains(person_j) ||	block_i_x.getPeople().contains(person_k) ||
									block_j_x.getPeople().contains(person_i) ||	block_j_x.getPeople().contains(person_k) ||
									block_k_x.getPeople().contains(person_i) ||	block_k_x.getPeople().contains(person_j)) {
								continue;
							}

							ArrayList<Block> blockList = new ArrayList<Block>();
							blockList.add(block_i_x);
							blockList.add(block_j_x);
							blockList.add(block_k_x);

							boolean duplicate = false;
							for (int z = 0; z < blockList.size(); z ++){
								Block block_z = blockList.get(z);
								for (int zz = z+1; zz < blockList.size(); zz++){
									Block block_zz = blockList.get(zz);
									if (block_z.equals(block_zz)) {
										duplicate = true;
										break;
									}
								}
								if (duplicate) { break;	}
							}
							if (duplicate) { continue; }

							//Undo the assignments
							block_i_x.removePerson(person_i);
							block_j_x.removePerson(person_j);
							block_k_x.removePerson(person_k);

							person_i.removeBlock(block_i_x);
							person_j.removeBlock(block_j_x);
							person_k.removeBlock(block_k_x);

							ArrayList<ArrayList<Person>> listofPersonLists = new ArrayList<ArrayList<Person>>();

							ArrayList<Person> personList = new ArrayList<Person>();
							personList.add(person_i); //Block i_x
							personList.add(person_j); //Block j_x
							personList.add(person_k); //Block k_x
							listofPersonLists.add(personList);

							personList = new ArrayList<Person>();
							personList.add(person_i); //Block i_x
							personList.add(person_k); //Block j_x
							personList.add(person_j); //Block k_x
							listofPersonLists.add(personList);

							personList.add(person_j); //Block i_x
							personList.add(person_i); //Block j_x
							personList.add(person_k); //Block k_x
							listofPersonLists.add(personList);

							personList.add(person_j); //Block i_x
							personList.add(person_k); //Block j_x
							personList.add(person_i); //Block k_x
							listofPersonLists.add(personList);

							personList.add(person_k); //Block i_x
							personList.add(person_i); //Block j_x
							personList.add(person_j); //Block k_x
							listofPersonLists.add(personList);

							personList.add(person_k); //Block i_x
							personList.add(person_j); //Block j_x
							personList.add(person_i); //Block k_x
							listofPersonLists.add(personList);

							double[] score_vec = new double[listofPersonLists.size()];
							for (int z = 0; z < listofPersonLists.size(); z ++){
								personList = listofPersonLists.get(z);						

								//Assign
								for (int zz = 0; zz < personList.size(); zz++){
									Person person = personList.get(zz);
									Block block = blockList.get(zz);
									person.addBlock(block);
									block.addPerson(person);
								}

								double personalViolationsCounter = (double) getNumPersonalViolations();
								double blockYearViolationsCounter = (double) getNumBlockYearViolations();
								double blockMinMaxViolationsCounter = (double) getNumBlockMinMaxViolations();
								double blockDurationViolationsCounter = (double) getNumBlockDurationViolations();

								//Get Vacation Score
								double vacScore = 0;
								for (int zz = 0; zz < personList.size(); zz++){
									Person person = personList.get(zz);
									Block block = blockList.get(zz);

									//Are the vacation requirements satisfied?
									if (person.getNumVacationViolations() == 0){
										vacScore += person.getVacationScore(block.getBlockName(),false);
									}
								}

								//Get Firm Constraint
								//How much does this respect the firm constraint?
								double firmConstraintPoints = 0;
								for (int zz = 0; zz < personList.size(); zz++){
									Block block = blockList.get(zz);
									Person person = personList.get(zz);
									if (block.getFirmConstraint() == -1){
										firmConstraintPoints +=  0;	//Firm constraint inactive
									} else if (block.getFirmConstraint() != person.getFirm()) {
										firmConstraintPoints += -1;	//Firm constraint broken
									} else {
										firmConstraintPoints +=  0;	//Firm constraint satisfied.
									}
								}

								personalViolationsCounter = personalViolationsCounter*PersonConstraintMultiplier;
								blockDurationViolationsCounter = blockDurationViolationsCounter*blockDurationMultiplier;
								blockYearViolationsCounter = blockYearViolationsCounter*blockYearConstraintMultiplier;
								blockMinMaxViolationsCounter = blockMinMaxViolationsCounter*blockMinMaxPeopleConstraintMultiplier;
								vacScore = vacScore*VacationPreferenceMultiplier;
								firmConstraintPoints = firmConstraintPoints*blockFirmConstraintMultiplier;

								score_vec[z] = -personalViolationsCounter-blockYearViolationsCounter
										-blockMinMaxViolationsCounter-blockDurationViolationsCounter+vacScore+firmConstraintPoints;

								//Unassign
								for (int zz = 0; zz < personList.size(); zz++){
									Person person = personList.get(zz);
									Block block = blockList.get(zz);
									person.removeBlock(block);
									block.removePerson(person);
								}
							}

							double maxScore = Double.NEGATIVE_INFINITY;
							int maxInd = 0;
							for (int z = 0; z < score_vec.length; z ++){
								if (score_vec[z] >= maxScore){
									maxInd = z;
									maxScore = score_vec[z];
									if (z > 0){
										changeFound = true;
									}
								}
							}

							//Make assignments
							personList = listofPersonLists.get(maxInd);
							for (int z = 0; z < personList.size(); z ++){	
								Person person = personList.get(z);
								Block block = blockList.get(z);
								person.addBlock(block);
								block.addPerson(person);
								//								System.out.println("New: " + person.getName() +  " | " + block.getService().getName() + ", " + block.getRotation().getName() + ", " + block.getBlockName());
							}

							//Get number of personal violations.
							int personalViolationsCounter = getNumPersonalViolations();
							//							int blockViolationsCounter = getNumBlockViolations();
							int blockDurationViolationsCounter = getNumBlockDurationViolations();

							if (maxInd > 0){							
								changeFound = true;
								if (maxScore-score_vec[0] > 0){
									System.out.println("Person: " + personalViolationsCounter + "| Duration: "+ blockDurationViolationsCounter+" | Score Diff: " + (maxScore-score_vec[0]) + " | New Score: " + maxScore + " | Base Score: " + score_vec[0]);
									printSchedule("(New Schedule)");
								}
							}
						}
					}
				}
			}
		}
	}

	public int getNumberInARow(Person person, Block block){
		//Does this adhere to the block duration constraint.
		int numberInARow = 0;
		Block priorBlock = block.getPriorBlock();
		Block postBlock = block.getPostBlock();
		if (priorBlock == null && postBlock == null){
			//No adjacent blocks. I don't think this will ever happen.
		} else {
			//How far into the future can we go and still have the same person?
			int futureBlocksCount = 0;
			while (true){
				if (postBlock != null){
					if (postBlock.getPeople().contains(person)){
						futureBlocksCount++;
						postBlock = postBlock.getPostBlock();
						continue;
					} else {
						break;
					}
				} else {
					break;
				}
			}
			//How far into the past can we go and still have the same person?
			int pastBlocksCount = 0;
			while (true){
				if (priorBlock != null){
					if (priorBlock.getPeople().contains(person)){
						pastBlocksCount++;
						priorBlock = priorBlock.getPriorBlock();
						continue;
					} else {
						break;
					}
				} else {
					break;
				}
			}

			numberInARow = pastBlocksCount + futureBlocksCount + 1; //+1 for the new assignment
		}
		return numberInARow;
	}

	public int getNumBlockMinMaxViolations(){
		//Calculate number of block constraint violations
		int blockViolationsCounter = 0;
		for (Block block : grandBlockList){
			int numPeople = block.getPeople().size();
			if (numPeople > block.getBlockMax()){			//This block has too many people
				blockViolationsCounter += Math.pow(numPeople-block.getBlockMax(),2);
			} else if (numPeople < block.getBlockMin()){	//This block doesn't have enough people
				blockViolationsCounter += Math.pow(block.getBlockMin()-numPeople,2);
			}
		}
		return blockViolationsCounter;
	}

	public int getNumBlockYearViolations(){
		//Calculate number of block constraint violations
		int blockViolationsCounter = 0;
		for (Block block : grandBlockList){
			ArrayList<Person> persons = block.getPeople();
			for (Person person : persons){
				if ((block.getBlockType().equalsIgnoreCase("2nd Year") && person.getYear() != 2) ||
						(block.getBlockType().equalsIgnoreCase("3rd Year") && person.getYear() != 3)){
					if (person.isChangeable(block)){
						blockViolationsCounter++;
					}
				}
			}
		}
		return blockViolationsCounter;
	}

	public int getNumBlockDurationViolations(){
		int  blockDurationViolationsCounter = 0;
		for (Block block : grandBlockList){
			ArrayList<Person> persons = block.getPeople();
			for (Person person : persons){
				if (block.getDurationType() != getNumberInARow(person,block)){
					blockDurationViolationsCounter++;
				}
			}
		}
		return blockDurationViolationsCounter;
	}

	public int getNumPersonalViolations(){
		int violationsCounter = 0;
		for (Person person : people){
			if (person.getNumberOfPreAssignments() < maxNumAssignments){
				violationsCounter += person.getTotalNumViolations();
			}
		}
		return violationsCounter;
	}

	public int getNumFirmConstraintViolations(){
		//Did anyone violate a firm constraint?
		int numViolatingFirmConstraint = 0;
		for (int i = 0; i < services.size(); i ++){
			Service sv = services.get(i);
			ArrayList<Rotation> rts = sv.getRotations();
			for (int j = 0; j < rts.size(); j ++){
				Rotation rt = rts.get(j);
				ArrayList<Block> blockList = rt.getBlockList();;
				for (int k = 0; k < blockNames.size(); k ++){
					String columnBlockName = blockNames.get(k);
					boolean found = false;
					int block_ind = -1;
					for (int kk = 0; kk < blockList.size(); kk++){
						String rotationBlockName = blockList.get(kk).getBlockName();
						if (rotationBlockName.equals(columnBlockName)){
							found = true;
							block_ind = kk;
							break;
						}
					}
					if (found){
						//Did anyone violate the firm constraint?
						Block block = blockList.get(block_ind);
						if (block.getFirmConstraint() > -1){
							for (Person person : block.getPeople()){
								if (person.getFirm() != block.getFirmConstraint()){
									numViolatingFirmConstraint += 1;
								}
							}
						}
						
					}
				}
			}
		}
		return numViolatingFirmConstraint;
	}
	
	public void printSchedule(String string) throws IOException, java.text.ParseException{
		
		
		/**
		 * TODO - Send this to the UI, and Sarah will use it to create a graph showing how much
		 * the scheduling is improving over time.
		 */
		double overallScore = getOverallScore();
		
		//Only print the schedule if it is better than the last.
		if (overallScore > bestScheduleSoFar){
			bestScheduleSoFar = overallScore;
			scoreList.add(overallScore);
			long time = System.currentTimeMillis();
			Date date = new Date(time);
			if(!fastSchedule){
				System.out.println(date.toString());
				session.getBasicRemote().sendText(date.toString() + "~ThoroughUpdate");
			}
			
		} else {

			return;
		}
		
		//Convert the score list to the range 0 to 1
		double[] y_values = new double[scoreList.size()];
		double[] x_values = new double[scoreList.size()];
		
		double scoreMin = Double.MIN_VALUE;
		double scoreMax = Double.MAX_VALUE;
		for(int i = 0; i<=scoreList.size()-1; i++){
			if(scoreList.get(i) < scoreMax){
				scoreMax = scoreList.get(i);
			}
			if(scoreList.get(i) > scoreMin){
				scoreMin = scoreList.get(i);
			}
		}
		
//		double scoreMax = scoreList.get(0);
//		
//		double scoreMin = overallScore;
		double scoreDiff = Math.abs(scoreMax-scoreMin);
		double x_counter = 0;
		if (scoreDiff > 0){
			for (int i = 0; i < scoreList.size(); i ++){
				y_values[i] = Math.abs(scoreList.get(i)-scoreMin)/scoreDiff;
				
				if(y_values[i] > 1){
					System.out.println("Bug");
				}
				
				x_values[i] = x_counter/scoreList.size();
				x_counter++;
//				System.out.print(scoreListNormalized[i] + " ");
			}
		} else {
			for (int i = 0; i < scoreList.size(); i ++){
				y_values[i] = 1.0;
				
				x_values[i] = x_counter/scoreList.size();
				x_counter++;
//				System.out.print(scoreListNormalized[i] + " ");
			}	
		}
		
		String pointsString = "";
		for(int i = 0; i <= x_values.length - 1; i++){
			String point = null;
			double x = x_values[i];
			double y = y_values[i];
			if(i != x_values.length -1){
				point = x + "," + y +"" + "|";
			}else{
				point = x + "," + y +"" + "";
			}
			pointsString = pointsString + point;			
		}
		System.err.println("Score: " + pointsString);
		session.getBasicRemote().sendText(pointsString + "~Score");


		//Print out schedule to a csv file
		PrintWriter writer = new PrintWriter(directory + fileName + string + "(Score " + overallScore +  ").csv", "UTF-8");

		//Print out information about the people
		writer.print("Legend:,* - Pre-Assignment,\n\n");
		writer.print("Name,Year,Firm ,# Pre-Assignments,Block Year Violation,Overall Violations,Yearly Violations,Firm Violations,Vac Score,");
		for (int i = 0; i < blockNames.size(); i ++){
			writer.print(blockNames.get(i)+ ",");
		}
		writer.print("\n");
		for (int j = 0; j < people.size(); j ++){
			Person person = people.get(j);
			HashMap<String, Block> schedule = person.getSchedule();

			writer.print("\""+person.getName() +"\""+ "," + person.getYear() + "," + person.getFirm() + ",");

			writer.print(person.getNumberOfPreAssignments() +",");            

			//What is someone's score?
			writer.print(person.getBlockYearConstraintViolations() +",");
			writer.print(person.getOverallViolations() +",");
			writer.print(person.getYearlyViolations() +",");
			writer.print(person.getFirmViolations() + ",");

			//What is someone's vacation score?
			double vacationScore = -1;
			double maxVacationScore = 0;
			if (!person.getIgnoreVacationPreference()){
				vacationScore = person.getTotalVacationScore();
				maxVacationScore = person.getMaxVacationScore();
			}

			if (vacationScore > 0){
				writer.print(vacationScore + " of "  + maxVacationScore + ",");
			} else {
				writer.print("No Preference Provided,");
			}

			//What is their schedule?
			for (int i = 0; i < blockNames.size(); i ++){
				if (schedule.containsKey(blockNames.get(i))){
					Block block = schedule.get(blockNames.get(i));

					if  (block.getDurationType() == -1){
						writer.print(block.getRotation().getName());
						if (!person.isChangeable(block)){
							writer.print("*");
						}
						writer.print(",");
					} else {
						//What is this person's block duration score?
						int totalInARow = 0;
						Block priorBlock = block.getPriorBlock();
						Block postBlock = block.getPostBlock();
						if (priorBlock == null && postBlock == null){
							//No adjacent blocks. I don't think this will ever happen.
						} else {
							//How far into the future can we go and still have the same person?
							int futureBlocksCount = 0;
							while (true){
								if (postBlock != null){
									if (postBlock.getPeople().contains(person)){
										futureBlocksCount++;
										postBlock = postBlock.getPostBlock();
										continue;
									} else {
										break;
									}
								} else {
									break;
								}
							}
							//How far into the past can we go and still have the same person?
							int pastBlocksCount = 0;
							while (true){
								if (priorBlock != null){
									if (priorBlock.getPeople().contains(person)){
										pastBlocksCount++;
										priorBlock = priorBlock.getPriorBlock();
										continue;
									} else {
										break;
									}
								} else {
									break;
								}
							}

							totalInARow = pastBlocksCount + futureBlocksCount + 1; //+1 for the new assignment
						}
						String blockRotationName = block.getRotation().getName();
						writer.print(blockRotationName);
						if (!person.isChangeable(block)){
							writer.print("*");
						}
						if (totalInARow != block.getDurationType()){
							writer.print("(" + totalInARow + "/" + block.getDurationType() + ")");
						}
						writer.print(",");

					}
				} else {
					writer.print("VAC,");
				}
			}

			writer.print("\n");
		}
		writer.print("\n\n\n");


		//Print out the information about the services and rotations.
		writer.print("PERSONNEL CONSTRAINT: MIN/MAX PEOPLE REQUIRED\n");
		writer.print("Service,Rotation,,,,,,,,");
		for (int i = 0; i < blockNames.size(); i ++){
			writer.print(blockNames.get(i)+ ",");
		}
		writer.print("\n");
		for (int i = 0; i < services.size(); i ++){
			Service sv = services.get(i);
			ArrayList<Rotation> rts = sv.getRotations();
			for (int j = 0; j < rts.size(); j ++){
				Rotation rt = rts.get(j);
				writer.print(sv.getName() + "," + rt.getName() + ",,,,,,,,");

				ArrayList<Block> blockList = rt.getBlockList();;
				for (int k = 0; k < blockNames.size(); k ++){
					String columnBlockName = blockNames.get(k);

					boolean found = false;
					int block_ind = -1;
					for (int kk = 0; kk < blockList.size(); kk++){
						String rotationBlockName = blockList.get(kk).getBlockName();
						if (rotationBlockName.equals(columnBlockName)){
							found = true;
							block_ind = kk;
							break;
						}
					}

					if (found){
						String str = "";
						Block block = blockList.get(block_ind);

						if (block.getPeople().size() > block.getBlockMax()){
							//This block has too many people
							str = "\"+" + (block.getPeople().size()-block.getBlockMax()) + "\"";                                
						} else if (block.getPeople().size() < block.getBlockMin()){
							//This block doesn't have enough people
							str = "\"-" + (block.getBlockMin()-block.getPeople().size()) + "\"";                                
						}
						writer.print(str + ",");
					} else {
						writer.print(",");
					}
				}
				writer.print("\n");
			}
		}
		writer.close();
	}
	
	double round4Decimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.####");
    return Double.valueOf(twoDForm.format(d));
}
	
}
