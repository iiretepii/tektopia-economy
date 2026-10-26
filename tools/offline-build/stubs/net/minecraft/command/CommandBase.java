package net.minecraft.command; import java.util.List; import javax.annotation.Nullable; import net.minecraft.server.MinecraftServer; import net.minecraft.util.math.BlockPos; import net.minecraft.entity.player.EntityPlayerMP;
public abstract class CommandBase implements ICommand { public int compareTo(ICommand o){return 0;}
 public abstract String getName(); public abstract String getUsage(ICommandSender s);
 public int getRequiredPermissionLevel(){return 4;}
 public boolean checkPermission(MinecraftServer server, ICommandSender sender){return true;}
 public abstract void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;
 public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos){return null;}
 public static int parseInt(String s, int min) throws NumberInvalidException {return 0;}
 public static EntityPlayerMP getCommandSenderAsPlayer(ICommandSender s) throws PlayerNotFoundException {return null;}
 public static List<String> getListOfStringsMatchingLastWord(String[] args, String... p){return null;}
 public static List<String> getListOfStringsMatchingLastWord(String[] args, java.util.Collection<?> p){return null;} }
