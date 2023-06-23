package io.github.thecsdev.betterstats.client.gui.panel.stats;

import static io.github.thecsdev.betterstats.util.StatUtils.getModName;
import static io.github.thecsdev.tcdcommons.api.util.TextUtils.literal;
import static io.github.thecsdev.tcdcommons.api.util.TextUtils.translatable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import io.github.thecsdev.betterstats.BetterStats;
import io.github.thecsdev.betterstats.api.registry.BetterStatsRegistry;
import io.github.thecsdev.betterstats.client.gui.panel.BSPanel;
import io.github.thecsdev.betterstats.client.gui.screen.BetterStatsScreen;
import io.github.thecsdev.betterstats.client.gui_hud.screen.BetterStatsHudScreen;
import io.github.thecsdev.betterstats.client.gui_hud.widget.BSHudStatWidget_Entity;
import io.github.thecsdev.betterstats.util.StatUtils;
import io.github.thecsdev.betterstats.util.StatUtils.StatUtilsMobStat;
import io.github.thecsdev.betterstats.util.StatUtils.StatUtilsStat;
import io.github.thecsdev.tcdcommons.api.client.gui.other.TEntityRendererElement;
import io.github.thecsdev.tcdcommons.api.client.gui.other.TLabelElement;
import io.github.thecsdev.tcdcommons.api.client.gui.panel.TContextMenuPanel;
import io.github.thecsdev.tcdcommons.api.client.gui.panel.TPanelElement;
import io.github.thecsdev.tcdcommons.api.client.gui.util.GuiUtils;
import io.github.thecsdev.tcdcommons.api.client.gui.util.HorizontalAlignment;
import io.github.thecsdev.tcdcommons.api.client.gui.widget.TSelectEnumWidget;
import io.github.thecsdev.tcdcommons.api.client.gui.widget.TSelectWidget;
import io.github.thecsdev.tcdcommons.api.util.TextUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.stat.StatHandler;
import net.minecraft.text.MutableText;

public class BSStatPanel_Mobs extends BSStatPanel
{
	// ==================================================
	public static enum BSStatPanelMobs_SortBy
	{
		Default(literal("-")),
		Kills(translatable("betterstats.hud.entity.kills")),
		Deaths(translatable("betterstats.hud.entity.deaths"));
		
		private final MutableText text;
		BSStatPanelMobs_SortBy(MutableText text) { this.text = text; }
		public MutableText asText() { return text; }
	}
	// --------------------------------------------------
	protected final boolean guiMobsFollowCursor;
	// ==================================================
	public BSStatPanel_Mobs(int x, int y, int width, int height)
	{
		super(x, y, width, height);
		this.guiMobsFollowCursor = BetterStats.getInstance().getConfig().guiMobsFollowCursor;
	}
	public BSStatPanel_Mobs(TPanelElement parentToFill)
	{
		super(parentToFill);
		this.guiMobsFollowCursor = BetterStats.getInstance().getConfig().guiMobsFollowCursor;
	}
	// ==================================================
	@Override
	public Predicate<StatUtilsStat> getStatPredicate()
	{
		//make sure it is a mob stat, as some subclasses
		//assume the stat is a mob stat
		return stat ->
		{
			//check instance type
			if(!(stat instanceof StatUtilsMobStat))
				return false;
			//check entity type (included non-empty non-living entities)
			var ent = TEntityRendererElement.getCachedEntityFromType(((StatUtilsMobStat)stat).entityType);
			if(!(ent instanceof LivingEntity) && stat.isEmpty())
				return false;
			//return true if all checks pass
			return true;
		};
	}
	
	public @Override TSelectWidget createFilterSortByWidget(BetterStatsScreen bss, int x, int y, int width, int height)
	{
		var sw = new TSelectEnumWidget<BSStatPanelMobs_SortBy>(x, y, width, height, BSStatPanelMobs_SortBy.class);
		sw.setSelected(bss.cache.getAs("BSStatPanelMobs_SortBy", BSStatPanelMobs_SortBy.class, BSStatPanelMobs_SortBy.Default), false);
		sw.setEnumValueToLabel(newVal -> ((BSStatPanelMobs_SortBy)newVal).asText());
		sw.setOnSelectionChange(newVal ->
		{
			bss.cache.set("BSStatPanelMobs_SortBy", newVal);
			bss.getStatPanel().init_stats();
		});
		return sw;
	}
	// ==================================================
	@Override
	public void init(BetterStatsScreen bss, StatHandler statHandler, Predicate<StatUtilsStat> statFilter)
	{
		//by default, group by mods
		switch(getFilterGroupBy())
		{
			case None: initByNoGroups(bss, statHandler, statFilter); break;
			default: initByModGroups(bss, statHandler, statFilter); break;
		}
	}
	
	protected void initByNoGroups(BetterStatsScreen bss, StatHandler statHandler, Predicate<StatUtilsStat> statFilter)
	{
		//get mob stats
		var mobStats = StatUtils.getMobStats(statHandler, statFilter.and(getStatPredicate()));
		ArrayList<StatUtilsMobStat> allMobs = Lists.newArrayList();
		//merge mob stats
		for(var mobGroup : mobStats.keySet())
			allMobs.addAll(mobStats.get(mobGroup));
		//init
		if(mobStats.size() > 0)
		{
			init_groupLabel(literal("*"));
			init_mobStats(bss, allMobs);
			init_totalStats(mobStats.values());
		}
		//if there are no stats...
		else init_noResults();
	}
	
	protected void initByModGroups(BetterStatsScreen bss, StatHandler statHandler, Predicate<StatUtilsStat> statFilter)
	{
		var mobStats = StatUtils.getMobStats(statHandler, statFilter.and(getStatPredicate()));
		for(var mobGroup : mobStats.keySet())
		{
			init_groupLabel(literal(getModName(mobGroup)));
			init_mobStats(bss, mobStats.get(mobGroup));
		}
		//if there are no stats...
		if(mobStats.size() == 0) init_noResults();
		//else init total stats as well
		else init_totalStats(mobStats.values());
	}
	// --------------------------------------------------
	protected void init_mobStats(BetterStatsScreen bss, ArrayList<StatUtilsMobStat> mobStats)
	{
		//sort the stats
		switch(bss.cache.getAs("BSStatPanelMobs_SortBy", BSStatPanelMobs_SortBy.class, BSStatPanelMobs_SortBy.Default))
		{
			case Kills: Collections.sort(mobStats, (o1, o2) -> Integer.compare(o2.killed, o1.killed)); break;
			case Deaths: Collections.sort(mobStats, (o1, o2) -> Integer.compare(o2.killedBy, o1.killedBy)); break;
			default: break;
		}
		
		//declare the starting XY
		int nextX = getTpeX() + getScrollPadding();
		int nextY = getTpeY() + getScrollPadding();
		
		//calculate nextY based on the last child
		{
			var lastChild = getLastTChild(false);
			if(lastChild != null) nextY = lastChild.getTpeEndY() + 2;
		}
		
		//iterate and add item stat elements
		final int SIZE = 50;
		for(var stat : mobStats)
		{
			//create and add the widget for the stat
			addTChild(createStatWidget(stat, nextX, nextY, SIZE), false);
			
			//increment next XY
			nextX += SIZE + 2;
			if(nextX + SIZE > getTpeEndX() - getScrollPadding())
			{
				nextX = getTpeX() + getScrollPadding();
				nextY += SIZE + 2;
			}
		}
	}
	
	protected void init_totalStats(Collection<ArrayList<StatUtilsMobStat>> mobStats)
	{
		//define KD
		int kills = 0, deaths = 0;
		
		//iterate all stats
		for(var group : mobStats)
		{
			//and count the kills and deaths
			for(var groupItem : group)
			{
				//ignore empty stats
				if(groupItem == null || groupItem.isEmpty())
					continue;
				//count
				kills += groupItem.killed;
				deaths += groupItem.killedBy;
			}
		}
		
		//init a new group
		var glSb = new StringBuilder();
		glSb.append(new char[] { 8592, 32, 8226, 32, 8594 });
		var groupLabel = init_groupLabel(TextUtils.literal(glSb.toString()));
		groupLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);
		
		//declare the starting XY
		int nextX = getTpeX() + getScrollPadding();
		int nextY = getTpeY() + getScrollPadding();
		
		//calculate nextY based on the last child
		{
			var lastChild = getLastTChild(false);
			if(lastChild != null) nextY = lastChild.getTpeEndY() + 2;
		}
		
		//create the panel
		var panel = new BSPanel(nextX, nextY, getTpeWidth() - (getScrollPadding() * 2), 20);
		panel.setScrollPadding(0);
		addTChild(panel, false);
		
		//create the labels
		int leftX = 5, leftW = (panel.getTpeWidth() / 2) - 10;
		int rightX = (panel.getTpeWidth() / 2) + 5, rightW = (panel.getTpeWidth() / 2) - 10;
		
		var lbl_kills_a = new TLabelElement(leftX, 0, leftW, 20, translatable("betterstats.hud.entity.kills"));
		var lbl_kills_b = new TLabelElement(leftX, 0, leftW, 20, literal(Integer.toString(kills)));
		lbl_kills_b.setHorizontalAlignment(HorizontalAlignment.RIGHT);
		
		var lbl_deaths_a = new TLabelElement(rightX, 0, rightW, 20, translatable("betterstats.hud.entity.deaths"));
		var lbl_deaths_b = new TLabelElement(rightX, 0, rightW, 20, literal(Integer.toString(deaths)));
		lbl_deaths_b.setHorizontalAlignment(HorizontalAlignment.RIGHT);
		
		panel.addTChild(lbl_kills_a, true);
		panel.addTChild(lbl_kills_b, true);
		panel.addTChild(lbl_deaths_a, true);
		panel.addTChild(lbl_deaths_b, true);
	}
	// ==================================================
	protected BSStatWidget_Mob createStatWidget(StatUtilsMobStat stat, int x, int y, int size)
	{
		return new BSStatWidget_Mob(stat, x, y, size);
	}
	// ==================================================
	protected class BSStatWidget_Mob extends BSStatWidget
	{
		// ----------------------------------------------
		public final StatUtilsMobStat stat;
		public final TEntityRendererElement entityRenderer;
		// ----------------------------------------------
		public BSStatWidget_Mob(StatUtilsMobStat stat, int x, int y, int size)
		{
			super(x, y, size, size);
			this.stat = Objects.requireNonNull(stat, "stat must not be null.");
			addTChild(this.entityRenderer = new TEntityRendererElement(x, y, size, size, stat.entityType), false);
			this.entityRenderer.setFollowCursor(BSStatPanel_Mobs.this.guiMobsFollowCursor);
			
			updateTooltip();
		}
		// ----------------------------------------------
		public @Override void updateTooltip()
		{
			String entityName = stat.label.getString();
			String s0 = translatable("stat_type.minecraft.killed.none", entityName).getString();
			String s1 = translatable("stat_type.minecraft.killed_by.none", entityName).getString();
			
			if(stat.killed != 0)
				s0 = translatable("stat_type.minecraft.killed", Integer.toString(stat.killed), entityName).getString();
			if(stat.killedBy != 0)
				s1 = translatable("stat_type.minecraft.killed_by", entityName, Integer.toString(stat.killedBy)).getString();
			
			setTooltip(literal(s0 + "\n" + s1));
		}
		// ----------------------------------------------
		public @Override boolean mousePressed(int mouseX, int mouseY, int button)
		{
			//handle Wikis
			if(button == 2)
			{
				String url = BetterStatsRegistry.getMobWikiURL(Registries.ENTITY_TYPE.getId(this.stat.entityType));
				if(url != null)
				{
					GuiUtils.showUrlPrompt(url, false);
					//if successful, block the focus by returning false
					return false;
				}
			}
			//return super if all else fails
			return super.mousePressed(mouseX, mouseY, button);
		}
		// ----------------------------------------------
		@Override
		protected void onContextMenu(TContextMenuPanel contextMenu)
		{
			super.onContextMenu(contextMenu);
			contextMenu.addButton(translatable("betterstats.gui.ctx_menu.pin_to_hud"), btn ->
			{
				var bshs = BetterStatsHudScreen.getOrCreateInstance(this.screen);
				getClient().setScreen(bshs);
				bshs.addHudStatWidget(new BSHudStatWidget_Entity(0, 0, stat.statHandler, stat.entityType));
			});
			contextMenu.addButton(translatable("betterstats.gui.ctx_menu.close"), btn -> {});
		}
		// ----------------------------------------------
	}
	// ==================================================
}