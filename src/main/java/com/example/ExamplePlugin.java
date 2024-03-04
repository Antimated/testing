package com.example;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.WidgetNode;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
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
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ExampleConfig config;

	private final int COMPONENT_ID = WidgetUtil.packComponentId(303, 2); // (interfaceId << 16) | childId

	private int INTERFACE_ID;

	private WidgetNode widgetNode;

	private final int TICKS_FOR_INTERFACE_CHANGE = 5;

	private final List<Integer> BLACKLISTED_INTERFACE_IDS = ImmutableList.of(16, 49, 76, 80, 109, 115, 116, 149, 160, 161, 162, 163, 164, 182, 216);

	private int interfaceTicks = TICKS_FOR_INTERFACE_CHANGE;

	private boolean shouldDisplayInterface = false;

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

	private void openInterface() throws IllegalStateException
	{
		if (!client.getGameState().equals(GameState.LOGGED_IN)) {
			return;
		}

		clientThread.invokeLater(() -> {
			// If no interfaceID is given set it
			if (INTERFACE_ID == 0)
			{
				// Set to last blacklisted interface id + 1
				INTERFACE_ID = BLACKLISTED_INTERFACE_IDS.get(BLACKLISTED_INTERFACE_IDS.size() - 1) + 1;
			}

			// 153 quest complete
			// If a widgetNode is opened, close it
			if (widgetNode != null)
			{
				client.closeInterface(widgetNode, true);
			}


			if (!BLACKLISTED_INTERFACE_IDS.contains(INTERFACE_ID)) {
				// Open interface
				try {
					log.debug("Attempting to open interface with id '{}'", INTERFACE_ID);
					widgetNode = client.openInterface(COMPONENT_ID, INTERFACE_ID, WidgetModalMode.MODAL_NOCLICKTHROUGH);
					log.debug("Opened interface with id {} successfully", INTERFACE_ID);
				} catch (IllegalStateException e) {
					throw new IllegalStateException();
				}
			} else {
				widgetNode = null;
				log.debug("Could not open interface with id '{}' as it is blacklisted.", INTERFACE_ID);

			}


			// Set interfaceID to the next one
			INTERFACE_ID++;
		});
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (!shouldDisplayInterface)
		{
			return;
		}

		if (interfaceTicks > 0)
		{
			//log.debug("Ticks left... {}", interfaceTicks);
			interfaceTicks--;
		}
		else if (interfaceTicks == 0)
		{
			interfaceTicks = TICKS_FOR_INTERFACE_CHANGE;

//			log.debug("Show next interface");
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

				case "test":

					shouldDisplayInterface = !shouldDisplayInterface;

					if (!shouldDisplayInterface)
					{
						interfaceTicks = TICKS_FOR_INTERFACE_CHANGE;
						log.debug("Reset interface ticks to {}", interfaceTicks);
					}
					//openInterface();

					break;
			}

		}
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}
