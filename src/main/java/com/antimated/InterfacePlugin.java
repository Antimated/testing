package com.antimated;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import java.nio.BufferOverflowException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WidgetNode;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Example"
)
public class InterfacePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InterfaceConfig config;

	private final int COMPONENT_ID = WidgetUtil.packComponentId(303, 2); // (interfaceId << 16) | childId
//	private final int COMPONENT_ID = ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_RESIZABLE_VIEWPORT_BOTTOM_LINE; // (interfaceId << 16) | childId

	private int interfaceId;

	private WidgetNode widgetNode;

	private final int TICKS_FOR_INTERFACE_CHANGE = 3;

	//private final List<Integer> BLACKLISTED_INTERFACE_IDS = ImmutableList.of(16, 49, 76, 80, 109, 115, 116, 149, 160, 161, 162, 163, 164, 182, 216, 218, 239, 246, 303, 320, 387);
	private final List<Integer> BLACKLISTED_INTERFACE_IDS = ImmutableList.of(16, 80, 164, 246, 478, 592, 601, 626, 652, 723, 864); // These give null-pointer, stackoverflow, ... exceptions (864 = out of bounds)

	private int interfaceTicks = 0;

	private boolean shouldAutoOpen = false;

	@Inject
	@Named("developerMode")
	boolean developerMode;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	private void openInterface()
	{
		if (!client.getGameState().equals(GameState.LOGGED_IN))
		{
			return;
		}

		clientThread.invokeLater(() -> {
			// Setup the interfaceID we have to start from.
			if (interfaceId == 0)
			{
				// Set to last blacklisted interface id + 1 (so we dont have to start over)
				interfaceId = BLACKLISTED_INTERFACE_IDS.get(BLACKLISTED_INTERFACE_IDS.size() - 1) + 1;
//				interfaceId = f;
			}

			// Try and close previously opened widgetNode.
			if (widgetNode != null)
			{
				try
				{
					client.closeInterface(widgetNode, true);
				}
				catch (IllegalArgumentException | NullPointerException e)
				{
					log.error("Error closing widgetNode interface: {}", e.getMessage());
					widgetNode = null;
				}
			}


			if (!BLACKLISTED_INTERFACE_IDS.contains(interfaceId))
			{
				// Open interface
				try
				{
					log.debug("Attempting to open interface with id '{}'", interfaceId);
					widgetNode = client.openInterface(COMPONENT_ID, interfaceId, WidgetModalMode.MODAL_CLICKTHROUGH);
					log.debug("Opened interface with id {} successfully", interfaceId);
				}
				catch (IllegalStateException | NullPointerException e)
				{
					log.error("Error opening widgetNode: {}", e.getMessage());
					widgetNode = null;
				}
			}
			else
			{
				widgetNode = null;
				log.debug("Could not open interface with id '{}' as it is blacklisted.", interfaceId);

			}


			// Set interfaceID to the next one
			interfaceId++;
		});
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (!shouldAutoOpen)
		{
			return;
		}

		if (interfaceTicks > 0)
		{
			interfaceTicks--;
		}
		else if (interfaceTicks == 0)
		{
			interfaceTicks = TICKS_FOR_INTERFACE_CHANGE;
			openInterface();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if (developerMode)
		{
			String[] args = commandExecuted.getArguments();

			switch (commandExecuted.getCommand())
			{

				case "open":

					if (args.length == 0) {
						shouldAutoOpen = !shouldAutoOpen;

						if (!shouldAutoOpen)
						{
							log.debug("Stop auto-opening interfaces");
							interfaceTicks = TICKS_FOR_INTERFACE_CHANGE;

						}
						else
						{
							interfaceTicks = 0;
							log.debug("Starting to open interfaces...");
						}

						log.debug("Reset interface ticks to {}", interfaceTicks);
					}
					else if (args.length == 1)
					{
						shouldAutoOpen = false;
						interfaceId = Integer.parseInt(args[0]);
						clientThread.invokeLater(this::openInterface);
					}

					break;
			}

		}
	}

	@Provides
	InterfaceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InterfaceConfig.class);
	}
}
