package designpatterns.resourcepool;

/**
 * Factory interface.
 * <p> Provides abstraction.
 * <p> Declares a method createResource, which is overridden in the interface implementation
 */
public interface ResourceFactory {
    /**
     * This method creates a resource; the type will be specified in the implementation of the interface
     * @return a resource
     * @throws Exception
     */
    public Resource createResource() throws Exception;
}
