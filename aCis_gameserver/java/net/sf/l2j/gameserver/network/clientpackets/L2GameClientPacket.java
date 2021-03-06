package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.mmocore.ReceivablePacket;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * Packets received by the gameserver from clients.
 */
public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient>
{
	protected static final Logger _log = Logger.getLogger(L2GameClientPacket.class.getName());
	
	protected abstract void readImpl();
	
	protected abstract void runImpl();
	
	@Override
	protected boolean read()
	{
		if (Config.PACKET_HANDLER_DEBUG)
			_log.info(getType());
		
		try
		{
			readImpl();
			return true;
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + e, e);
			
			if (e instanceof BufferUnderflowException) // only one allowed per client per minute
				getClient().onBufferUnderflow();
		}
		return false;
	}
	
	@Override
	public void run()
	{
		try
		{
			runImpl();
			
			// Depending of the packet send, removes spawn protection
			if (triggersOnActionRequest())
			{
				final Player player = getClient().getActiveChar();
				if (player != null && player.isSpawnProtected())
					player.onActionRequest();
			}
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + t, t);
			
			if (this instanceof EnterWorld)
				getClient().closeNow();
		}
	}
	
	protected final void sendPacket(L2GameServerPacket gsp)
	{
		getClient().sendPacket(gsp);
	}
	
	/**
	 * @return A String with this packet name for debuging purposes
	 */
	public String getType()
	{
		return "[C] " + getClass().getSimpleName();
	}
	
	/**
	 * Overriden with true value on some packets that should disable spawn protection
	 * @return
	 */
	protected boolean triggersOnActionRequest()
	{
		return true;
	}
}