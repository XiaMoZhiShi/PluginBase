package xiamomc.pluginbase.Exceptions;

public class DependencyAlreadyRegisteredException extends RuntimeException
{
    public DependencyAlreadyRegisteredException(String s)
    {
        super(s);
    }
}
