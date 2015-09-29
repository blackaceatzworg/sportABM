/*
©Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
 */

package repastcity3.agent;          

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import repast.simphony.random.RandomHelper;
import repastcity3.environment.Building;
import repastcity3.environment.Route;
import repastcity3.main.ContextManager;
import repastcity3.main.GlobalVars;



public class DefaultAgent implements IAgent {

	private static Logger LOGGER = Logger.getLogger(DefaultAgent.class
			.getName());

	private Building home; // Where the agent lives
	private Building workplace; // Where the agent works
	private Route route; // An object to move the agent around the world
	private String transport = "";
	private List<String> transportAvailable = null;

	//private boolean goingHome = false; // Whether the agent is going to or from
	// their home

	private static int uniqueID = 0;
	private int id;
	int numberOfPools = 19; //it should rather be global variable
	Building[] allClubs =new Building[20]; //KEJ+ an array of all clubs

	private int whereAmI=0; // This variable says where this agent is.
	//0 - home, 1 - work, 2 - club, 3 - on the way.

	private double myFavouriteClub= 0;
	private double myTime4Sport = 16.1;
	private double myTimeOfDeparture;
	private double myTimeAtClub;
	private boolean IsHeGoingToClubFromHome=false; //KEJ+ remember to get rid of it if for not working agents


	//KEJ+ MY VARIABLES hold in myRanking
	private double[][] sRanking =new double[20][4]; // sRanking => ranking of swimming pools (20)

	// 0-idClub 1-ranking 2-HC 3-tooCrowded


	public DefaultAgent() { //bankers change. CONSTRUCTOR.
		this.id = uniqueID++;
		// Find a building that agents can use as their workplace. First, iterate over all buildings in the model (there should be less than 10 000)
		// populate 2 arrays of clubs.
		int i=0;
		for (Building b:ContextManager.buildingContext.getRandomObjects(Building.class, 10000)) {
			// See if the building is a bank (they will have type==2).
			if (b.getType()==2) { //KEJ+ here I can change the code so agent will find his real workplace
				this.workplace = b;
			}
			//make a list of all clubs
			if (b.getType()==3 && b.getSwimming()==1) {
				allClubs[i]= b;
				sRanking[i][0]=b.getClubID();
				i++;
			}
		}

		// (temp) 2-HC - home-club distance in km (it will be replaced with Nicks method calc)
		for (int s=0; s<numberOfPools+1; s++) {
			// check if the building is a club
			sRanking[s][2]= s;
		}

		// this part will go at the beginning of each day. here to check (THIS WILL BE ALSO CHANGED)
		//1-ranking
		for (int s=0; s< numberOfPools+1; s++) { 
			// check if the building is a club
			sRanking[s][1]= sRanking[s][2]+sRanking[s][3];       
		}

		//KEJ+ what is my favourite club index?
		myFavouriteClub=whatsClubIndex(48.0); //TEMP, id of club that every1 goes to

		//		for (int f=1; f<numberOfPools;f++){
		//			if (sRanking[(int)myFavouriteClub][1]>sRanking[f][1]){
		//				myFavouriteClub=f;//TEMP. SEARCH FOR THE LOWEST NUMBER (=highest position in ranking)
		//			}
		//		}



		//check if Ranking is OK
		String check = Arrays.deepToString(sRanking);
		LOGGER.log(Level.INFO, "Agent " + this.toString() + "has favourite club with index: "+ myFavouriteClub+
				" and has ranking list of all clubs which is: " + check) ;
	}



	@Override
	public void step() throws Exception {
// step is done in 3 parts: 1. Checks where am I 2. if at destination do sth 3. else: travel

		// Nick's code - agents go to work
		// See what the time is, this will determine what the agent should be doing. The BigDecimal stuff
		// is just to round the time to 5 decimal places, otherwise it will never be exactly 9.0 or 17.0.
		double theTime = BigDecimal.valueOf(ContextManager.realTime).
				round(new MathContext(5,RoundingMode.HALF_UP)).doubleValue();
		
		// IS HE HOME?
		if (whereAmI==0){ 
			if (theTime == 7.0) { // I assume at 4pm he goes to club or home
				this.route = new Route(this, this.allClubs[(int)myFavouriteClub].getCoords(), this.allClubs[(int)myFavouriteClub]); // Create a route home
				//		        this.route = new Route(this, this.allClubs[(int) myFavouriteClub].getCoords(), this.allClubs[(int)myFavouriteClub]); // Create a route home
				whereAmI=3; // he is on the WAY now
				myTimeOfDeparture=theTime;
				//				double[] distances = Route.getRouteDistance(this, this.allClubs[(int)myFavouriteClub].getCoords(), this.allClubs[(int)myFavouriteClub]);
				//				System.out.println(distances[0]+","+distances[1]);
			}
			if (IsHeGoingToClubFromHome && myTime4Sport==theTime){
				this.route = new Route(this, this.allClubs[(int)myFavouriteClub].getCoords(), this.allClubs[(int)myFavouriteClub]); // Create a route home
				whereAmI=3; // he is on the WAY now
				myTimeOfDeparture=theTime;				
			}
		}

		// IS HE AT CLUB? Check if they finished their sport practice
		else if(whereAmI==2 && theTime == myTimeAtClub + GlobalVars.howLongAtClub){ 
			allClubs[(int)myFavouriteClub].removeAgent(this);
			System.out.print("clear");//agent go home after swimming
			LOGGER.log(Level.INFO, "Agent " + this.toString() + " has visited club with ID: "+ sRanking[(int)myFavouriteClub][0] +" and is heading home now" );

			this.route = new Route(this, this.home.getCoords(), this.home);
		}
		
		//IS HE AT WORK?
		else if(whereAmI==1 && theTime == 16.0){ //we can change 16 into variable...
			if (IsHeGoingToClubFromHome){
				this.route = new Route(this, this.home.getCoords(), this.home); 
				whereAmI=3; // he is on the WAY now
			}
			else{
			this.route = new Route(this, this.allClubs[(int)myFavouriteClub].getCoords(), this.allClubs[(int)myFavouriteClub]); // Create a route home
			whereAmI=3; // he is on the WAY now
			myTimeOfDeparture=theTime;
			}
		}
		
		//		if (theTime == 7.0) { // 7am, Agent should go to work
		//			this.route = new Route(this, this.workplace.getCoords(), this.workplace); // Create a route to work
		//		}

		//		//if agent worked 8 hours, he goes to the club (if IsHeGoingToClubFromHome==false) or he goes home
		//		if (theTime ==16.0){
		//				if (!IsHeGoingToClubFromHome && myTime4Sport-16.0 < 0.5){ //NOTE THAT myTime4Sport SHOULD BE GREATER THAN 16.0
		//					this.route = new Route(this, this.allClubs[(int)myFavouriteClub].getCoords(), this.allClubs[(int)myFavouriteClub]); 
		//				}


		
		

		if (this.route == null) {
		} // Don't do anything if a route hasn't been created.

		else if (this.route.atDestination()) {
			// Have reached our destination, lets delete the old route (more efficient). //KEJ+ IT SHOULD BE DONE DIFFERENTLY to make it more efficient
			this.route = null;

			//make it syncronised .,.,;
			//THAT'S THE HEART OF MY SPORT ABM

			//AM I AT CLUB?
			if (allClubs[(int)myFavouriteClub].getCoords() == ContextManager.getAgentGeometry(this).getCoordinate()){
				//if agent is by the club, he checks if he can enter (if not => he goes home, if yes - he stay there for 1 hour)

				if (allClubs[(int)myFavouriteClub].mayIenter() && !allClubs[(int)myFavouriteClub].isAgentAtThisClub(this)){ //check if he can enter 
					//HE ENTERS THE CLUB
					whereAmI=2;
					allClubs[(int)myFavouriteClub].addAgent(this);
					allClubs[(int)myFavouriteClub].clientEntry(); //add up client entry
					myTimeAtClub=theTime;

					//System.out.println("HOP ");
					System.out.print("\n"+ "agent " + this.id + " got here in (h): ");
					System.out.printf("%.2f", (theTime-myTimeOfDeparture));
				}

				//if agent CAN'T ENTER to the club:
				else if(!allClubs[(int)myFavouriteClub].isAgentAtThisClub(this)){
					this.route = new Route(this, this.home.getCoords(), this.home);
					whereAmI=3;

					myTime4Sport += GlobalVars.checkClubLater; //check the club later on
					IsHeGoingToClubFromHome=true;//next time check the club from home //ITS NOT ON WORK NOW, because I omitted work in the model
					if (myTime4Sport > 21.50)
					{	//check next club
						myFavouriteClub=whatsClubIndex(24.0); // TEMP. it should follow ranking
						//TEMP. NOT DONE YET//check the overcrowded club as overcrowded=1 - it should place it at the end of club ranking
						//return to default values
						myTime4Sport=16.0;
						IsHeGoingToClubFromHome=false;
						System.out.print(this + "changed the favourite club");

						//TEMP NOT DONE YET // if agent iterated through all clubs, and he couldn't find his place, print: "I'd rather play squash!"
					}
				}
			}

			//AM I AT WORK?
			else if (this.workplace.getCoords() == ContextManager.getAgentGeometry(this).getCoordinate()){
				whereAmI=1;
			}
			//AM I HOME?
			else { //if (this.home.getCoords() == ContextManager.getAgentGeometry(this).getCoordinate())
				whereAmI=0;
			}

		} // end of if ...atDestination

		else {
			// Otherwise travel towards the destination
			this.route.travel();


		} // else



	}// step()

	/**
	 * There will be no inter-agent communication so these agents can be
	 * executed simulataneously in separate threads.
	 */
	@Override //KEJ+ I changed it
	public final boolean isThreadable() {
		return false;
	}

	@Override
	public void setHome(Building home) { //there should be error here
		this.home = home;
	}

	@Override
	public Building getHome() {
		return this.home;
	}

	@Override
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
	}

	@Override
	public List<String> getTransportAvailable() {
		//List<String> myTransport = new ArrayList<String>();
		//myTransport.add("bus");
		return this.transportAvailable; //IT SHOULD BE WRITTEN IN PEOPLE SHAPEFILE
	} //	return null;

	// return new Arrays.asList(new String[]{"car", "bus"});

	@Override
	public String toString() {
		return "Agent " + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultAgent))
			return false;
		DefaultAgent b = (DefaultAgent) obj;
		return this.id == b.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}


	public void setTransport(String transport) {
		this.transport=transport;
		this.transportAvailable = Arrays.asList( this.transport.split("\\s+")); //it splits a string because 1 agent can use at least 2 modes of transport (walk + sth else).
		System.out.print(this.id+":"+this.transport+" - "+this.transportAvailable.toString()); //check

	}
	public String getTransport() {
		return this.transport;
	}

	//how to get to club with id X
	public int whatsClubIndex(double clubID){
		int searchedIndex=-1;
		for (int s=0; s< numberOfPools+1; s++) { 
			// check if the building is a club

			if (clubID==sRanking[s][0]){
				searchedIndex=s;
			}       
		}
		return searchedIndex;
	}

}
