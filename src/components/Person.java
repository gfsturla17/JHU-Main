package components;
import java.util.ArrayList;
import java.util.HashMap;


public class Person {



	private String name;
	private int year;
	private int firm;
	private boolean UH_Flag;
	private boolean MP_Flag;
	private HashMap<String,Block> schedule;

	ArrayList<String> preAssignedBlocks;
	private int numberOfPreAssignments;

	private HashMap<String,Integer> vacationPrefs;
	//	private HashMap<String,Integer> vacationPrefs_o;
	private HashMap<String,Integer[]> yearlyReqs;
	private HashMap<String,Integer[]> overallReqs;
	private HashMap<String,Integer> yearlyReqTally;
	private HashMap<String,Integer> overallReqTally;
	private boolean ignoreVacationPreferences;
	
	private int numOverallViolations;
	private int numYearlyViolations;

	public Person(String name, int year, int firm, boolean MP_Flag, boolean UH_Flag, HashMap<String, Integer[]> yearlyReqs, HashMap<String, Integer[]> overallReqs, HashMap<String,Integer> overallReqTally, HashMap<String, Integer> vacationPrefs){

		this.name = name;
		this.year = year;
		this.firm = firm;
		this.UH_Flag = UH_Flag;
		this.MP_Flag = MP_Flag;
		this.vacationPrefs = vacationPrefs;
		this.yearlyReqs = yearlyReqs;
		this.overallReqs = overallReqs;
		this.overallReqTally = overallReqTally;
		
		if (overallReqs.keySet().size() != overallReqTally.keySet().size()){
			System.err.println("Incorrectly created overall requirements for " + name + ".");
			System.exit(0);
		}
		
		if (yearlyReqs == null){
			System.err.println("Incorrectly created yearly requirements for " + name + ".");
			System.exit(0);
		}

		this.yearlyReqTally = new HashMap<String,Integer>();
		if (!MP_Flag){
			for (String key : yearlyReqs.keySet()){
				yearlyReqTally.put(key, 0);
			}
		}

		//The schedule should have 26 elements. Initialize them to be empty.
		schedule = new HashMap<String,Block>();

		preAssignedBlocks = new ArrayList<String>();
		
		updateNumYearlyViolations();
		updateNumOverallViolations();
	}

	public String getName(){
		return this.name;
	}

	public int getYear(){
		return this.year;
	}

	public int getFirm(){
		return this.firm;
	}

	public boolean getUHFlag(){
		return this.UH_Flag;
	}

	public boolean getMPFlag(){
		return this.MP_Flag;
	}

	public double getTotalVacationScore(){
		double totalVacationScore = 0;
		for(String blockName : schedule.keySet()){
			if (schedule.get(blockName).getService().getName().contains("Vac")){
				totalVacationScore += getVacationScore(blockName, false);
			}
		}
		if (totalVacationScore > 6.0){ // 6.0 is the maximum score one person can have.
			System.out.println("Person has too many vacations: " + this.getName());
		}
		return totalVacationScore;
	}

	public double getVacationScore(String blockName, boolean additional){

		if (ignoreVacationPreferences){
			return 0.0;
		} else if (blockName.equals("Christmas")){
			return 0.0;
		} else if (blockName.equals("New Years")){
			return 0.0;
		} else if (blockName.equals("X")){
			return 0.0;
		}
		
		if (!additional) {
			// Just get the person's preference for this block. Does not take into consideration other assignments.
			return vacationPrefs.get(blockName);
		}

		/*
		 * Determine if this is a fall or spring assignment.
		 */
		int firstLetter = 0;
		try{
			firstLetter = Integer.parseInt(blockName.substring(0, 1));
		} catch (Exception e){
			System.err.println("Unable to identify block \"" + blockName + "\". If it is a holiday, please make sure that it is either \"Christmas\", \"New Years\", or \"NY\"");
			System.exit(0);
		}
		String secondLetter = blockName.substring(1, 2);
		String fallOrSpring = "";
		if (firstLetter < 7 && blockName.length() <= 2){
			fallOrSpring = "Vac Fall";
		} else if (firstLetter == 7 && blockName.length() <= 2){
			if (secondLetter.equalsIgnoreCase("A")){
				fallOrSpring = "Vac Fall";
			} else {
				fallOrSpring = "Vac Spring";
			}
		} else {
			fallOrSpring = "Vac Spring";
		}

		/*
		 *  Get the additional value of assigning a vacation to this block considering the other vacation
		 *  assignments we have already made for this person.
		 */
		Integer tally = yearlyReqTally.get(fallOrSpring);
		int min = yearlyReqs.get(fallOrSpring)[0];

		if (tally < min){
			return vacationPrefs.get(blockName);
		} else {
			return -1;
		}
	}

	public HashMap<String,Block> getSchedule(){
		return schedule;
	}

	public void addBlock(Block block) {

		schedule.put(block.getBlockName(), block);

		//Update the requirements

		String sv = block.getRotation().getService().getName();

		if (yearlyReqTally.get(sv) != null){
			Integer val = yearlyReqTally.get(sv)+1;
//			yearlyReqTally.replace(sv, val);
			yearlyReqTally.put(sv, val);
		}

		if (overallReqTally.get(sv) != null){
			Integer val = overallReqTally.get(sv)+1;
//			overallReqTally.replace(sv, val);
			overallReqTally.put(sv, val);

		}
				
		updateNumYearlyViolations();
		updateNumOverallViolations();
	}
	
	public void removeBlock(Block block){
		schedule.remove(block.getBlockName());

		//Update the requirements

		String sv = block.getRotation().getService().getName();
		
		if (yearlyReqTally.get(sv) != null){
			Integer val = yearlyReqTally.get(sv)-1;
//			yearlyReqTally.replace(sv, val);
			yearlyReqTally.put(sv, val);

		}

		if (overallReqTally.get(sv) != null){
			Integer val = overallReqTally.get(sv)-1;
//			overallReqTally.replace(sv, val);
			overallReqTally.put(sv, val);

		}
		
		updateNumYearlyViolations();
		updateNumOverallViolations();
	}
	
	public void setBlockNotChangeable(Block block) {
		this.preAssignedBlocks.add(block.getBlockName());
	}

	public boolean isChangeable(Block block){
		for (String str : preAssignedBlocks){
			if (str.equalsIgnoreCase(block.getBlockName())){
				return false;
			}
		}
		return true;
	}

	public int getNumVacationViolations(){
		int count = 0;
		for (String key : yearlyReqTally.keySet()){
			if (!(key.equals("Vac Spring") || key.equalsIgnoreCase("Vac Fall"))){
				continue;
			}
			Integer val = yearlyReqTally.get(key);
			int min = yearlyReqs.get(key)[0];
			int max = yearlyReqs.get(key)[1];
			if (val < min){
				//Currently less than minimum
				count += Math.abs(val-min);
			} else if (val > max) {
				//Would be greater than maximum
				count += Math.abs(val-max);
			}
		}
		return count;

	}

	public String getYearlyViolations(){
		String str = "";
		if (MP_Flag){
			return str;
		}
		for (String key : yearlyReqTally.keySet()){
			Integer val = yearlyReqTally.get(key);
			int min = yearlyReqs.get(key)[0];
			int max = yearlyReqs.get(key)[1];
			if (val < min){
				//Currently less than minimum
				str += " " + key + ": -" + (min-val);
			} else if (val > max) {
				//Would be greater than maximum
				str += " " + key + ": +" + (val-max);
			}
		}
		return str;

	}

	public int getNumYearlyViolations(){
		return numYearlyViolations;
	}
	
	public void updateNumYearlyViolations(){
		numYearlyViolations = 0;
		if (MP_Flag){
			return;
		}
		for (String key : yearlyReqTally.keySet()){
			Integer val = yearlyReqTally.get(key);
			int min = yearlyReqs.get(key)[0];
			int max = yearlyReqs.get(key)[1];
			if (val < min){
				//Currently less than minimum
				numYearlyViolations += Math.abs(val-min)*10;
			} else if (val > max) {
				//Would be greater than maximum
				numYearlyViolations += Math.abs(val-max);
			}
		}
	}

	public String getOverallViolations(){
		String str = "";
		if (MP_Flag){
			return str;
		}
		for (String key : overallReqs.keySet()){
			Integer val = overallReqTally.get(key);
			int min = overallReqs.get(key)[0];
			int max = overallReqs.get(key)[1];
			if (val < min){
				if (this.year == 3){
					//Currently less than minimum
					str += " " + key + ": -" + (min-val);
				}
			} else if (val > max) {
				//Would be greater than maximum
				str += " " + key + ": +" + (val-max);
			}
		}
		return str;

	}

	public int getNumOverallViolations(){
		return numOverallViolations;
	}
	
	private void updateNumOverallViolations(){
		numOverallViolations = 0;
		if (MP_Flag){
			return;
		}
		for (String key : overallReqs.keySet()){
			Integer val = overallReqTally.get(key);
			int min = overallReqs.get(key)[0];
			int max = overallReqs.get(key)[1];
			if (val < min){
				if (this.year == 3){
					//Currently less than minimum
					numOverallViolations += Math.abs(val-min)*10;
				}
			} else if (val > max) {
				//Would be greater than maximum
				numOverallViolations += Math.abs(val-max);
			}
		}
	}

	public int getTotalNumViolations(){
		return this.getNumOverallViolations()+this.getNumYearlyViolations();
	}

	public double getMaxVacationScore() {
		double maxSpringVacScore = 0;
		double maxFallVacScore = 0;
		for (String key : vacationPrefs.keySet()){

			if (key.equalsIgnoreCase("Christmas") || key.equalsIgnoreCase("NY") || key.equalsIgnoreCase("New Years")){
				continue;
			}

			int firstLetter = Integer.parseInt(key.substring(0, 1));
			String secondLetter = key.substring(1, 2);
			double val = (double) vacationPrefs.get(key);

			if (firstLetter < 7 && key.length() <= 2){
				maxFallVacScore = Math.max(maxFallVacScore,val);
			} else if (firstLetter == 7 && key.length() <= 2){
				if (secondLetter.equalsIgnoreCase("A")){
					maxFallVacScore = Math.max(maxFallVacScore,val);
				} else {
					maxSpringVacScore = Math.max(maxSpringVacScore,val);
				}
			} else {
				maxSpringVacScore = Math.max(maxSpringVacScore,val);
			}			
		}
		return maxSpringVacScore + maxFallVacScore;
	}

	public void setNumberOfPreAssignments(int numberOfPreAssignments) {
		this.numberOfPreAssignments = numberOfPreAssignments;
	}

	public int getNumberOfPreAssignments(){
		return numberOfPreAssignments;
	}

	public void setIgnoreVacationPreference(boolean b) {
		this.ignoreVacationPreferences = b;
	}

	public boolean getIgnoreVacationPreference(){
		return ignoreVacationPreferences;
	}

	public String getBlockYearConstraintViolations() {
		String str = "\"";
		if (year == 4){
			str += "\"";
			return str;
		}
		int count = 0;
		for (String blockName : schedule.keySet()){
			Block block = schedule.get(blockName);
			if ((block.getBlockType().equalsIgnoreCase("2nd Year") && this.getYear() != 2) ||
					(block.getBlockType().equalsIgnoreCase("3rd Year") && this.getYear() != 3)){
				if (count > 0){
					str = str + ",";
				}
				str = str + block.getRotation().getName() + "(" + block.getBlockName() + ")";

				if (!isChangeable(block)){
					str = str+"*";
				}
				count++;
			}
		}
		str += "\"";

		return str;
	}

	public boolean hasEnough(String sv) {
		if (yearlyReqTally.containsKey(sv)){
			int total = yearlyReqTally.get(sv);
			int min = yearlyReqs.get(sv)[0];
			int max = yearlyReqs.get(sv)[1];
			if (min <= total && total <= max){
				//OK
			} else {
				return false;
			}
		}
		if (overallReqTally.containsKey(sv)){
			int total = yearlyReqTally.get(sv);
			int min = yearlyReqs.get(sv)[0];
			int max = yearlyReqs.get(sv)[1];
			if (min <= total && total <= max){
				//OK
			} else {
				return false;
			}
		}
		
		
		return true;
	}

	public boolean isScheduled(String block) {
		for (String key : schedule.keySet()){
			if (key.equalsIgnoreCase(block)){
				return true;
			}
		}
		return true;
	}
}
