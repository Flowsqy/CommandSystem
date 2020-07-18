package fr.flo504.commandsystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.server.v1_16_R1.CommandListenerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.craftbukkit.v1_16_R1.command.BukkitCommandWrapper;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class NewTabCompleter {

    private static final String versionName = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    private static final int version = Integer.parseInt(versionName.substring(1).split("_")[1]);
    private static final String npack = "net.minecraft.server." + versionName + ".";
    private static final String cpack = Bukkit.getServer().getClass().getPackage().getName() + ".";

    private final static Method getHandleMethod;
    private final static Object server;
    private final static Field NMSCommandDispatcherField;
    private final static Method actualizePlayerMethod;
    private final static Field commandDispatcherField;
    private final static Constructor<?> bukkitCommandWrapperConstructor;
    private final static Method registerMethod;

    static {
        if(version >= 13) {
            Method getHandle = null;
            try {
                getHandle = Class.forName(cpack + "entity.CraftPlayer").getDeclaredMethod("getHandle");
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {}

            getHandleMethod = getHandle;

            Field nmsCommandDispatcherField0 = null;
            Object server0 = null;
            try {
                final Field serverField = Bukkit.getServer().getClass().getDeclaredField("console");
                serverField.setAccessible(true);
                server0 = serverField.get(Bukkit.getServer());
                nmsCommandDispatcherField0 = Class.forName(npack + "MinecraftServer").getDeclaredField("commandDispatcher");
                nmsCommandDispatcherField0.setAccessible(true);
            } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException ignored) {}

            server = server0;
            NMSCommandDispatcherField = nmsCommandDispatcherField0;

            Class<?> nmsCommandDispatcherClass = null;
            try {
                nmsCommandDispatcherClass = Class.forName(npack+"CommandDispatcher");
            } catch (ClassNotFoundException ignored) {}

            assert nmsCommandDispatcherClass != null;

            Method actualizePlayer = null;
            try {
                actualizePlayer = nmsCommandDispatcherClass.getDeclaredMethod("a", Class.forName(npack + "EntityPlayer"));
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {}

            actualizePlayerMethod = actualizePlayer;

            Field commandDispatcherField0 = null;
            try {
                commandDispatcherField0 = nmsCommandDispatcherClass.getDeclaredField("b");
                commandDispatcherField0.setAccessible(true);
            } catch (NoSuchFieldException ignored) {}

            commandDispatcherField = commandDispatcherField0;

            Constructor<?> bukkitCommandWrapper = null;
            Method register = null;
            try {
                final Class<?> bukkitCommandWrapperClass = Class.forName(cpack + "command.BukkitCommandWrapper");
                bukkitCommandWrapper = bukkitCommandWrapperClass.getDeclaredConstructor(Bukkit.getServer().getClass(), Command.class);
                register = bukkitCommandWrapperClass.getDeclaredMethod("register", CommandDispatcher.class, String.class);
                bukkitCommandWrapper.setAccessible(true);
                register.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {}

            bukkitCommandWrapperConstructor = bukkitCommandWrapper;
            registerMethod = register;
        }
        else{
             getHandleMethod = null;
             server = null;
             NMSCommandDispatcherField = null;
             actualizePlayerMethod = null;
             commandDispatcherField = null;
             bukkitCommandWrapperConstructor = null;
             registerMethod = null;
        }
    }

    private static Object getNMSCommandDispatcher(){
        try {
            return NMSCommandDispatcherField.get(server);
        } catch (IllegalAccessException ignored) {}
        throw new RuntimeException("Can not access to the command dispatcher");
    }

    private static CommandDispatcher<?> getCommandDispatcher(){
        try {
            return (CommandDispatcher<?>) commandDispatcherField.get(getNMSCommandDispatcher());
        } catch (IllegalAccessException ignored) {}
        throw new RuntimeException("Can not access to the command dispatcher");
    }

    public static int getVersion() {
        return version;
    }

    private static void checkVersion(){
        if(version < 13){
            throw new RuntimeException(new IllegalAccessException("This method is only for minecraft 1.13 and more"));
        }
    }

    /*

    public static void addCommand(Set<String> labels, PluginCommand command){
        System.out.println(labels);
        checkVersion();
        try{
            final Object bukkitCommandWrapper = bukkitCommandWrapperConstructor.newInstance(Bukkit.getServer(), command);
            for(String label : labels){
                registerMethod.invoke(bukkitCommandWrapper, getCommandDispatcher(), label);
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException ignored) {}
    }

    public static void removeCommands(Set<String> commands){
        System.out.println(commands);
        checkVersion();
        final RootCommandNode<?> rootCommandNode = getCommandDispatcher().getRoot();
        rootCommandNode.getChildren().stream()
                .filter(e->commands.contains(e.getName()))
                .map(CommandNode::getName)
                .collect(Collectors.toSet())
                .forEach(rootCommandNode::removeCommand);
    }

    public static void synchronize(){
        System.out.println(getNMSCommandDispatcher());
        System.out.println(getCommandDispatcher());
        listCommand(getCommandDispatcher().getRoot().getChildren());
        checkVersion();
        final Object NMSCommandDispatcher = getNMSCommandDispatcher();
        for(final Player player : Bukkit.getOnlinePlayers()){
            try {
                actualizePlayerMethod.invoke(NMSCommandDispatcher, getHandleMethod.invoke(player));
            } catch (IllegalAccessException | InvocationTargetException ignored) {}
        }
    }
     */

    public static void addCommand(Set<String> labels, PluginCommand command){
        final CraftServer server = (CraftServer) Bukkit.getServer();
        final CommandDispatcher<CommandListenerWrapper> commandDispatcher = server.getHandle().getServer().dataPackResources.commandDispatcher.a();

        for(String label : labels){
            new BukkitCommandWrapper(server, command).register(commandDispatcher, label);
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeCommands(Set<String> commands){
        final CraftServer server = (CraftServer) Bukkit.getServer();
        final CommandDispatcher<CommandListenerWrapper> commandDispatcher = server.getHandle().getServer().dataPackResources.commandDispatcher.a();
        final RootCommandNode<?> rootCommandNode = commandDispatcher.getRoot();

        try{
            final Field childrenMapField = CommandNode.class.getDeclaredField("children");
            final Field literalsMapField = CommandNode.class.getDeclaredField("literals");
            final Field argumentsMapField = CommandNode.class.getDeclaredField("arguments");

            childrenMapField.setAccessible(true);
            literalsMapField.setAccessible(true);
            argumentsMapField.setAccessible(true);

            Map<String, CommandNode<?>> children = (Map<String, CommandNode<?>>) childrenMapField.get(rootCommandNode);
            Map<String, LiteralCommandNode<?>> literals = (Map<String, LiteralCommandNode<?>>) literalsMapField.get(rootCommandNode);
            Map<String, ArgumentCommandNode<?, ?>> arguments = (Map<String, ArgumentCommandNode<?, ?>>) argumentsMapField.get(rootCommandNode);

            for(String command : commands){
                children.remove(command);
                literals.remove(command);
                arguments.remove(command);
            }
        }catch (Exception ignored){}

    }

    public static void synchronize(){
        final CraftServer server = (CraftServer) Bukkit.getServer();
        final net.minecraft.server.v1_16_R1.CommandDispatcher commandDispatcher = server.getHandle().getServer().dataPackResources.commandDispatcher;

        for(final Player player : Bukkit.getOnlinePlayers()){
            final CraftPlayer craftPlayer = (CraftPlayer) player;
            commandDispatcher.a(craftPlayer.getHandle());
        }
    }

    /**
     *
     * Debug Method (not useful at all)
     *
     */
    private static <T> void listCommand(Collection<CommandNode<T>> children){

        for(CommandNode<T> child : children){
            System.out.println(child.getName());
            listCommand(child.getChildren());
        }

    }

}
