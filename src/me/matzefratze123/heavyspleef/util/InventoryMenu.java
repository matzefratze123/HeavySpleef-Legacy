package me.matzefratze123.heavyspleef.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.matzefratze123.heavyspleef.HeavySpleef;
import me.matzefratze123.heavyspleef.core.Game;
import me.matzefratze123.heavyspleef.core.GameManager;
import me.matzefratze123.heavyspleef.core.flag.FlagType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class InventoryMenu implements Listener {
	
	private static final String infinity = "\u221E";
	private boolean unregistered;
	private String title;
	
	private Set<String> viewing = new HashSet<String>();
	
	public InventoryMenu(String title, Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		
		this.title = title;
	}
	
	public void open(final Player player) {
		validateState();
		
		int size = calculateSize(GameManager.getGames().length);
		Game[] games = GameManager.getGames();
		Arrays.sort(games, new GameSorter());
		
		final Inventory inv = Bukkit.createInventory(null, size, title);
		for (Game game : games) {
			ItemStack icon = game.getFlag(FlagType.ICON);
			icon = icon.getData().toItemStack(icon.getAmount());
			
			if (icon == null)
				icon = new ItemStack(Material.DIAMOND_SPADE);
			
			ItemMeta meta = icon.getItemMeta();
			ChatColor color = game.isWaiting() || game.isPreLobby() ? ChatColor.GREEN : ChatColor.RED;
			
			meta.setDisplayName(color + "Join " + game.getName());
			List<String> lore = new ArrayList<String>();
			
			String maxPlayers = String.valueOf(game.getFlag(FlagType.MAXPLAYERS) < 2 ? infinity : game.getFlag(FlagType.MAXPLAYERS));
			
			lore.add(color + "" + game.getPlayers().length + ChatColor.DARK_GRAY + ChatColor.BOLD + " / " + ChatColor.RED + maxPlayers);
			lore.add(ChatColor.AQUA + Util.toFriendlyString(game.getGameState().name()));
			
			meta.setLore(lore);
			icon.setItemMeta(meta);
			
			inv.addItem(icon);
		}
		
		player.closeInventory();
		Bukkit.getScheduler().runTask(HeavySpleef.instance, new Runnable() {
			
			@Override
			public void run() {
				player.openInventory(inv);
				viewing.add(player.getName());
			}
		});
	}
	
	public void unregister() {
		HandlerList.unregisterAll(this);
		title = null;
		unregistered = true;
	}
	
	private void validateState() {
		if (unregistered)
			throw new IllegalStateException("Cannot perform inventory menu options while unregistered!");
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		if (viewing.contains(player.getName()))
			viewing.remove(player.getName());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		Inventory inv = e.getInventory();
		
		if (!inv.getTitle().equalsIgnoreCase(title))
			return;
		if (!viewing.contains(player.getName()))
			return;
		
		e.setCancelled(true);
		if (e.getSlotType() != SlotType.CONTAINER)
			return;
		int slot = e.getSlot();
		if (slot >= inv.getSize()) //Prevent ArrayIndexOutOfBoundsExceptions...
			return;
		
		ItemStack item = inv.getItem(slot);
		if (item == null) //No NPE's
			return;
		
		ItemMeta meta = item.getItemMeta();
		if (meta.getDisplayName() == null)
			return;
		
		String displayName = ChatColor.stripColor(meta.getDisplayName());
		if (displayName.length() < 5)
			return;
		
		String gameName = displayName.substring(5);
		player.closeInventory();
		
		player.performCommand("spleef join " + gameName);
	}
	
	static int calculateSize(int base) {
		base = Math.abs(base);
		
		while (base % 9 != 0)
			base++;
		
		if (base > 54)
			base = 54;
		if (base <= 0)
			base = 9;
		
		return base;
	}
	
	private class GameSorter implements Comparator<Game> {

		@Override
		public int compare(Game o1, Game o2) {
			return o1.getName().compareTo(o2.getName());
		}
		
	}
	
}
