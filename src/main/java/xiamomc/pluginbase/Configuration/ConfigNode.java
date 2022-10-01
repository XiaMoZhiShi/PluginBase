package xiamomc.pluginbase.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ConfigNode
{
    private List<String> nodes;

    public ConfigNode()
    {
        this("root");
    }

    public ConfigNode(String initialNode)
    {
        this.nodes = new ArrayList<>();
        this.Append(initialNode);
    }

    private ConfigNode(List<String> nodes)
    {
        this.nodes = nodes;
    }

    public ConfigNode Append(String node)
    {
        if (node == null || node.isBlank() || node.isEmpty())
            throw new IllegalArgumentException("节点名称不能为空");

        if (node.contains("."))
            throw new IllegalArgumentException("节点名称不能包含“.”: " + node);

        nodes.add(node);
        return this;
    }

    public ConfigNode getCopy()
    {
        return new ConfigNode(nodes);
    }

    public static ConfigNode create()
    {
        return new ConfigNode();
    }

    public static ConfigNode create(String initialNode)
    {
        return new ConfigNode(initialNode);
    }

    @Override
    public String toString()
    {
        StringBuilder finalValue = new StringBuilder();

        for (var n : nodes)
        {
            finalValue.append(".").append(n);
        }

        return finalValue.toString();
    }
}
