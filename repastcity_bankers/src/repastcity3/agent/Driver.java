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

public class Driver implements IAgent {

	private static Logger LOGGER = Logger.getLogger(Driver.class
			.getName());

	private Building home; // Where the agent lives
	private Building workplace; // Where the agent works
	private Route route; // An object to move the agent around the world

	private boolean goingHome = false; // Whether the agent is going to or from
										// their home

	private static int uniqueID = 0;
	private int id;

	public Driver() { //bankers change. CONSTRUCTOR.
        this.id = uniqueID++;
        // Find a building that agents can use as their workplace. First, iterate over all buildings in the model
        for (Building b:ContextManager.buildingContext.getRandomObjects(Building.class, 10000)) {
                // See if the building is a bank (they will have type==2).
                if (b.getType()==2) {
                        this.workplace = b;
                        break; // Have found a bank, stop searching.
                }
        }
	}
	
	
	
	@Override
	public void step() throws Exception {

		
		// Nick's code - agenst go to work
		// See what the time is, this will determine what the agent should be doing. The BigDecimal stuff
		// is just to round the time to 5 decimal places, otherwise it will never be exactly 9.0 or 17.0.
		double theTime = BigDecimal.valueOf(ContextManager.realTime).
		        round(new MathContext(5,RoundingMode.HALF_UP)).doubleValue();

		if (theTime == 9.0) { // 9am, Agent should be working
		        this.route = new Route(this, this.workplace.getCoords(), this.workplace); // Create a route to work
		}
		else if (theTime == 17.0) { // 5pm, agent should go home
		        this.route = new Route(this, this.home.getCoords(), this.home); // Create a route home
		}

		if (this.route == null) {
		        // Don't do anything if a route hasn't been created.
		} else if (this.route.atDestination()) {
		        // Have reached our destination, lets delete the old route (more efficient).
		        this.route = null;
		
		
/*		}else { IF I COMMENT IT OUT AND DELETE PART OF THE STEP BELOW, I'LL GET AGETS GO TO WORK CODE.
		        // Otherwise travel towards the destination
		        this.route.travel();
		}*/
		
		// AGENTS GO BURGLE RANDOM HOME        
		} 
		else {
        // Otherwise travel towards the destination
        this.route.travel();
        for (Building b : this.route.getPassedBuildings()) {
                if (b.getType() == 1) { // Only burgle houses (not banks too)
                // Roll a dice to see if this house should be burgled (1 in 100 chance)
                double random;
                synchronized (ContextManager.randomLock) {
                        // This synchronized block ensures that only one agent at a time can access RandomHelper
                        random = RandomHelper.nextDouble();
                }
                if (random >= 0.99) {
                        b.burgled(); // Tell the building that it has been burgled
                        LOGGER.log(Level.INFO, "Agent " + this.toString() + " has burgled building "
                                        + b.getIdentifier() + "Total: " + b.getNumBurglaries() + 
                                        ". Random value: " + random);
                        }
                }
        } // for passed buildings
} // else
		
		
	} // step()

	/**
	 * There will be no inter-agent communication so these agents can be
	 * executed simulataneously in separate threads.
	 */
	@Override
	public final boolean isThreadable() {
		return true;
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
		List<String> myTransport = new ArrayList<String>();
		myTransport.add("car");
		return myTransport;
	}

	// return new Arrays.asList(new String[]{"car", "bus"});

	@Override
	public String toString() {
		return "Agent " + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Driver))
			return false;
		Driver b = (Driver) obj;
		return this.id == b.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}
}
