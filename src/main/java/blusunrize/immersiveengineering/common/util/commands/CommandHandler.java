package blusunrize.immersiveengineering.common.util.commands;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;
import blusunrize.immersiveengineering.common.util.Lib;

public class CommandHandler extends CommandBase
{
	static ArrayList<IESubCommand> commands = new ArrayList();
	static
	{
		commands.add(new CommandHelp());
		commands.add(new CommandMineral());
		commands.add(new CommandShaders());
	}

	@Override
	public String getCommandName()
	{
		return "ie";
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args)
	{
		ArrayList<String> list = new ArrayList<String>();
		if(args.length>0)
			for(IESubCommand sub : commands)
			{
				if(args.length==1)
				{
					if(args[0].isEmpty() || sub.getIdent().startsWith(args[0].toLowerCase()))
						list.add(sub.getIdent());
				}
				else if(sub.getIdent().equalsIgnoreCase(args[0]))
				{
					String[] redArgs = new String[args.length-1];
					System.arraycopy(args,1, redArgs,0, redArgs.length);
					ArrayList<String> subCommands = sub.getSubCommands(redArgs);	
					if(subCommands!=null)
						list.addAll(subCommands);
				}
			}
		return list;
	}

	@Override
	public String getCommandUsage(ICommandSender sender)
	{
		String sub = "";
		int i=0;
		for(IESubCommand com : CommandHandler.commands)
			sub += ((i++)>0?"|":"")+com.getIdent();
		return "/ie <"+sub+">";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args)
	{
		ArrayList<String> list = new ArrayList<String>();
		String assemble = null;
		for(String ss : args)
		{
			ss = ss.trim();
			if(assemble!=null)
			{
				assemble += " "+ss;
				if(ss.endsWith(">"))
				{
					list.add(assemble.substring(1, assemble.length()-1));
					assemble = null;
				}
			}
			else if(ss.startsWith("<")&&!ss.endsWith(">"))
				assemble = ss;
			else
			{
				if(ss.startsWith("<"))
					ss = ss.substring(1);
				if(ss.endsWith(">"))
					ss = ss.substring(0,ss.length()-1);
				list.add(ss);
			}
		}
		args = list.toArray(new String[list.size()]);

		if(args.length>0)
			for(IESubCommand com : commands)
			{
				if(com.getIdent().equalsIgnoreCase(args[0]))
					com.perform(sender, args);
			}
		else
		{
			String sub = "";
			int i=0;
			for(IESubCommand com : CommandHandler.commands)
				sub += ((i++)>0?", ":"")+com.getIdent();
			sender.addChatMessage(new ChatComponentTranslation(Lib.CHAT_COMMAND+"available",sub));
		}
	}

	public static abstract class IESubCommand
	{
		public abstract String getIdent();
		public abstract void perform(ICommandSender sender, String[] args);
		public String getHelp(String subIdent)
		{
			return Lib.CHAT_COMMAND+getIdent()+subIdent+".help";
		}
		public abstract ArrayList<String> getSubCommands(String[] args);
	}
}