package designpatterns.resourcepool;

import java.util.*;

import java.util.concurrent.locks.*;


/**
* Resource Pool
* <p>This is the program which creates and manages the resource pool.
* <p>It maintains two arraylists, one with all the free resources, and one with all the used resources.
* <p>It facilitates the acquiring and releasing of resources.
* <p>Also, based on the percentage of free connections, it can expand or shrink the pool. It uses threads to do so, so that expansion and shrinkage can happen independently, without having to stall the user.
*/

//uses threads for pool expansion and shrinkage, so that they can run parallelly and concurrently, and it doesn't affect user's wait time
public class ResourcePool {

	private ArrayList<Resource> usedResources;
	private ArrayList<Resource> freeResources;

	//poolLock used to avoid race condition in getResource and releaseResource
	//therefore, only one thread can acquire poolLock and execute inside getResource/releaseResource at a time
	private Lock poolLock;
	Condition resourceAvailable; 
	
	private boolean shrinkingInProgress = false;
	private boolean expandingInProgress = false;
	
	//resource pool does not know about which type of resource is needed, and the factory that creates it
	//therefore, factory is of ResourceFactory type
	private ResourceFactory factory;

	private int minSize;
	private int maxSize;
	private int capacity;
	

	/**
	 * This is the resource pool constructor.
	 * 
	 * @param minSize This is the minimum capacity of the pool.
	 * @param maxSize This is the maximum capacity of the pool.
	 * @param factory This is the factory which creates the type of connection specified by the user.
	 */
	public ResourcePool(int minSize, int maxSize, ResourceFactory factory) {
        poolLock = new ReentrantLock();
		resourceAvailable = poolLock.newCondition();
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.factory = factory;
		capacity = minSize;
        usedResources = new ArrayList<Resource>();
		freeResources = new ArrayList<Resource>();
		
		//initialising the pool to initial capacity
		for(int i=0; i<minSize; i++) {
			try {
				freeResources.add(factory.createResource());
			}
			catch (Exception e) {
				log(e.toString());
				break;
			}
		}
	}

	
	/**
	 * Implements the lock mechanism when waiting for a connection to become available
	*/
	public void waitForResource() throws Exception{
		try{ 
			poolLock.lock();
			//await implicitly releases the lock until it receives a signal
			resourceAvailable.await();
		} finally {
			poolLock.unlock();
		}
	}


	/**
	 * After acquiring a poolLock, this method is used to get a resource from the free resource arraylist, assuming it's not empty.
	 * <p> It transfers one free resource from the free resource arraylist to the used resource arraylist.
	 * <p> It also checks to see if it should expand the pool size.  If so, it creates and runs a new thread for it.
	 * @return A resource object, if free resources isn't empty. Otherwise, it returns null.
	 
	 */
	public Resource getResource() {
		//only one thread can handle the arraylists at a time
		try {
			poolLock.lock();
			int free_size = freeResources.size();		
			//if there are free resources, remove the last one from freeResources and place it in usedResources	
			if(free_size>0) {
				usedResources.add(freeResources.remove(free_size-1));
				
				//checks for expansion condition (total no. of resources < max capacity AND more than 75% of the pool is being used)
				if(shouldExpand()) {
					//create a thread for expansion and run it (it will run parallelly and independently)
					expandThread eThread = new expandThread();				
					eThread.start();					
				}
				//return the resource that was just placed in usedResources
				return usedResources.get(usedResources.size()-1);
			}
			else {
				return null;
			}
		} finally {
				poolLock.unlock();
			}
		
    }


	/**
	 * After acquiring a poolLock, this method is used to release a resource from the used resource arraylist, assuming it's not empty.
	 * <p> It transfers the used resource from the used resource arraylist back to the free resource arraylist.
	 * <p> It also checks to see if it should shrink the pool size. If so, it creates and runs a new thread for it.
	 * @param c This is the resource that was obtained through the getResource method.
	 */
	public void releaseResource(Resource c) throws Exception{
		//only one thread can handle the arraylists at a time
		try {
			//log("ReleaseResource going to aquireLock");
			poolLock.lock();
			//log("ReleaseResource got the lock");
			int used_size = usedResources.size();

			//if there are used resources, remove the passed resource parameter from usedResources and place it back in freeResources		
			if(used_size>0) {
				freeResources.add(usedResources.remove(usedResources.indexOf(c)));
				resourceAvailable.signal();
				
				//checks for shrinkage condition (total no. of resources > min capacity AND less than 25% of the pool is being used)
				if(shouldShrink()) {
					//create a thread for shrinkage and run it (it will run parallelly and independently)				
					shrinkThread sThread = new shrinkThread();				
					sThread.start();
				}
			}
			else
				throw new Exception("ERROR: No such resource");
		} finally {
			poolLock.unlock();
		}
    }

	/**
	 * Logs the current thread during which the output is to be displayed
	 * @param s This is the string to be printed
	 */
	public static void log(String s)
	{
		System.out.println("\n-------------------------------\nBegin: " + Thread.currentThread().getName() + "\n" + s + "\nEnd: " + Thread.currentThread().getName()+"\n-------------------------------\n");
	}
	
	/**
	 * Displays the number of used resources and the number of free resources
	 */
	public void displayPoolState()
	{
		log("***** Resource Pool State *****\nUsed Size: " + usedResources.size() + "\nFree Size: " + freeResources.size() + "\n*******************************");
	}

	/**
	 * Checks to see if the pool size should expand, ie, if
	 * <p> total number of resources < maximum capacity AND more than 75% of the pool is being used
	 * @return Boolean value
	 */
	private boolean shouldExpand() {
		int used_size = usedResources.size();
		int free_size = freeResources.size();

		//total no. of resources < max capacity AND more than 75% of the pool is being used
		if((free_size + used_size)<maxSize && (used_size) / (free_size+used_size) == 1.0) {
			return true;
		}
		return false;
	}

	/**
	 * Checks to see if the pool size should shrink, i.e., if
	 * <p> total number of resources > minimum capacity AND less than 25% of the pool is being used
	 * @return Boolean value
	 */
	private boolean shouldShrink() {
		int used_size = usedResources.size();
		int free_size = freeResources.size();

		//total no. of resources > min capacity AND less than 25% of the pool is being used
		if((free_size + used_size)>minSize && ((used_size) / (free_size + used_size)) < 0.25) {
			return true;
		}
		return false;
	}


	/**
	 * Defines the thread used for pool expansion
	 */
	private class expandThread extends Thread {
		/**
		 * runs the thread
		 */
		public void run () {
			try {
				poolLock.lock();
				if(expandingInProgress)
					return;
				expandingInProgress = true;
			} finally {
				poolLock.unlock();
			}
			int expandedBy=0;
			for(int i=0; i<capacity; i++) {
				//expand while pool max capacity has not been reached
				if(usedResources.size() + freeResources.size() >= maxSize)
					break;
				try {
					poolLock.lock();
					try {
						freeResources.add(factory.createResource());
						expandedBy++;
					}
					catch (Exception e) {
						System.out.println(e);
						break;
					}
				} finally {
					poolLock.unlock();
				}
			}
			if(expandedBy>0) {
				log("Expanded by: "+expandedBy);
				displayPoolState();
				capacity = usedResources.size() + freeResources.size();
				expandedBy=0;
			}
			expandingInProgress = false;			
		}
	}


	/**
	 * Defines the thread used for pool shrinkage
	 */
	private class shrinkThread extends Thread {
		/**
		 * runs the thread
		 */
		public void run () {	
			try {
				poolLock.lock();
				if(shrinkingInProgress)
					return;
				shrinkingInProgress = true;
			} finally {
				poolLock.unlock();
			}
			
			int shrunkBy=0;
			//cut free size in half
			for(int i=0; i<freeResources.size()/2; i++) {				
				//stay above min capacity
				if(usedResources.size() + usedResources.size() + freeResources.size() <= minSize)
					break;
				//remove and destroy from freeResources
				Resource rsc = null;
				try {
					poolLock.lock();
					if(!shouldShrink()) {
						break;
					}						
					rsc = freeResources.remove(freeResources.size()-1);
					shrunkBy++;
				} finally {
					poolLock.unlock();
				}
				try {	
					rsc.destroyResource();
				}
				catch(Exception e) {
					System.out.println(e);
					break;
				}
			}
			if(shrunkBy>0) {
				log("Shrunk by: "+shrunkBy);
				displayPoolState();
				capacity = usedResources.size() + freeResources.size();
			}
			shrinkingInProgress = false;			
		}
	}
}
