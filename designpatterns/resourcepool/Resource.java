package designpatterns.resourcepool;

/**
 * This is the interface for the resources
 */
public interface Resource {     
    /**
    * This is the method which destroys the resource.
    * <p> In the case of a database connections, it kills/closes it.
    * <p> We need to use this while shrinking the pool.
    */
    public void destroyResource();
}